package com.carrelay.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class IncidentType(val label: String) {
    COLLISION("접촉 사고"),
    SCRATCH("긁힘"),
    DENT("찌그러짐"),
    BROKEN("깨짐·파손"),
    OTHER("기타 이상"),
}

/** 차 위치. 차 그림에서 눌러 고릅니다. */
@Serializable
enum class CarZone(val label: String) {
    FRONT("앞"),
    LEFT("왼쪽"),
    RIGHT("오른쪽"),
    REAR("뒤"),
    INTERIOR("실내"),
}

@Serializable
enum class IncidentSource(val label: String) {
    /** 운전한 사람이 주차 후 스스로 신고 */
    AFTER_DRIVE("운행 후 신고"),

    /** 다음 운전자가 넘겨받으며 발견 */
    AT_PICKUP("인수 점검에서 발견"),
}

@Serializable
enum class IncidentStatus(val label: String) {
    OPEN("처리 필요"),
    IN_PROGRESS("처리 중"),
    RESOLVED("처리 완료"),
}

/**
 * 사고·파손 기록.
 *
 * 처리 완료가 될 때까지 모든 운전자의 브리핑에 계속 나옵니다(그냥 넘어가기 어렵게).
 * 인수 점검에서 발견된 경우 직전 운전자와 주차 시각을 함께 남겨 "언제부터"를 좁힙니다.
 * 누가 원인인지 단정하지는 않고, 확인할 대상만 보여 줍니다.
 */
@Serializable
data class Incident(
    val id: String,
    val reporterId: String,
    val type: IncidentType,
    val zones: Set<CarZone>,
    val source: IncidentSource,
    val createdAtMillis: Long,
    val previousDriverId: String? = null,
    val previousParkedAtMillis: Long? = null,
    val status: IncidentStatus = IncidentStatus.OPEN,
    val statusNote: String = "",
    val statusUpdatedAtMillis: Long? = null,
    val statusUpdatedBy: String? = null,
) {
    val isOpen: Boolean get() = status != IncidentStatus.RESOLVED

    val zonesLabel: String
        get() = if (zones.isEmpty()) "위치 미지정" else CarZone.entries.filter { it in zones }.joinToString("·") { it.label }

    /** "앞·왼쪽 긁힘" */
    val summary: String get() = "$zonesLabel ${type.label}"
}

/** 인수 점검 결과 */
@Serializable
data class Inspection(
    val driverId: String,
    val atMillis: Long,
    val ok: Boolean,
    val incidentId: String? = null,
)
