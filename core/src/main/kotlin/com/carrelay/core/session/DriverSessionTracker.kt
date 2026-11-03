package com.carrelay.core.session

import com.carrelay.core.model.DoorEvent
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.Gear
import com.carrelay.core.model.TripPurpose

sealed interface SessionState {
    /** 운전자 선택 대기(주차 중). */
    data class AwaitingDriver(val lastDriverId: String?) : SessionState

    /** 운전 구간 진행 중. */
    data class Active(val session: DriveSession) : SessionState

    /** 주차 중 운전석 문이 열렸다 닫힘 → "운전자 바뀌셨나요?" 재확인 중. */
    data class ConfirmingChange(val session: DriveSession) : SessionState
}

sealed interface SessionEffect {
    /** 이 운전자에게 인수인계 브리핑을 보여 줍니다. */
    data class ShowBriefing(val driverId: String) : SessionEffect

    /** 선택 없이 출발해 직전 운전자로 자동 지정했습니다(미확인 기록). */
    data class AutoAssigned(val session: DriveSession) : SessionEffect

    /** 운전 구간이 끝났습니다(저장 대상). */
    data class SessionEnded(val session: DriveSession) : SessionEffect

    /** "운전자 바뀌셨나요?" 화면을 띄웁니다. */
    data object AskDriverChanged : SessionEffect

    /** 같은 운전자가 계속 운전합니다. */
    data object KeptSameDriver : SessionEffect
}

/**
 * 운전자 선택·자동 지정·교대 감지를 담당하는 상태 머신.
 *
 * 규칙(참가신청서 F2·F4 + 아이디어 검토 반영)
 * - 운전자 선택은 기어 P(또는 기어 정보를 알 수 없을 때)에서만 받습니다.
 * - 선택 없이 출발하면 직전 운전자로 자동 지정하되 confirmed=false(미확인)로 남깁니다.
 * - 기어 P에서 운전석 문이 열렸다 닫히면 운전자 변경 여부를 다시 묻습니다.
 *   (짐을 꺼내는 등 교대가 아닌 경우도 있으므로 '그대로' 선택지를 함께 둡니다.)
 * - 재확인 중에 응답 없이 출발하면 같은 운전자가 계속 운전하는 것으로 봅니다.
 */
