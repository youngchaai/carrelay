package com.carrelay.vehicle.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.carrelay.core.text.KoreanText
import com.carrelay.core.care.CareDailySummary
import com.carrelay.core.care.CareEffect
import com.carrelay.core.care.CareEvent
import com.carrelay.core.care.CareModeMonitor
import com.carrelay.core.driving.DrivingEvent
import com.carrelay.core.driving.DrivingEventDetector
import com.carrelay.core.handover.HandoverBriefingBuilder
import com.carrelay.core.incident.IncidentLog
import com.carrelay.core.memo.MemoBoard
import com.carrelay.core.memo.VoiceMemoParser
import com.carrelay.core.model.CarUsage
import com.carrelay.core.model.CarZone
import com.carrelay.core.model.TripPurpose
import com.carrelay.core.model.DoorEvent
import com.carrelay.core.model.Driver
import com.carrelay.core.model.IncidentSource
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.IncidentType
import com.carrelay.core.model.Inspection
import com.carrelay.core.profile.LicenseCheck
import com.carrelay.core.profile.PartyRoster
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import com.carrelay.core.model.MemoPreset
import com.carrelay.core.model.Signal
import com.carrelay.core.model.SignalStatus
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.session.DriverSessionTracker
import com.carrelay.core.session.SessionEffect
import com.carrelay.core.session.SessionState
import com.carrelay.core.shift.ContinuousDrivingClock
import com.carrelay.core.shift.DriverRecommender
import com.carrelay.core.shift.ShiftAdvisor
import com.carrelay.vehicle.AppContainer
import com.carrelay.vehicle.CarRelayApplication
import com.carrelay.vehicle.data.AppData
import com.carrelay.vehicle.data.FakeVehicleGateway
import com.carrelay.vehicle.data.GuardianAlert
import com.carrelay.vehicle.data.SeedData
import com.carrelay.vehicle.feature.VoiceResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * 차량 앱의 두뇌. 차량 신호를 받아 core 로직(세션·브리핑·교대·케어)에 넣고, 결과를 화면 상태로 만듭니다.
 */
class CarRelayViewModel(private val container: AppContainer) : ViewModel() {

    private val gateway = container.gateway
    private val speaker = container.speaker
    private val voice = container.voice
    private var voiceJob: Job? = null

    /** 가상 차량일 때만 값이 있습니다('가상 차량' 패널용). */
    val simulatedCar: FakeVehicleGateway? = gateway as? FakeVehicleGateway

    private var data = AppData()
    private var tracker: DriverSessionTracker? = null
    private val drivingClock = ContinuousDrivingClock(hourOf = { localHour(it) })
    private val shiftAdvisor = ShiftAdvisor()
    private var careMonitor = CareModeMonitor()
    private var lastGear: Gear = Gear.UNKNOWN
    private val eventDetector = DrivingEventDetector()
    /** 이번 브리핑(인수 점검)에서 이상을 신고했는지 */
    private var foundIncidentThisBriefing = false
    /** 신고 화면을 닫으면 돌아갈 화면 */
    private var returnAfterReport: Screen = Screen.HOME
    /** 프로필 화면을 닫으면 돌아갈 화면 */
    private var returnAfterProfile: Screen = Screen.SETTINGS

    private val _ui = MutableStateFlow(UiState(sourceName = gateway.sourceName, isSimulated = gateway.isSimulated))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        data = container.store.load()
        val t = DriverSessionTracker(data.lastDriverId) { UUID.randomUUID().toString() }
        data.activeSession?.let { t.restore(it) }
        tracker = t
        careMonitor = CareModeMonitor(careConfig(data.demoFastThresholds))

        gateway.start()
        container.uxMonitor.start()
        refreshSnapshot()
        _ui.update { it.copy(loading = false, screen = Screen.PICKER) }
        publishData()

