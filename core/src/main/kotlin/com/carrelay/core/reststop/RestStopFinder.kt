package com.carrelay.core.reststop

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 휴게소(SERVICE) 또는 졸음쉼터(SHELTER) */
data class RestStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: Kind,
) {
    enum class Kind { SERVICE, SHELTER }
}

data class RestStopPick(
    val stop: RestStop,
    val distanceKm: Double,
    /** 진행 방향 앞쪽에 있는지(방향을 몰랐거나 앞쪽에 없어 가장 가까운 곳을 골랐으면 false) */
    val ahead: Boolean,
)

/**
 * 가까운 쉼터 고르기.
 * 고속도로는 방향마다 휴게소가 따로 있어, 진행 방향(heading)을 알면 앞쪽(±[AHEAD_DEGREES])에 있는 곳 중 가장 가까운 곳을 고릅니다.
 * 앞쪽에 없으면 거리만 보고 가장 가까운 곳을 고릅니다. [maxKm] 밖은 고르지 않습니다.
 */
object RestStopFinder {
    const val AHEAD_DEGREES = 60.0
    const val DEFAULT_MAX_KM = 50.0
    private const val EARTH_RADIUS_KM = 6371.0

    fun nearest(
        stops: List<RestStop>,
        latitude: Double,
        longitude: Double,
        headingDeg: Double?,
        maxKm: Double = DEFAULT_MAX_KM,
        minAheadKm: Double = 0.3,
    ): RestStopPick? {
        val within = stops
            .map { it to distanceKm(latitude, longitude, it.latitude, it.longitude) }
            .filter { (_, d) -> d <= maxKm }
        if (within.isEmpty()) return null

        if (headingDeg != null) {
            val ahead = within
                .filter { (s, d) -> d >= minAheadKm && angleDiff(bearingDeg(latitude, longitude, s.latitude, s.longitude), headingDeg) <= AHEAD_DEGREES }
                .minByOrNull { (_, d) -> d }
            if (ahead != null) return RestStopPick(ahead.first, ahead.second, ahead = true)
        }
        val closest = within.minBy { (_, d) -> d }
        return RestStopPick(closest.first, closest.second, ahead = false)
    }

    /** 두 지점 사이 거리 (km, 대원 거리) */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    /** 1 → 2 방향 (북쪽 0°, 시계 방향) */
    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs((a - b) % 360.0)
        return if (d > 180.0) 360.0 - d else d
    }
}
