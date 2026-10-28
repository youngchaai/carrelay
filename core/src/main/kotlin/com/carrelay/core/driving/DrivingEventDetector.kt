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
 * 샘플 간격이 고르지 않으면(몇 ms 차이로 붙어 들어오는 경우) 두 점만으로는 가속도가 크게 튑니다.
 * 그래서 지금 샘플과 [windowMillis] 이상 떨어진 가장 가까운 이전 샘플을 비교합니다.
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

    fun onSample(s: DrivingSample): DrivingEvent? {
        val v1 = s.speedKmh ?: return null
        val now = s.timestampMillis
        val last = recent.lastOrNull()
        // 시간이 거꾸로 가거나 오래 끊겼으면 새로 시작합니다.
        if (last != null && (now < last.timestampMillis || now - last.timestampMillis > maxGapMillis)) recent.clear()
        recent.addLast(s)
        while (recent.size > 1 && now - recent.first().timestampMillis > maxGapMillis) recent.removeFirst()

        val ref = recent.lastOrNull { now - it.timestampMillis >= windowMillis } ?: return null
        val v0 = ref.speedKmh ?: return null
        val accel = (v1 - v0) / 3.6 / ((now - ref.timestampMillis) / 1000.0)
        if (-accel >= harshDecelMps2 && lastDecelAt.let { it == null || now - it >= debounceMillis }) {
            lastDecelAt = now
            return DrivingEvent.HARSH_DECEL
        }
        if (accel >= harshAccelMps2 && lastAccelAt.let { it == null || now - it >= debounceMillis }) {
            lastAccelAt = now
            return DrivingEvent.HARSH_ACCEL
        }
        return null
    }

    fun reset() {
        recent.clear()
        lastDecelAt = null
        lastAccelAt = null
    }
}
