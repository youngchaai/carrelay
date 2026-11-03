package com.carrelay.vehicle.ui

import com.carrelay.core.care.CareDailySummary
import com.carrelay.core.care.CareEvent
import com.carrelay.core.care.CareModeMonitor
import com.carrelay.core.care.CareReason
import com.carrelay.core.handover.Briefing
import com.carrelay.core.memo.VoiceMemoDraft
import com.carrelay.core.model.CarUsage
import com.carrelay.core.triplog.TripLog
import com.carrelay.core.triplog.TripLogRow
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentSource
import com.carrelay.core.model.Memo
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.profile.LicenseInfo
import com.carrelay.core.shift.DriverRecommendation
import com.carrelay.core.shift.ShiftAdvice
import com.carrelay.vehicle.data.GuardianAlert

enum class Screen {
    /** 누가 운전하시나요? */
    PICKER,

    /** 인수인계 브리핑 + 인수 점검 */
    BRIEFING,

    /** 주차 중 기본 화면 */
    HOME,

    /** 다음 운전자에게 메모 남기기 / 주차 직후 마무리 */
    MEMO,

    /** 사고·파손 신고 */
    REPORT,

    /** 주행 중: 음성 + 큰 버튼만 */
    DRIVING,

    /** 운전자별 기록 · 케어 요약 · 사고 이력 */
    RECORDS,

    /** 프로필 · 케어 모드 · 시연 설정 */
    SETTINGS,

    /** 운전자(가족·직원) 추가·고치기·삭제 (주차 중에만) */
    PROFILE,
}

/** 프로필 화면에서 고치는 사람. driverId 가 null 이면 새 사람 */
data class ProfileEdit(val driverId: String?)

data class UiState(
    val loading: Boolean = true,
    val screen: Screen = Screen.PICKER,
    val sourceName: String = "",
    val isSimulated: Boolean = true,
    val vehicleName: String = "",
    /** 가족 차 / 회사 차 */
    val usage: CarUsage = CarUsage.FAMILY,

    val drivers: List<Driver> = emptyList(),
    /** 삭제한 사람 (기록에 이름을 보여 주는 데만 씀) */
    val formerDrivers: List<Driver> = emptyList(),
    val profileEdit: ProfileEdit? = null,
    val currentDriver: Driver? = null,
    /** 진행 중인 운전 구간 (회사 차: 운행 목적, 운행기록부의 '운행 중' 줄) */
    val activeSession: DriveSession? = null,
    /** 현재 운전 구간을 운전자가 직접 선택했는지(false 면 자동 지정·미확인) */
    val sessionConfirmed: Boolean = true,
    val lastDriverId: String? = null,
    val canSelectDriver: Boolean = true,
    /** 주차 중 문 개폐 후 "운전자 바뀌셨나요?" */
    val askingDriverChange: Boolean = false,
    /** 주차 직후 메모 화면으로 왔는지 */
    val parkedPrompt: Boolean = false,
    /** 주차 직후: 이번 운전 구간의 급정거 횟수 */
    val parkedHarshDecel: Int = 0,
    /** 주차 직후 "차에 이상이 있나요?"에 답했는지 */
    val afterDriveChecked: Boolean = false,

    val briefing: Briefing? = null,
    /** 새 운전자가 막 탔을 때 브리핑 앞에 잠깐 보여 주는 환영 장면 */
    val greeting: Boolean = false,
    val snapshot: VehicleSnapshot? = null,
    val driving: DrivingSample? = null,
    /** 주행 규제 UI(음성 + 큰 버튼)를 써야 하는지 */
    val drivingUi: Boolean = false,
    val continuousMillis: Long = 0L,

    val recommendation: DriverRecommendation? = null,
    val conditions: Map<String, DriverCondition> = emptyMap(),
    val licenses: Map<String, LicenseInfo> = emptyMap(),
    val shiftAdvice: ShiftAdvice? = null,
    val careCheckIn: CareReason? = null,
    /** 안부 확인 진행 단계 (질문 중 → 쉼터 추가 → 보호자 알림) */
    val careStage: CareModeMonitor.Stage = CareModeMonitor.Stage.NORMAL,

    val memos: List<Memo> = emptyList(),
    val sessions: List<DriveSession> = emptyList(),
    val careEvents: List<CareEvent> = emptyList(),
    val careSummaries: List<CareDailySummary> = emptyList(),
    val guardianAlerts: List<GuardianAlert> = emptyList(),
    val incidents: List<Incident> = emptyList(),

    /** 사고·파손 신고 화면을 연 경로 */
    val reportSource: IncidentSource = IncidentSource.AFTER_DRIVE,

    val demoFastThresholds: Boolean = false,
    val voiceEnabled: Boolean = true,
    val toast: String? = null,

    /** 말로 남기는 메모 (null 이면 닫힘) */
    val voiceMemo: VoiceMemoUi? = null,
)

/** 말로 남기는 메모 진행 상태 */
data class VoiceMemoUi(
    val stage: Stage,
    /** 들은 말 (듣는 동안에는 중간 결과) */
    val heard: String = "",
    val draft: VoiceMemoDraft? = null,
    val message: String? = null,
    /** 주행 중에는 버튼을 누르지 않아도 몇 초 뒤 저장합니다 */
    val autoSaveIn: Int? = null,
) {
    enum class Stage {
        /** 차량 마이크로 듣는 중 */
        LISTENING,

        /** 마이크가 없는 가상 빌드: 예시 문장 고르기 */
        EXAMPLES,

        /** 이렇게 남길까요? */
        CONFIRM,

        /** 알아듣지 못함 */
        FAILED,
    }
}

fun UiState.nameOf(driverId: String): String =
    drivers.firstOrNull { it.id == driverId }?.name
        ?: formerDrivers.firstOrNull { it.id == driverId }?.name
        ?: if (driverId == Driver.GUEST_ID) Driver.GUEST.name else "알 수 없음"

/** 지금 파티원 또는 삭제한 사람 (기록 표시용) */
fun UiState.anyDriver(driverId: String): Driver? =
    drivers.firstOrNull { it.id == driverId }
        ?: formerDrivers.firstOrNull { it.id == driverId }
        ?: if (driverId == Driver.GUEST_ID) Driver.GUEST else null

/** 가장 최근에 끝난 운전 구간 */
val UiState.lastEndedSession: DriveSession? get() = sessions.lastOrNull()

val UiState.isCompany: Boolean get() = usage == CarUsage.COMPANY

/** 운행기록부: 끝난 운행 + 진행 중인 운행(지금 계기판 값까지) */
fun UiState.tripLogRows(): List<TripLogRow> =
    TripLog.rows(sessions, activeSession, snapshot?.odometerKm) { id -> anyDriver(id) }
