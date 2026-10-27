package com.carrelay.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Gear(val label: String) {
    PARK("P"), REVERSE("R"), NEUTRAL("N"), DRIVE("D"), UNKNOWN("-");

    /** 주행 기어(D·R): 케어 모드의 '주행 기어 상태 정차' 판단에 씁니다. */
    val isDriveGear: Boolean get() = this == DRIVE || this == REVERSE
}

/**
 * 주행 중 실시간 판단에 쓰는 차량 신호 묶음. 값이 null 이면 해당 신호를 받지 못한 상태입니다.
 */
@Serializable
data class DrivingSample(
    val timestampMillis: Long,
    val gear: Gear,
    val speedKmh: Double? = null,
    val brakePedalPressed: Boolean? = null,
    val acceleratorPressed: Boolean? = null,
) {
    val isStopped: Boolean get() = (speedKmh ?: 0.0) < 1.0
}

/** 운전석 문 이벤트. */
enum class DoorEvent { OPENED, CLOSED }

/**
 * 한 명의 운전 구간.
 *
 * @property confirmed 운전자가 직접 선택했으면 true. 선택 없이 출발해 직전 운전자로 자동 지정되면 false.
 *   자동 지정된 기록은 리포트에서 '미확인'으로 따로 표시합니다.
 */
@Serializable
data class DriveSession(
    val id: String,
    val driverId: String,
    val confirmed: Boolean,
    val startedAtMillis: Long,
    val startOdometerKm: Double? = null,
    val endedAtMillis: Long? = null,
    val endOdometerKm: Double? = null,
) {
    val distanceKm: Double?
        get() = if (startOdometerKm != null && endOdometerKm != null) {
            (endOdometerKm - startOdometerKm).coerceAtLeast(0.0)
        } else {
            null
        }
}
