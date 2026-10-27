package com.carrelay.core.shift

import com.carrelay.core.model.ConditionLevel
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition
import com.carrelay.core.text.KoreanText

data class DriverRecommendation(
    val driverId: String,
    val message: String,
)

/**
 * 출발 전 '오늘의 운전자 추천'(참가신청서 F4). 워치 데이터는 동기화 지연이 있으므로 출발 전에만 씁니다.
 * 컨디션 정보가 있는 파티원이 두 명 이상이고, 1등이 분명할 때만 추천합니다.
 */
object DriverRecommender {

    fun recommend(drivers: List<Driver>, conditions: Map<String, DriverCondition>): DriverRecommendation? {
        val withData = drivers
            .filter { !it.isGuest }
            .mapNotNull { d -> conditions[d.id]?.let { d to it } }
        if (withData.size < 2) return null

        val sorted = withData.sortedWith(
            compareByDescending<Pair<Driver, DriverCondition>> { it.second.level.rank }
                .thenByDescending { it.second.sleepHours ?: 0.0 },
        )
        val (best, bestCondition) = sorted.first()
        val (_, secondCondition) = sorted[1]
        if (bestCondition.level == ConditionLevel.REST_NEEDED) return null

        val clearlyBetter = bestCondition.level.rank > secondCondition.level.rank ||
            (bestCondition.sleepHours ?: 0.0) - (secondCondition.sleepHours ?: 0.0) >= 1.0
        if (!clearlyBetter) return null

        val detail = sorted.joinToString(" / ") { (d, c) ->
            val sleep = c.sleepHours?.let { "수면 ${KoreanText.hours(it)}" } ?: c.level.label
            "${d.name} $sleep"
        }
        return DriverRecommendation(
            driverId = best.id,
            message = "오늘 장거리는 ${best.name}님이 운전하는 걸 추천해요 ($detail)",
        )
    }
}
