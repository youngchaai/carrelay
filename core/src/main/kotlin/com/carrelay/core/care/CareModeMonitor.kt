package com.carrelay.core.care

import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear
import kotlinx.serialization.Serializable

@Serializable
enum class CareReason(val label: String) {
    LONG_STOP_NO_INPUT("주행 기어 상태에서 오래 정차하고 조작이 없음"),
    REPEATED_HARSH_DECEL("짧은 시간에 급감속이 반복됨"),
}

@Serializable
data class CareEvent(
    val driverId: String,
    val type: Type,
    val atMillis: Long,
    val reason: CareReason? = null,
) {
    @Serializable
    enum class Type(val label: String) {
        HARSH_DECEL("급감속"),
        LONG_STOP_RECORDED("장시간 정차 기록"),
        CHECK_IN_ASKED("안부 확인"),
        CHECK_IN_ANSWERED("응답함"),
        RESOLVED_BY_DRIVER_ACTION("운전 조작으로 해제"),
        REST_STOP_WAYPOINT_ADDED("쉼터 경유지 추가"),
        GUARDIAN_NOTIFIED("보호자 알림"),
    }
}

sealed interface CareEffect {
    data class Record(val type: CareEvent.Type, val reason: CareReason? = null) : CareEffect

    /** 1단계 대응: 음성 + 큰 버튼으로 "괜찮으세요?" */
    data class AskAreYouOk(val reason: CareReason) : CareEffect

    /** 2단계 대응: 가까운 졸음쉼터·휴게소를 경유지로 추가 (NaviHelper SDK) */
    data class AddRestStopWaypoint(val reason: CareReason) : CareEffect

    /** 3단계 대응: 보호자 폰에 위치와 상황 알림. 위치는 이 단계에서만 보냅니다. */
    data class NotifyGuardian(val reason: CareReason) : CareEffect

    /** 안부 확인 화면을 닫습니다. */
    data object CloseCheckIn : CareEffect
}

/**
 * 케어 모드 주행 중 이상 징후 안부 확인(참가신청서 F6, '케어 모드 판단 로직(초안)').
 *
 * 실시간 판단에는 지연이 없는 차량 신호(기어·속도·페달)만 씁니다. 워치 데이터는 쓰지 않습니다.
 * 단일 신호가 아닌 조합으로 판단해 신호 대기마다 묻는 오경보를 줄입니다.
 *
 *  NORMAL ─(D/R·정차·페달 변화 없음 recordAfter)→ RECORDED ─(askAfter)→ ASKING
 *  NORMAL ─(급감속 N회/window)────────────────────────────────────→ ASKING
 *  ASKING ─(무응답 responseTimeout)→ WAYPOINT_ADDED ─(무응답 guardianAfter)→ GUARDIAN_NOTIFIED
 *  어느 단계든 "괜찮아요" 응답 또는 P 주차 → NORMAL.  장시간 정차 사유는 정상 주행 재개로도 해제.
 *
 * 임계값은 시연 테스트 후 조정하고, 이후 본인의 평소 2주 패턴을 기준선으로 바꿀 예정입니다.
 * "졸음 진단·질환 감지" 같은 의료적 표현은 쓰지 않습니다.
 */
class CareModeMonitor(private val config: Config = Config()) {

    data class Config(
        val recordAfterMillis: Long = 90_000L,
        val askAfterMillis: Long = 120_000L,
        val responseTimeoutMillis: Long = 30_000L,
        val guardianAfterWaypointMillis: Long = 60_000L,
        val cooldownAfterAnswerMillis: Long = 5 * 60_000L,
        val movingSpeedKmh: Double = 5.0,
        val harshDecelMps2: Double = 3.5,
        val harshDecelDebounceMillis: Long = 3_000L,
        val harshDecelWindowMillis: Long = 5 * 60_000L,
        val harshDecelCountToAsk: Int = 3,
        val maxSampleGapForDecelMillis: Long = 3_000L,
    )

    enum class Stage { NORMAL, RECORDED, ASKING, WAYPOINT_ADDED, GUARDIAN_NOTIFIED }

    var stage: Stage = Stage.NORMAL
        private set
    var activeReason: CareReason? = null
        private set

    private var stageEnteredAt = 0L
    private var stopStartedAt: Long? = null
    private var lastBrake: Boolean? = null
    private var lastAccel: Boolean? = null
    private var previous: DrivingSample? = null
    private var lastHarshAt: Long? = null
    private val harshDecels = ArrayDeque<Long>()
    private var suppressAskUntil = 0L

