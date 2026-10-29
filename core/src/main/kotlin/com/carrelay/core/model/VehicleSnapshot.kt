package com.carrelay.core.model

import kotlinx.serialization.Serializable

/** 브리핑에 쓰는 차량 신호 종류. */
@Serializable
enum class Signal(val label: String) {
    FUEL("연료"),
    EV_BATTERY("배터리"),
    RANGE("주행 가능 거리"),
    ODOMETER("누적 주행거리"),
    WARNINGS("경고등"),
}

/** 신호를 읽지 못한 이유. 값이 없는 것과 읽기 실패를 구분합니다. */
@Serializable
enum class SignalStatus(val label: String) {
    UNSUPPORTED("이 차량은 지원하지 않아요"),
    FAILED("값을 읽지 못했어요"),
    NO_PERMISSION("이 앱에서는 볼 수 없어요"),
}

/** 브리핑에 표시하는 경고등·소모품 경고. */
@Serializable
enum class WarningLight(val label: String, val severity: Int) {
    ENGINE_CHECK("엔진 경고등", 3),
    BRAKE("브레이크 경고등", 3),
    BATTERY_CHARGE("충전 경고등", 3),
    TIRE_PRESSURE("타이어 공기압 경고", 2),
    ENGINE_OIL("엔진오일 부족", 2),
    WASHER_FLUID("워셔액 부족", 1),
}

/**
 * 한 시점에 읽은 차량 상태.
 *
 * 값이 null 이면 [unavailable] 에서 이유를 확인합니다.
 * [capturedAtMillis] 는 앱이 값을 받은 시각입니다(차량 내부 측정 시각이 아님).
 */
@Serializable
data class VehicleSnapshot(
    val capturedAtMillis: Long,
    val fuelPercent: Int? = null,
    val evBatteryPercent: Int? = null,
    val rangeKm: Int? = null,
    val odometerKm: Double? = null,
    val activeWarnings: Set<WarningLight> = emptySet(),
    val unavailable: Map<Signal, SignalStatus> = emptyMap(),
) {
    /** 연료차면 연료, 전기차면 배터리 잔량. */
    val energyPercent: Int? get() = fuelPercent ?: evBatteryPercent
    val energyLabel: String get() = if (fuelPercent == null && evBatteryPercent != null) "배터리" else "연료"

    fun isStale(nowMillis: Long, maxAgeMillis: Long = STALE_AFTER_MILLIS): Boolean =
        nowMillis - capturedAtMillis > maxAgeMillis

    companion object {
        const val STALE_AFTER_MILLIS: Long = 5 * 60 * 1000L
    }
}
