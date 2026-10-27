package com.carrelay.vehicle.ui

import com.carrelay.core.care.CareDailySummary
import com.carrelay.core.care.CareEvent
import com.carrelay.core.care.CareReason
import com.carrelay.core.handover.Briefing
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Memo
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.shift.DriverRecommendation
import com.carrelay.core.shift.ShiftAdvice
import com.carrelay.vehicle.data.GuardianAlert

enum class Screen {
    /** 누가 운전하시나요? */
    PICKER,

    /** 인수인계 브리핑 */
    BRIEFING,

    /** 주차 중 기본 화면 */
    HOME,

    /** 다음 운전자에게 메모 남기기 */
    MEMO,

    /** 주행 중: 음성 + 큰 버튼만 */
    DRIVING,

    /** 운전자별 기록 · 케어 요약 */
    RECORDS,

    /** 프로필 · 케어 모드 · 시연 설정 */
    SETTINGS,
}

data class UiState(
    val loading: Boolean = true,
    val screen: Screen = Screen.PICKER,
    val sourceName: String = "",
    val isSimulated: Boolean = true,

    val drivers: List<Driver> = emptyList(),
    val currentDriver: Driver? = null,
    /** 현재 운전 구간을 운전자가 직접 선택했는지(false 면 자동 지정·미확인) */
    val sessionConfirmed: Boolean = true,
    val lastDriverId: String? = null,
    val canSelectDriver: Boolean = true,
    /** 주차 중 문 개폐 후 "운전자 바뀌셨나요?" */
    val askingDriverChange: Boolean = false,
    /** 주차 직후 메모 화면으로 왔는지 */
    val parkedPrompt: Boolean = false,

    val briefing: Briefing? = null,
    val snapshot: VehicleSnapshot? = null,
    val driving: DrivingSample? = null,
    /** 주행 규제 UI(음성 + 큰 버튼)를 써야 하는지 */
    val drivingUi: Boolean = false,
    val continuousMillis: Long = 0L,

    val recommendation: DriverRecommendation? = null,
    val conditions: Map<String, DriverCondition> = emptyMap(),
    val shiftAdvice: ShiftAdvice? = null,
    val careCheckIn: CareReason? = null,

    val memos: List<Memo> = emptyList(),
    val sessions: List<DriveSession> = emptyList(),
    val careEvents: List<CareEvent> = emptyList(),
    val careSummaries: List<CareDailySummary> = emptyList(),
    val guardianAlerts: List<GuardianAlert> = emptyList(),

    val demoFastThresholds: Boolean = false,
    val voiceEnabled: Boolean = true,
    val toast: String? = null,
)

fun UiState.nameOf(driverId: String): String =
    drivers.firstOrNull { it.id == driverId }?.name
        ?: if (driverId == Driver.GUEST_ID) Driver.GUEST.name else "알 수 없음"
