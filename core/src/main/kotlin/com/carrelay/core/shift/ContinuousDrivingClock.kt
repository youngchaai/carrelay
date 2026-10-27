package com.carrelay.core.shift

import com.carrelay.core.model.DrivingSample
import com.carrelay.core.model.Gear

/**
 * 연속 주행시간과 심야(00~05시) 연속 주행시간을 잽니다.
 *
 * - 기어가 P가 아니면 주행 시간으로 셉니다(신호 대기 포함).
 * - [restResetMillis] 이상 주차하면 휴식한 것으로 보고 0으로 되돌립니다.
 * - 샘플 사이 간격이 [maxGapMillis] 보다 크면 그만큼만 셉니다(앱이 백그라운드였던 경우 대비).
 *
 * Pleos Vehicle SDK 의 Odometer registerTripCurrentDrivingTime 을 쓸 수 있으면 그 값을 우선 사용하고,
 * 이 클래스는 SDK 미지원 시 대체 수단으로 씁니다.
 */
class ContinuousDrivingClock(
    private val hourOf: (epochMillis: Long) -> Int,
    private val restResetMillis: Long = 10 * 60_000L,
    private val maxGapMillis: Long = 60_000L,
) {
    var continuousMillis: Long = 0L
        private set
    var nightMillis: Long = 0L
        private set

    private var parkedMillis = 0L
    private var lastAt: Long? = null

    fun onSample(sample: DrivingSample) {
        val prev = lastAt
        if (prev != null && sample.timestampMillis < prev) return // 늦게 도착한 샘플은 무시
        lastAt = sample.timestampMillis
        if (prev == null) return
        val dt = (sample.timestampMillis - prev).coerceIn(0L, maxGapMillis)

        if (sample.gear == Gear.PARK) {
            parkedMillis += dt
            if (parkedMillis >= restResetMillis) {
                continuousMillis = 0L
                nightMillis = 0L
            }
        } else {
            parkedMillis = 0L
            continuousMillis += dt
            if (hourOf(sample.timestampMillis) in 0..4) nightMillis += dt
        }
    }

    /** 시연용: 주행 시간을 앞당깁니다(2시간 연속 주행을 기다리지 않고 교대 안내를 보여 주기 위함). */
    fun addDrivingTime(millis: Long, night: Boolean = false) {
        continuousMillis += millis
        if (night) nightMillis += millis
    }

    /** 운전자가 바뀌면 처음부터 다시 셉니다. */
    fun reset() {
        continuousMillis = 0L
        nightMillis = 0L
        parkedMillis = 0L
    }
}
