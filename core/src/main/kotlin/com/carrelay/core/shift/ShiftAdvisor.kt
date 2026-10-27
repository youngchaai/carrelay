package com.carrelay.core.shift

import com.carrelay.core.model.ConditionLevel
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.text.KoreanText

data class ShiftAdvice(
    val reason: Reason,
    val message: String,
    /** 교대 후보. 컨디션 정보가 없으면 null. */
    val reliefDriverId: String?,
) {
    enum class Reason { CONTINUOUS_DRIVING, NIGHT_DRIVING }
}

/**
 * 휴식·교대 권유(참가신청서 '교대 추천 로직(초안)').
 *
 * - 연속 주행 2시간 → 권유
 * - 자정~새벽 5시 연속 주행 1시간 → 권유
 * - 현재 운전자 수면 5시간 미만(워치 연동 시) → 간격 1시간 30분
 * - 케어 모드 운전자 → 간격 1시간
 * - '나중에'를 누르면 30분 뒤 다시 권유
 *
 * '가장 안전한 운전자' 같은 판정 표현은 쓰지 않고 '컨디션 참고' 수준으로 안내합니다.
 */
class ShiftAdvisor(
    private val baseIntervalMillis: Long = 120 * MINUTE,
    private val lowSleepIntervalMillis: Long = 90 * MINUTE,
    private val careModeIntervalMillis: Long = 60 * MINUTE,
    private val nightIntervalMillis: Long = 60 * MINUTE,
    private val repeatAfterMillis: Long = 30 * MINUTE,
    private val lowSleepHours: Double = 5.0,
) {
    private var lastAdvisedAt: Long? = null

    fun intervalFor(driver: Driver, condition: DriverCondition?): Long {
        var interval = baseIntervalMillis
        val sleep = condition?.sleepHours
        if (sleep != null && sleep < lowSleepHours) interval = minOf(interval, lowSleepIntervalMillis)
        if (driver.careModeEnabled) interval = minOf(interval, careModeIntervalMillis)
        return interval
    }

    fun evaluate(
        continuousMillis: Long,
        nightMillis: Long,
        current: Driver,
        currentCondition: DriverCondition?,
        others: List<Pair<Driver, DriverCondition?>>,
    ): ShiftAdvice? {
        val last = lastAdvisedAt
        if (last != null && continuousMillis < last) lastAdvisedAt = null // 휴식으로 시계가 초기화됨

        val isNight = nightMillis >= nightIntervalMillis
        val isLong = continuousMillis >= intervalFor(current, currentCondition)
        if (!isNight && !isLong) return null

        val advisedAt = lastAdvisedAt
        if (advisedAt != null && continuousMillis - advisedAt < repeatAfterMillis) return null
        lastAdvisedAt = continuousMillis

        val relief = pickRelief(current, others)
        val reliefText = relief?.let { "${it.name}님과 교대하거나 " } ?: ""
        val message = if (isNight) {
            "늦은 시간에 ${KoreanText.duration(nightMillis)}째 운전 중이에요. 가까운 휴게소에서 ${reliefText}쉬어 가실래요?"
        } else {
            "${KoreanText.duration(continuousMillis)} 연속 운전 중이에요. 가까운 휴게소에서 ${reliefText}쉬어 가실래요?"
        }
        return ShiftAdvice(
            reason = if (isNight) ShiftAdvice.Reason.NIGHT_DRIVING else ShiftAdvice.Reason.CONTINUOUS_DRIVING,
            message = message,
            reliefDriverId = relief?.id,
        )
    }

    /** 운전자가 바뀌거나 권유를 받아들였을 때 호출합니다. */
    fun reset() {
        lastAdvisedAt = null
    }

    /** 교대 후보: 게스트·현재 운전자를 뺀 파티원 중 컨디션 정보가 있고 '휴식 필요'가 아닌 사람. */
    private fun pickRelief(current: Driver, others: List<Pair<Driver, DriverCondition?>>): Driver? =
        others.asSequence()
            .filter { (d, c) -> !d.isGuest && d.id != current.id && c != null }
            .filter { (_, c) -> c!!.level != ConditionLevel.REST_NEEDED }
            .sortedWith(
                compareByDescending<Pair<Driver, DriverCondition?>> { it.second!!.level.rank }
                    .thenByDescending { it.second!!.sleepHours ?: 0.0 },
            )
            .firstOrNull()
            ?.first

    companion object {
        const val MINUTE = 60_000L
    }
}
