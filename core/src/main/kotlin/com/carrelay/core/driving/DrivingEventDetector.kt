package com.carrelay.core.driving

import com.carrelay.core.model.DrivingSample

enum class DrivingEvent(val label: String) {
    HARSH_DECEL("급정거"),
    HARSH_ACCEL("급가속"),
}

/**
 * 모든 운전자에게 적용하는 급정거·급가속 감지 (운전자별 안전 기록, 운행 후 이상 확인 질문에 사용).
 * 차량에서 온 연속 샘플의 속도 변화로 계산합니다. 사고 여부를 판정하지는 않습니다.
 *
 * - 샘플 간격이 고르지 않으면(몇 ms 차이로 붙어 들어오는 경우) 두 점만으로는 가속도가 크게 튑니다.
 *   그래서 지금 샘플과 [windowMillis] 이상 떨어진 가장 가까운 이전 샘플을 비교합니다.
 * - 한 번 길게 밟은 것은 한 번으로 셉니다. 기준을 넘어 한 번 센 뒤에는 가속도가 기준의 절반 아래로
 *   내려가야 다시 셀 준비가 됩니다(Pleos 에뮬레이터에서 가속 페달을 6초 끝까지 밟았을 때 두 번 세던 문제).
 */
class DrivingEventDetector(
    private val harshDecelMps2: Double = 3.5,
    private val harshAccelMps2: Double = 3.0,
    private val debounceMillis: Long = 3_000L,
    private val maxGapMillis: Long = 3_000L,
    private val windowMillis: Long = 500L,
) {
    private val recent = ArrayDeque<DrivingSample>()
    private var lastDecelAt: Long? = null
    private var lastAccelAt: Long? = null
    private var decelArmed = true
    private var accelArmed = true

    fun onSample(s: DrivingSample): DrivingEvent? {
        val v1 = s.speedKmh ?: return null
        val now = s.timestampMillis
        val last = recent.lastOrNull()
        // 시간이 거꾸로 가거나 오래 끊겼으면 새로 시작합니다.
        if (last != null && (now < last.timestampMillis || now - last.timestampMillis > maxGapMillis)) {
            recent.clear()
            decelArmed = true
            accelArmed = true
        }
        recent.addLast(s)
        while (recent.size > 1 && now - recent.first().timestampMillis > maxGapMillis) recent.removeFirst()

        val ref = recent.lastOrNull { now - it.timestampMillis >= windowMillis } ?: return null
        val v0 = ref.speedKmh ?: return null
        val accel = (v1 - v0) / 3.6 / ((now - ref.timestampMillis) / 1000.0)

        // 기준의 절반 아래로 내려가면 다시 셀 준비
        if (-accel < harshDecelMps2 / 2) decelArmed = true
        if (accel < harshAccelMps2 / 2) accelArmed = true

        if (-accel >= harshDecelMps2 && decelArmed && lastDecelAt.let { it == null || now - it >= debounceMillis }) {
            lastDecelAt = now
            decelArmed = false
            return DrivingEvent.HARSH_DECEL
        }
        if (accel >= harshAccelMps2 && accelArmed && lastAccelAt.let { it == null || now - it >= debounceMillis }) {
            lastAccelAt = now
            accelArmed = false
            return DrivingEvent.HARSH_ACCEL
        }
        return null
    }

    fun reset() {
        recent.clear()
        lastDecelAt = null
        lastAccelAt = null
        decelArmed = true
        accelArmed = true
    }
}