        viewModelScope.launch { gateway.driving.collect { onDrivingSample(it) } }
        viewModelScope.launch { gateway.driverDoorEvents.collect { onDoor(it) } }
        // 연료·배터리·경고등이 바뀌면 곧바로 다시 읽습니다(연달아 오면 마지막 것만).
        viewModelScope.launch {
            gateway.statusChanges.collectLatest {
                delay(300)
                refreshSnapshot()
            }
        }
        viewModelScope.launch { container.uxMonitor.requiresDistractionOptimization.collect { updateDrivingUi() } }
        viewModelScope.launch {
            var seconds = 0
            while (true) {
                delay(1_000)
                // 차량 값이 그대로여도 시간은 흐르므로 1초마다 타이머를 진행합니다.
                feedRealtime(gateway.driving.value.copy(timestampMillis = now()), fromTick = true)
                seconds++
                if (seconds % 10 == 0) refreshSnapshot()
            }
        }
    }

    // ── 차량 신호 처리 ─────────────────────────────────────

    private fun onDrivingSample(sample: DrivingSample) {
        val t = tracker ?: return
        if (sample.gear != lastGear) {
            val previous = lastGear
            lastGear = sample.gear
            handleSessionEffects(t.onGear(sample.gear, now(), _ui.value.snapshot?.odometerKm))
            if (sample.gear.isDriveGear) _ui.update { it.copy(parkedPrompt = false) }
            if (sample.gear == Gear.PARK && previous.isDriveGear) onParked()
        }
        feedRealtime(sample, fromTick = false)
        updateDrivingUi()
    }

    private fun feedRealtime(sample: DrivingSample, fromTick: Boolean) {
        if (tracker == null) return
        drivingClock.onSample(sample)
        // 시각만 바뀐 1초 틱은 화면 상태를 새로 만들지 않습니다(불필요한 다시 그리기 방지).
        _ui.update {
            val sameSample = it.driving?.copy(timestampMillis = 0L) == sample.copy(timestampMillis = 0L)
            if (sameSample && it.continuousMillis == drivingClock.continuousMillis) it
            else it.copy(driving = sample, continuousMillis = drivingClock.continuousMillis)
        }

        // 모든 운전자의 급정거·급가속 (차량에서 온 실제 샘플만)
        if (!fromTick) {
            val event = eventDetector.onSample(sample)
            if (event != null) {
                Log.i(TAG, "${event.label} 감지 (속도 ${sample.speedKmh?.let { "%.0f".format(it) }} km/h)")
                tracker?.addDrivingEvent(
                    harshDecel = if (event == DrivingEvent.HARSH_DECEL) 1 else 0,
                    harshAccel = if (event == DrivingEvent.HARSH_ACCEL) 1 else 0,
                )
                data = data.copy(activeSession = tracker?.currentSession)
                persist()
            }
        }

        val driver = currentDriver() ?: return
        if (driver.careModeEnabled) {
            handleCareEffects(careMonitor.onSample(sample, detectDecel = !fromTick), driver)
        }
        if (sample.gear.isDriveGear) evaluateShift(driver)
    }

    private fun onDoor(event: DoorEvent) {
        val t = tracker ?: return
        handleSessionEffects(t.onDriverDoor(event))
    }

    private fun updateDrivingUi() {
        val restricted = container.uxMonitor.requiresDistractionOptimization.value == true ||
            gateway.driving.value.gear.isDriveGear
        _ui.update { s ->
            val screen = when {
                restricted -> Screen.DRIVING
                // 실차는 기어를 P로 바꾼 뒤 주행 제한이 조금 늦게 풀립니다. 그 사이 띄운 '주차 후 메모'를 잃지 않게 합니다.
                s.screen == Screen.DRIVING -> if (s.parkedPrompt) Screen.MEMO else Screen.HOME
                else -> s.screen
            }
            s.copy(drivingUi = restricted, screen = screen)
        }
    }

    private fun onParked() {
        val driver = currentDriver()
        val harsh = tracker?.currentSession?.harshDecelCount ?: 0
        _ui.update {
            it.copy(
                screen = Screen.MEMO, parkedPrompt = true, shiftAdvice = null, careCheckIn = null,
                parkedHarshDecel = harsh, afterDriveChecked = false,
            )
        }
        if (harsh > 0) {
            speaker.speak("운행 중 급정거가 ${harsh}번 있었어요. 차에 이상이 있으면 신고해 주세요.")
        }
        viewModelScope.launch {
            val snap = refreshSnapshot()
            if (driver != null) {
                data = data.copy(lastSeen = data.lastSeen + (driver.id to snap))
                persist()
            }
        }
        if (harsh == 0) speaker.speak("주차하셨네요. 다음 운전자에게 남길 메모가 있으면 눌러 주세요.")
    }

    private fun evaluateShift(driver: Driver) {
        val s = _ui.value
        if (s.shiftAdvice != null || s.careCheckIn != null) return
        val others = data.drivers.filter { it.id != driver.id }.map { it to data.conditions[it.id] }
        val advice = shiftAdvisor.evaluate(
            continuousMillis = drivingClock.continuousMillis,
            nightMillis = drivingClock.nightMillis,
            current = driver,
            currentCondition = data.conditions[driver.id],
            others = others,
        ) ?: return
        _ui.update { it.copy(shiftAdvice = advice) }
        speaker.speak(advice.message)
    }

    private fun handleCareEffects(effects: List<CareEffect>, driver: Driver) {
        _ui.update { it.copy(careStage = careMonitor.stage) }
        if (effects.isEmpty()) return
        val at = now()
        for (effect in effects) {
            when (effect) {
                is CareEffect.Record -> data = data.copy(
                    careEvents = (data.careEvents + CareEvent(driver.id, effect.type, at, effect.reason))
                        .takeLast(MAX_CARE_EVENTS),
                )
                is CareEffect.AskAreYouOk -> {
                    // 안부 확인이 먼저입니다: 말로 메모를 남기던 중이면 닫습니다.
                    if (_ui.value.voiceMemo != null) cancelVoiceMemo()
                    _ui.update { it.copy(careCheckIn = effect.reason, shiftAdvice = null) }
                    speaker.speak("${driver.name}님, 괜찮으세요? 괜찮으시면 화면의 괜찮아요 버튼을 눌러 주세요.")
                }
                is CareEffect.AddRestStopWaypoint -> viewModelScope.launch {
                    val r = container.navigator.addNearestRestStop()
                    toast(r.message)
                    speaker.speak(
                        if (r.added && r.stopName != null) "응답이 없어 ${KoreanText.toward(r.stopName)} 안내할게요. 잠시 쉬어 가세요."
                        else r.message,
                    )
                }
                is CareEffect.NotifyGuardian -> viewModelScope.launch {
                    val location = container.navigator.currentLocationText()
                    val alert = GuardianAlert(
                        driverId = driver.id,
                        atMillis = at,
                        message = "${driver.name}님이 운전 중 안부 확인에 응답하지 않았어요. " +
                            "상황: ${effect.reason.label}. 위치: $location",
                    )
                    container.guardianNotifier.send(alert)
                    data = data.copy(guardianAlerts = (data.guardianAlerts + alert).takeLast(MAX_ALERTS))
                    publishData()
                    persist()
                    toast("${driver.guardianName ?: "보호자"}님께 알림을 보냈어요")
                }
                CareEffect.CloseCheckIn -> _ui.update { it.copy(careCheckIn = null) }
            }
        }
        publishData()
        persist()
    }

    private fun handleSessionEffects(effects: List<SessionEffect>) {
        if (effects.isEmpty()) return
        val t = tracker ?: return
        for (effect in effects) {
            when (effect) {
                is SessionEffect.ShowBriefing -> {
                    resetRealtimeMonitors()
                    foundIncidentThisBriefing = false
                    viewModelScope.launch { showBriefing(effect.driverId, greet = true) }
                }
                is SessionEffect.AutoAssigned -> {
                    resetRealtimeMonitors()
                    toast("운전자를 선택하지 않아 ${nameOf(effect.session.driverId)}님으로 기록해요 (미확인)")
                }
                is SessionEffect.SessionEnded -> {
                    val ended = effect.session
                    val snap = _ui.value.snapshot
                    data = data.copy(
                        sessions = (data.sessions + ended).takeLast(MAX_SESSIONS),
                        lastSeen = if (snap != null) data.lastSeen + (ended.driverId to snap) else data.lastSeen,
                    )
                }
                SessionEffect.AskDriverChanged -> {
                    _ui.update { it.copy(screen = Screen.PICKER, askingDriverChange = true) }
                    speaker.speak("운전자 바뀌셨나요?")
                }
                SessionEffect.KeptSameDriver -> _ui.update { it.copy(askingDriverChange = false) }
            }
        }
        val awaitingLast = (t.state as? SessionState.AwaitingDriver)?.lastDriverId
        data = data.copy(
            activeSession = t.currentSession,
            lastDriverId = t.currentSession?.driverId ?: awaitingLast ?: data.lastDriverId,
        )
        publishData()
        persist()
    }

    private suspend fun showBriefing(driverId: String, greet: Boolean = false) {
        val driver = driverById(driverId)
        val snap = refreshSnapshot()
        val briefing = HandoverBriefingBuilder.build(
            driver = driver,
            current = snap,
            lastSeen = data.lastSeen[driverId],
            memos = data.memos,
            nameOf = ::nameOf,
            nowMillis = now(),
            incidents = data.incidents,
        )
        _ui.update { it.copy(screen = Screen.BRIEFING, briefing = briefing, askingDriverChange = false, greeting = greet) }
        updateDrivingUi()
        speaker.speak(briefing.speech)
    }

    private suspend fun refreshSnapshot(): VehicleSnapshot {
        val snap = try {
            gateway.readStatus()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            VehicleSnapshot(now(), unavailable = Signal.entries.associateWith { SignalStatus.FAILED })
        }
        _ui.update { it.copy(snapshot = snap) }
        return snap
    }

    // ── 화면에서 부르는 동작 ───────────────────────────────

    fun selectDriver(driverId: String) {
        val t = tracker ?: return
        val effects = t.selectDriver(driverId, now(), _ui.value.snapshot?.odometerKm)
        if (effects == null) {
            toast("기어 P에서 선택할 수 있어요")
            return
        }
        handleSessionEffects(effects)
        if (SessionEffect.KeptSameDriver in effects) {
            viewModelScope.launch { showBriefing(driverId) }
        }
    }

    /** 재확인 화면의 '그대로 운전해요' */
    /** 환영 장면이 끝났거나 눌러서 넘김 */
    fun greetingDone() {
        _ui.update { it.copy(greeting = false) }
    }

    fun keepSameDriver() {
        val id = tracker?.currentSession?.driverId ?: return
        selectDriver(id)
    }

    /**
     * 브리핑 '확인했어요': 보여 준 메모를 읽음 처리하고, 이 시점을 '마지막 확인'으로 저장합니다.
     * 회사 차는 이때 고른 운행 목적을 이번 운전 구간에 적습니다(운행기록부).
     */
    fun acknowledgeBriefing(purpose: TripPurpose? = null) {
        val briefing = _ui.value.briefing ?: return
        val driverId = briefing.driver.id
        if (purpose != null) {
            tracker?.setPurpose(purpose)
            data = data.copy(activeSession = tracker?.currentSession)
        }
        var memos = data.memos
        val at = now()
        briefing.unresolvedMemos.forEach { memos = MemoBoard.markRead(memos, it.id, driverId, at) }
        // 인수 점검 기록: 이번 브리핑에서 이상을 신고하지 않았으면 '이상 없음'
        val inspection = Inspection(
            driverId = driverId,
            atMillis = at,
            ok = !foundIncidentThisBriefing,
            incidentId = if (foundIncidentThisBriefing) data.incidents.lastOrNull()?.id else null,
        )
        data = data.copy(
            memos = memos,
            lastSeen = data.lastSeen + (driverId to briefing.current),
            inspections = (data.inspections + inspection).takeLast(MAX_SESSIONS),
        )
        speaker.stop()
        _ui.update { it.copy(screen = Screen.HOME, briefing = null) }
        if (purpose != null) toast("${purpose.label} 운행으로 운행기록부에 적을게요")
        publishData()
        persist()
    }

    /** 회사 차: 주차 중 운행 목적 바꾸기 */
    fun setTripPurpose(purpose: TripPurpose) {
        if (tracker?.currentSession == null) return
        tracker?.setPurpose(purpose)
        data = data.copy(activeSession = tracker?.currentSession)
        toast("운행 목적을 ${KoreanText.toward(purpose.label)} 바꿨어요")
        publishData()
        persist()
    }

    fun replayBriefing() {
        val driver = currentDriver() ?: return
        viewModelScope.launch { showBriefing(driver.id) }
    }

    fun addMemo(preset: MemoPreset) {
        val author = currentDriver()?.id ?: Driver.GUEST_ID
        data = data.copy(memos = MemoBoard.add(data.memos, UUID.randomUUID().toString(), author, now(), preset = preset))
        toast("메모를 남겼어요. 다음 운전자에게 전달돼요")
        publishData()
        persist()
    }

    // ── 말로 남기는 메모 ───────────────────────────────────

    /** 마이크 버튼: 차량 마이크로 듣거나(Pleos), 가상 빌드에서는 예시 문장을 고르게 합니다. */
    fun startVoiceMemo() {
        voiceJob?.cancel()
        speaker.stop()
        if (!voice.isAvailable) {
            _ui.update { it.copy(voiceMemo = VoiceMemoUi(VoiceMemoUi.Stage.EXAMPLES)) }
            return
        }
        _ui.update { it.copy(voiceMemo = VoiceMemoUi(VoiceMemoUi.Stage.LISTENING)) }
        voiceJob = viewModelScope.launch {
            val result = voice.listen { partial ->
                _ui.update { s ->
                    val v = s.voiceMemo
                    if (v?.stage == VoiceMemoUi.Stage.LISTENING) s.copy(voiceMemo = v.copy(heard = partial)) else s
                }
            }
            when (result) {
                is VoiceResult.Heard -> onHeard(result.text)
                is VoiceResult.Failed -> _ui.update {
                    if (it.voiceMemo == null) it else it.copy(voiceMemo = VoiceMemoUi(VoiceMemoUi.Stage.FAILED, message = result.message))
                }
                VoiceResult.Cancelled -> Unit
            }
        }
    }

    /** 가상 빌드: 예시 문장을 말한 것처럼 */
    fun pickVoiceExample(text: String) = onHeard(text)

    private fun onHeard(text: String) {
        Log.i(TAG, "말로 남기는 메모: '$text'")
        val draft = VoiceMemoParser.parse(text, data.drivers.map { VoiceMemoParser.Person(it.id, it.name) })
        if (draft == null) {
            _ui.update {
                it.copy(voiceMemo = VoiceMemoUi(VoiceMemoUi.Stage.FAILED, heard = text, message = "무엇을 남길지 알아듣지 못했어요"))
            }
            return
        }
        val driving = _ui.value.drivingUi
        _ui.update {
            it.copy(
                voiceMemo = VoiceMemoUi(
                    VoiceMemoUi.Stage.CONFIRM, heard = text, draft = draft,
                    autoSaveIn = if (driving) AUTO_SAVE_SECONDS else null,
                ),
            )
        }
        if (driving) {
            // 주행 중에는 화면을 누르지 않아도 되게, 읽어 준 뒤 몇 초 뒤 저장합니다(취소 가능).
            speaker.speak("${draft.text}. 이렇게 남길게요")
            voiceJob = viewModelScope.launch {
                for (left in AUTO_SAVE_SECONDS downTo 1) {
                    _ui.update { s -> s.copy(voiceMemo = s.voiceMemo?.copy(autoSaveIn = left)) }
                    delay(1_000)
                }
                voiceJob = null
                saveVoiceMemo()
            }
        }
    }

    fun saveVoiceMemo() {
        val draft = _ui.value.voiceMemo?.draft ?: return
        voiceJob?.cancel()
        voiceJob = null
        val author = currentDriver()?.id ?: Driver.GUEST_ID
        data = data.copy(
            memos = MemoBoard.add(
                data.memos, UUID.randomUUID().toString(), author, now(),
                preset = draft.preset, text = draft.text, toDriverId = draft.toDriverId, spoken = true,
            ),
        )
        _ui.update { it.copy(voiceMemo = null) }
        val to = draft.toDriverId?.takeIf { it != author }?.let(::nameOf)
        toast(if (to != null) "${to}에게 메모를 남겼어요" else "메모를 남겼어요. 다음 운전자에게 전달돼요")
        if (_ui.value.drivingUi) speaker.speak("메모를 남겼어요")
        publishData()
        persist()
    }

    fun cancelVoiceMemo() {
        voiceJob?.cancel()
        voiceJob = null
        voice.cancel()
        _ui.update { it.copy(voiceMemo = null) }
    }

    fun resolveMemo(memoId: String, note: String) {
        val by = currentDriver()?.id ?: Driver.GUEST_ID
        data = data.copy(memos = MemoBoard.resolve(data.memos, memoId, by, note, now()))
        val b = _ui.value.briefing
        if (b != null) {
            val rebuilt = HandoverBriefingBuilder.build(b.driver, b.current, b.lastSeen, data.memos, ::nameOf, now(), data.incidents)
            _ui.update { it.copy(briefing = rebuilt) }
        }
        toast("해결로 기록했어요. 차량 경고등 상태는 따로 표시돼요")
        publishData()
        persist()
    }

    // ── 사고·파손 신고 / 인수 점검 ─────────────────────────

    /** 신고 화면 열기. AT_PICKUP = 넘겨받으며 발견, AFTER_DRIVE = 운행 후 스스로 신고 */
    fun startReport(source: IncidentSource) {
        if (_ui.value.drivingUi) return
        returnAfterReport = _ui.value.screen
        _ui.update { it.copy(screen = Screen.REPORT, reportSource = source) }
    }

    fun cancelReport() {
        _ui.update { it.copy(screen = returnAfterReport) }
    }

    fun submitIncident(type: IncidentType, zones: Set<CarZone>, alreadyHandled: Boolean) {
        if (_ui.value.screen != Screen.REPORT) return // 두 번 눌러도 한 건만
        val reporter = currentDriver()?.id ?: Driver.GUEST_ID
        val source = _ui.value.reportSource
        val at = now()
        val id = UUID.randomUUID().toString()
        var list = IncidentLog.report(
            data.incidents, id, reporter, type, zones, source, at,
            previousSession = if (source == IncidentSource.AT_PICKUP) data.sessions.lastOrNull() else null,
        )
        if (alreadyHandled) {
            list = IncidentLog.updateStatus(list, id, IncidentStatus.IN_PROGRESS, "보험·정비 접수", reporter, at)
        }
        data = data.copy(incidents = list.takeLast(MAX_INCIDENTS))
        if (source == IncidentSource.AT_PICKUP) foundIncidentThisBriefing = true
        _ui.update { it.copy(afterDriveChecked = true) }
        if (data.usage == CarUsage.COMPANY) {
            toast("관리자에게 보고했어요. 처리 완료 전까지 모든 운전자에게 보여요")
            speaker.speak("관리자에게 보고했어요. 처리가 끝날 때까지 다음 운전자에게도 알려 드릴게요.")
        } else {
            toast("신고했어요. 처리 완료 전까지 모든 운전자에게 보여요")
            speaker.speak("신고했어요. 처리가 끝날 때까지 다음 운전자에게도 알려 드릴게요.")
        }
        publishData()
        persist()
        // 브리핑에서 왔으면 같은 브리핑에 새 신고를 넣어 바로 돌아갑니다(인수 점검 계속).
        val back = returnAfterReport
        val b = _ui.value.briefing
        if (back == Screen.BRIEFING && b != null) {
            val rebuilt = HandoverBriefingBuilder.build(b.driver, b.current, b.lastSeen, data.memos, ::nameOf, at, data.incidents)
            _ui.update { it.copy(screen = Screen.BRIEFING, briefing = rebuilt) }
        } else {
            _ui.update { it.copy(screen = if (back == Screen.BRIEFING) Screen.HOME else back) }
        }
    }

    /** 주차 후 "차에 이상이 있나요?" → 이상 없어요 */
    fun markAfterDriveOk() {
        _ui.update { it.copy(afterDriveChecked = true) }
        toast("이상 없음으로 기록했어요")
    }

    fun updateIncidentStatus(id: String, status: IncidentStatus, note: String) {
        val by = currentDriver()?.id ?: Driver.GUEST_ID
        data = data.copy(incidents = IncidentLog.updateStatus(data.incidents, id, status, note, by, now()))
        val b = _ui.value.briefing
        if (b != null) {
            val rebuilt = HandoverBriefingBuilder.build(b.driver, b.current, b.lastSeen, data.memos, ::nameOf, now(), data.incidents)
            _ui.update { it.copy(briefing = rebuilt) }
        }
        toast(if (status == IncidentStatus.RESOLVED) "처리 완료로 바꿨어요" else "${KoreanText.toward(status.label)} 바꿨어요")
        publishData()
        persist()
    }

    /** '운전 끝내기': 다음 운전자에게 넘깁니다. */
    fun handOver() {
        val t = tracker ?: return
        handleSessionEffects(t.endSession(now(), _ui.value.snapshot?.odometerKm))
        _ui.update { it.copy(screen = Screen.PICKER, parkedPrompt = false) }
    }

    fun navigate(screen: Screen) {
        if (_ui.value.drivingUi) return
        _ui.update { it.copy(screen = screen, askingDriverChange = false, parkedPrompt = false, profileEdit = null) }
    }

    fun answerCareCheckIn() {
        val driver = currentDriver() ?: return
        handleCareEffects(careMonitor.onDriverAnswered(now()), driver)
        _ui.update { it.copy(careCheckIn = null) }
        speaker.speak("알겠어요. 안전 운전하세요.")
    }

    fun acceptShift() {
        _ui.update { it.copy(shiftAdvice = null) }
        viewModelScope.launch {
            val r = container.navigator.addNearestRestStop()
            toast(r.message)
            speaker.speak(if (r.added && r.stopName != null) "${KoreanText.toward(r.stopName)} 안내할게요." else r.message)
        }
    }

    fun declineShift() {
        _ui.update { it.copy(shiftAdvice = null) }
    }

    // ── 운전자 추가·고치기·삭제 (주차 중에만, 키보드는 차가 서 있을 때만 씁니다) ──

    /** driverId 가 null 이면 새 사람. 프로필 화면을 닫으면 연 화면으로 돌아갑니다. */
    fun openProfile(driverId: String?) {
        if (_ui.value.drivingUi) return
        if (driverId == null && !PartyRoster.canAdd(data.drivers)) {
            toast("한 차에 ${PartyRoster.MAX_DRIVERS}명까지 등록할 수 있어요")
            return
        }
        returnAfterProfile = _ui.value.screen.takeIf { it != Screen.PROFILE } ?: Screen.SETTINGS
        _ui.update { it.copy(screen = Screen.PROFILE, profileEdit = ProfileEdit(driverId), askingDriverChange = false) }
    }

    fun closeProfile() {
        _ui.update { it.copy(screen = returnAfterProfile, profileEdit = null) }
    }

    fun saveProfile(draft: Driver) {
        val edit = _ui.value.profileEdit ?: return
        val problem = PartyRoster.nameProblem(draft.name, data.drivers, selfId = edit.driverId)
        if (problem != null) {
            toast(problem)
            return
        }
        val name = draft.name.trim()
        data = if (edit.driverId == null) {
            data.copy(drivers = PartyRoster.add(data.drivers, draft, "d-" + UUID.randomUUID().toString().take(8)))
        } else {
            data.copy(drivers = PartyRoster.update(data.drivers, draft.copy(id = edit.driverId)))
        }
        toast(
            when {
                edit.driverId != null -> "${name}님 프로필을 저장했어요"
                returnAfterProfile == Screen.PICKER -> "${name}님을 추가했어요. 이름을 눌러 출발하세요"
                else -> "${name}님을 추가했어요"
            },
        )
        closeProfile()
        publishData()
        persist()
    }

    /**
     * 삭제: 지금 운전 중인 사람과 마지막 한 명은 지우지 않습니다.
     * 지난 운전 기록·운행기록부·메모의 이름은 남기고(회사 차 증빙), 직전 운전자 자동 지정에서는 뺍니다.
     */
    fun deleteDriver(driverId: String) {
        val name = nameOf(driverId)
        when {
            tracker?.currentSession?.driverId == driverId -> {
                toast("지금 운전 중인 분은 삭제할 수 없어요. '운전 끝내기' 뒤에 지워 주세요")
                return
            }
            data.drivers.size <= 1 -> {
                toast("최소 한 명은 있어야 해요")
                return
            }
        }
        val r = PartyRoster.remove(data.drivers, data.formerDrivers, driverId)
        tracker?.forgetDriver(driverId)
        data = data.copy(
            drivers = r.drivers,
            formerDrivers = r.former,
            conditions = data.conditions - driverId,
            lastSeen = data.lastSeen - driverId,
            lastDriverId = data.lastDriverId?.takeIf { it != driverId },
        )
        toast("${name}님을 삭제했어요. 지난 기록에는 이름이 남아요")
        closeProfile()
        publishData()
        persist()
    }

    /** 케어 모드는 운전자 본인의 동의로만 켜고 끕니다(화면에서 동의 확인 후 호출). */
    fun setCareMode(driverId: String, enabled: Boolean) {
        data = data.copy(drivers = data.drivers.map { if (it.id == driverId) it.copy(careModeEnabled = enabled) else it })
        if (currentDriver()?.id == driverId) careMonitor.reset()
        publishData()
        persist()
    }

    /** 큰 글씨 모드: 이 사람이 운전할 때만 차 화면의 글씨와 버튼을 키웁니다. */
    fun setLargeText(driverId: String, enabled: Boolean) {
        data = data.copy(drivers = data.drivers.map { if (it.id == driverId) it.copy(largeText = enabled) else it })
        toast(if (enabled) "${nameOf(driverId)}님이 운전할 때 글씨를 크게 보여 드려요" else "${nameOf(driverId)}님 큰 글씨를 껐어요")
        publishData()
        persist()
    }

    fun setDemoFast(enabled: Boolean) {
        data = data.copy(demoFastThresholds = enabled)
        careMonitor = CareModeMonitor(careConfig(enabled))
        publishData()
        persist()
    }

    fun setVoiceEnabled(enabled: Boolean) {
        speaker.enabled = enabled
        if (!enabled) speaker.stop()
        _ui.update { it.copy(voiceEnabled = enabled) }
    }

    /** 시연용: 연속 주행 시간을 앞당깁니다. */
    fun debugAddDrivingTime(minutes: Int) {
        drivingClock.addDrivingTime(minutes * 60_000L, night = localHour(now()) in 0..4)
        _ui.update { it.copy(continuousMillis = drivingClock.continuousMillis) }
    }

    /** 모든 기록을 지우고 시연용 기본 파티로 되돌립니다(주차 중에만). 가족 차/회사 차는 그대로 둡니다. */
    fun resetAllData() {
        if (startOver(data.usage)) toast("기록을 초기화했어요")
    }

    /**
     * 차량 용도 바꾸기(주차 중에만). 함께 쓰는 사람이 달라지므로 시연용 파티와 기록을 새로 시작합니다.
     * 회사 차는 지난 운행 몇 건을 지금 계기판 값에 이어 붙여 운행기록부를 보여 줍니다.
     */
    fun setUsage(usage: CarUsage) {
        if (usage == data.usage) return
        if (startOver(usage)) {
            toast(if (usage == CarUsage.COMPANY) "회사 차로 바꿨어요. 운행기록부를 자동으로 적어요" else "가족 차로 바꿨어요")
        }
    }

    private fun startOver(usage: CarUsage): Boolean {
        if (_ui.value.drivingUi) return false
        data = SeedData.forUsage(usage, _ui.value.snapshot?.odometerKm)
        val t = DriverSessionTracker(data.lastDriverId) { UUID.randomUUID().toString() }
        t.onGear(gateway.driving.value.gear, now(), null)
        tracker = t
        lastGear = gateway.driving.value.gear
        resetRealtimeMonitors()
        careMonitor = CareModeMonitor(careConfig(false))
        _ui.update { it.copy(screen = Screen.PICKER, briefing = null, askingDriverChange = false, parkedPrompt = false, profileEdit = null) }
        publishData()
        persist()
        return true
    }

    fun onPermissionsChanged() = gateway.onPermissionsChanged()

    fun clearToast() = _ui.update { it.copy(toast = null) }

    // ── 내부 도우미 ───────────────────────────────────────

    private fun resetRealtimeMonitors() {
        drivingClock.reset()
        shiftAdvisor.reset()
        careMonitor.reset()
        eventDetector.reset()
        _ui.update { it.copy(shiftAdvice = null, careCheckIn = null, careStage = CareModeMonitor.Stage.NORMAL) }
    }

    private fun publishData() {
        val t = tracker
        val session = t?.currentSession
        val awaitingLast = (t?.state as? SessionState.AwaitingDriver)?.lastDriverId
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todaySessions = (data.sessions + listOfNotNull(session)).filter { it.startedAtMillis >= todayStart }
        val todayEvents = data.careEvents.filter { it.atMillis >= todayStart }
        val summaries = data.drivers.filter { it.careModeEnabled }.map { d ->
            CareDailySummary.from(d.id, d.name, todaySessions, todayEvents, now()) { localHour(it) }
        }
        val today = LocalDate.now()
        val licenses = data.drivers.associate { d -> d.id to LicenseCheck.of(d, today.toEpochDay(), today.year) }
        val company = data.usage == CarUsage.COMPANY
        _ui.update {
            it.copy(
                usage = data.usage,
                vehicleName = data.vehicleName,
                licenses = licenses,
                incidents = data.incidents,
                drivers = data.drivers,
                formerDrivers = data.formerDrivers,
                currentDriver = session?.let { s -> driverById(s.driverId) },
                activeSession = session,
                sessionConfirmed = session?.confirmed ?: true,
                lastDriverId = session?.driverId ?: awaitingLast ?: data.lastDriverId,
                canSelectDriver = t?.canSelectDriver ?: true,
                memos = data.memos,
                conditions = if (company) emptyMap() else data.conditions,
                // 회사 차는 직원 건강 정보(컨디션)로 운전자를 추천하지 않습니다.
                recommendation = if (company) null else DriverRecommender.recommend(data.drivers, data.conditions),
                sessions = data.sessions,
                careEvents = data.careEvents,
                careSummaries = summaries,
                guardianAlerts = data.guardianAlerts,
                demoFastThresholds = data.demoFastThresholds,
            )
        }
    }

    /** 화면이 닫혀도 저장은 끝까지 하도록 앱 범위에서 실행합니다. */
    private fun persist() {
        val snapshot = data
        container.appScope.launch { container.store.save(snapshot) }
    }

    private fun toast(message: String) = _ui.update { it.copy(toast = message) }

    private fun currentDriver(): Driver? = tracker?.currentSession?.let { driverById(it.driverId) }

    private fun driverById(id: String): Driver =
        data.drivers.firstOrNull { it.id == id }
            ?: data.formerDrivers.firstOrNull { it.id == id }
            ?: if (id == Driver.GUEST_ID) Driver.GUEST else Driver(id = id, name = "알 수 없음")

    private fun nameOf(id: String): String = driverById(id).name

    private fun now(): Long = System.currentTimeMillis()

    private fun careConfig(fast: Boolean): CareModeMonitor.Config =
        if (fast) {
            CareModeMonitor.Config(
                recordAfterMillis = 10_000L,
                askAfterMillis = 15_000L,
                responseTimeoutMillis = 10_000L,
                guardianAfterWaypointMillis = 10_000L,
                cooldownAfterAnswerMillis = 30_000L,
            )
        } else {
            CareModeMonitor.Config()
        }

    override fun onCleared() {
        gateway.stop()
        container.uxMonitor.stop()
        speaker.stop()
    }

    companion object {
        private const val TAG = "CarRelayVM"
        private const val MAX_CARE_EVENTS = 500
        private const val MAX_SESSIONS = 300
        private const val MAX_ALERTS = 100
        private const val MAX_INCIDENTS = 200
        private const val AUTO_SAVE_SECONDS = 5

        fun localHour(epochMillis: Long): Int =
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).hour

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CarRelayApplication
                CarRelayViewModel(app.container)
            }
        }
    }
}