    /**
     * @param detectDecel 차량에서 온 실제 샘플이면 true. 화면 타이머가 시간만 앞당기려고 다시 넣는
     *   샘플이면 false 로 두어, 샘플 간격이 짧아져 급감속으로 잘못 계산되는 일을 막습니다.
     */
    fun onSample(sample: DrivingSample, detectDecel: Boolean = true): List<CareEffect> {
        val now = sample.timestampMillis
        val effects = mutableListOf<CareEffect>()

        while (harshDecels.isNotEmpty() && now - harshDecels.first() > config.harshDecelWindowMillis) {
            harshDecels.removeFirst()
        }
        if (detectDecel) detectHarshDecel(sample, effects)
        val stoppedFor = updateStopTimer(sample)

        // 주차하면 운전자가 차를 조작한 것이므로 해제
        if (sample.gear == Gear.PARK) {
            if (stage != Stage.NORMAL) {
                effects += CareEffect.Record(CareEvent.Type.RESOLVED_BY_DRIVER_ACTION, activeReason)
                if (stage != Stage.RECORDED) effects += CareEffect.CloseCheckIn
            }
            resetToNormal()
            return effects
        }

        when (stage) {
            Stage.NORMAL -> when {
                now < suppressAskUntil -> Unit
                harshDecels.size >= config.harshDecelCountToAsk -> ask(CareReason.REPEATED_HARSH_DECEL, now, effects)
                stoppedFor >= config.recordAfterMillis -> {
                    stage = Stage.RECORDED
                    stageEnteredAt = now
                    effects += CareEffect.Record(CareEvent.Type.LONG_STOP_RECORDED, CareReason.LONG_STOP_NO_INPUT)
                }
            }
            Stage.RECORDED -> when {
                stoppedFor < config.recordAfterMillis -> resetToNormal() // 움직였거나 페달을 밟음
                stoppedFor >= config.askAfterMillis -> ask(CareReason.LONG_STOP_NO_INPUT, now, effects)
            }
            Stage.ASKING, Stage.WAYPOINT_ADDED, Stage.GUARDIAN_NOTIFIED -> {
                if (activeReason == CareReason.LONG_STOP_NO_INPUT && isMovingNormally(sample)) {
                    effects += CareEffect.Record(CareEvent.Type.RESOLVED_BY_DRIVER_ACTION, activeReason)
                    effects += CareEffect.CloseCheckIn
                    resetToNormal()
                } else {
                    escalateIfNoResponse(now, effects)
                }
            }
        }
        return effects
    }

    /** 운전자가 음성 또는 큰 버튼으로 "괜찮아요"라고 응답했습니다. */
    fun onDriverAnswered(nowMillis: Long): List<CareEffect> {
        if (stage == Stage.NORMAL || stage == Stage.RECORDED) return emptyList()
        val reason = activeReason
        resetToNormal()
        suppressAskUntil = nowMillis + config.cooldownAfterAnswerMillis
        stopStartedAt = nowMillis
        return listOf(
            CareEffect.Record(CareEvent.Type.CHECK_IN_ANSWERED, reason),
            CareEffect.CloseCheckIn,
        )
    }

    /** 운전자가 바뀌거나 케어 모드가 꺼지면 호출합니다. */
    fun reset() {
        resetToNormal()
        previous = null
        lastHarshAt = null
        stopStartedAt = null
        lastBrake = null
        lastAccel = null
        suppressAskUntil = 0L
    }

    private fun escalateIfNoResponse(now: Long, effects: MutableList<CareEffect>) {
        val reason = activeReason ?: return
        val elapsed = now - stageEnteredAt
        when (stage) {
            Stage.ASKING -> if (elapsed >= config.responseTimeoutMillis) {
                stage = Stage.WAYPOINT_ADDED
                stageEnteredAt = now
                effects += CareEffect.AddRestStopWaypoint(reason)
                effects += CareEffect.Record(CareEvent.Type.REST_STOP_WAYPOINT_ADDED, reason)
            }
            Stage.WAYPOINT_ADDED -> if (elapsed >= config.guardianAfterWaypointMillis) {
                stage = Stage.GUARDIAN_NOTIFIED
                stageEnteredAt = now
                effects += CareEffect.NotifyGuardian(reason)
                effects += CareEffect.Record(CareEvent.Type.GUARDIAN_NOTIFIED, reason)
            }
            else -> Unit
        }
    }

    private fun ask(reason: CareReason, now: Long, effects: MutableList<CareEffect>) {
        stage = Stage.ASKING
        activeReason = reason
        stageEnteredAt = now
        harshDecels.clear()
        effects += CareEffect.AskAreYouOk(reason)
        effects += CareEffect.Record(CareEvent.Type.CHECK_IN_ASKED, reason)
    }

    private fun resetToNormal() {
        stage = Stage.NORMAL
        activeReason = null
        harshDecels.clear()
    }

    private fun isMovingNormally(sample: DrivingSample): Boolean =
        (sample.speedKmh ?: 0.0) >= config.movingSpeedKmh

    /** D/R 상태로 정차해 페달 변화가 없는 시간을 돌려줍니다. 조건이 아니면 0. */
    private fun updateStopTimer(s: DrivingSample): Long {
        val speed = s.speedKmh
        val stoppedInDriveGear = s.gear.isDriveGear && speed != null && speed < 1.0
        val pedalChanged = changed(lastBrake, s.brakePedalPressed) || changed(lastAccel, s.acceleratorPressed)
        lastBrake = s.brakePedalPressed
        lastAccel = s.acceleratorPressed

        if (!stoppedInDriveGear) {
            stopStartedAt = null
            return 0L
        }
        val started = stopStartedAt
        if (started == null || pedalChanged) {
            stopStartedAt = s.timestampMillis
            return 0L
        }
        return s.timestampMillis - started
    }

    private fun changed(before: Boolean?, after: Boolean?): Boolean =
        before != null && after != null && before != after

    private fun detectHarshDecel(s: DrivingSample, effects: MutableList<CareEffect>) {
        val p = previous
        previous = s
        val now = s.timestampMillis
        if (p == null) return
        val v0 = p.speedKmh ?: return
        val v1 = s.speedKmh ?: return
        val dtMillis = now - p.timestampMillis
        if (dtMillis <= 0L || dtMillis > config.maxSampleGapForDecelMillis) return

        val decelMps2 = (v0 - v1) / 3.6 / (dtMillis / 1000.0)
        val last = lastHarshAt
        if (decelMps2 >= config.harshDecelMps2 && (last == null || now - last >= config.harshDecelDebounceMillis)) {
            lastHarshAt = now
            harshDecels.addLast(now)
            effects += CareEffect.Record(CareEvent.Type.HARSH_DECEL)
        }
    }
}