class DriverSessionTracker(
    initialLastDriverId: String?,
    private val newId: () -> String,
) {
    var state: SessionState = SessionState.AwaitingDriver(initialLastDriverId)
        private set

    var gear: Gear = Gear.UNKNOWN
        private set

    private var inDrive = false
    private var driverDoorOpened = false

    val currentSession: DriveSession?
        get() = when (val s = state) {
            is SessionState.Active -> s.session
            is SessionState.ConfirmingChange -> s.session
            is SessionState.AwaitingDriver -> null
        }

    val canSelectDriver: Boolean get() = gear == Gear.PARK || gear == Gear.UNKNOWN

    /**
     * 운전자를 선택합니다. 주행 중이면 null 을 돌려주고 아무것도 바꾸지 않습니다.
     */
    fun selectDriver(driverId: String, nowMillis: Long, odometerKm: Double?): List<SessionEffect>? {
        if (!canSelectDriver) return null
        return when (val s = state) {
            is SessionState.AwaitingDriver -> {
                state = SessionState.Active(newSession(driverId, true, nowMillis, odometerKm))
                listOf(SessionEffect.ShowBriefing(driverId))
            }
            is SessionState.Active -> switchOrKeep(s.session, driverId, nowMillis, odometerKm)
            is SessionState.ConfirmingChange -> switchOrKeep(s.session, driverId, nowMillis, odometerKm)
        }
    }

    fun onGear(newGear: Gear, nowMillis: Long, odometerKm: Double?): List<SessionEffect> {
        gear = newGear
        if (newGear == Gear.PARK) {
            inDrive = false
            return emptyList()
        }
        if (!newGear.isDriveGear || inDrive) return emptyList()

        // P(또는 N) → D/R : 출발
        inDrive = true
        driverDoorOpened = false
        return when (val s = state) {
            is SessionState.AwaitingDriver -> {
                val session = newSession(s.lastDriverId ?: Driver.GUEST_ID, false, nowMillis, odometerKm)
                state = SessionState.Active(session)
                listOf(SessionEffect.AutoAssigned(session))
            }
            is SessionState.ConfirmingChange -> {
                state = SessionState.Active(s.session)
                listOf(SessionEffect.KeptSameDriver)
            }
            is SessionState.Active -> emptyList()
        }
    }

    fun onDriverDoor(event: DoorEvent): List<SessionEffect> {
        if (gear != Gear.PARK) return emptyList()
        return when (event) {
            DoorEvent.OPENED -> {
                driverDoorOpened = true
                emptyList()
            }
            DoorEvent.CLOSED -> {
                if (!driverDoorOpened) return emptyList()
                driverDoorOpened = false
                val s = state
                if (s is SessionState.Active) {
                    state = SessionState.ConfirmingChange(s.session)
                    listOf(SessionEffect.AskDriverChanged)
                } else {
                    emptyList()
                }
            }
        }
    }

    /** 진행 중인 운전 구간에 급정거·급가속을 더합니다. */
    fun addDrivingEvent(harshDecel: Int = 0, harshAccel: Int = 0) {
        fun DriveSession.bump() = copy(
            harshDecelCount = harshDecelCount + harshDecel,
            harshAccelCount = harshAccelCount + harshAccel,
        )
        state = when (val s = state) {
            is SessionState.Active -> SessionState.Active(s.session.bump())
            is SessionState.ConfirmingChange -> SessionState.ConfirmingChange(s.session.bump())
            is SessionState.AwaitingDriver -> s
        }
    }

    /**
     * 파티에서 삭제된 사람을 '직전 운전자'에서 지웁니다.
     * 그러지 않으면 아무도 고르지 않고 출발했을 때 지운 사람에게 운전 구간이 자동으로 붙습니다.
     */
    fun forgetDriver(driverId: String) {
        val s = state
        if (s is SessionState.AwaitingDriver && s.lastDriverId == driverId) state = SessionState.AwaitingDriver(null)
    }

    /** 회사 차: 지금 운전 구간의 운행 목적을 정합니다(출발 전 선택, 주차 중 변경). */
    fun setPurpose(purpose: TripPurpose?) {
        state = when (val s = state) {
            is SessionState.Active -> SessionState.Active(s.session.copy(purpose = purpose))
            is SessionState.ConfirmingChange -> SessionState.ConfirmingChange(s.session.copy(purpose = purpose))
            is SessionState.AwaitingDriver -> s
        }
    }

    /** 앱 재시작 후 저장해 둔 진행 중 운전 구간을 되살립니다. */
    fun restore(session: DriveSession) {
        state = SessionState.Active(session)
    }

    /** "운전 끝내기": 다음 운전자에게 넘깁니다. */
    fun endSession(nowMillis: Long, odometerKm: Double?): List<SessionEffect> {
        val session = currentSession ?: return emptyList()
        val ended = session.copy(endedAtMillis = nowMillis, endOdometerKm = odometerKm)
        state = SessionState.AwaitingDriver(lastDriverId = session.driverId)
        return listOf(SessionEffect.SessionEnded(ended))
    }

    private fun switchOrKeep(
        session: DriveSession,
        driverId: String,
        nowMillis: Long,
        odometerKm: Double?,
    ): List<SessionEffect> {
        if (session.driverId == driverId) {
            state = SessionState.Active(session.copy(confirmed = true))
            return listOf(SessionEffect.KeptSameDriver)
        }
        val ended = session.copy(endedAtMillis = nowMillis, endOdometerKm = odometerKm)
        state = SessionState.Active(newSession(driverId, true, nowMillis, odometerKm))
        return listOf(SessionEffect.SessionEnded(ended), SessionEffect.ShowBriefing(driverId))
    }

    private fun newSession(driverId: String, confirmed: Boolean, nowMillis: Long, odometerKm: Double?) =
        DriveSession(
            id = newId(),
            driverId = driverId,
            confirmed = confirmed,
            startedAtMillis = nowMillis,
            startOdometerKm = odometerKm,
        )
}
