package com.carrelay.core.shift

import com.carrelay.core.model.ConditionLevel
import com.carrelay.core.model.Driver
import com.carrelay.core.model.DriverCondition

data class DriverRecommendation(
    val driverId: String,
    val message: String,
)

/**
 * 출발 전 '오늘의 운전자 추천'(참가신청서 F4). 워치 데이터는 동기화 지연이 있으므로 출발 전에만 씁니다.
 * 컨디션 정보가 있는 파티원이 두 명 이상이고, 1등의 단계가 2등보다 높을 때만 추천합니다.
 * 문구에는 3단계(좋음/보통/휴식 필요)만 넣습니다(신청서: 차량·다른 파티원에게는 3단계만 공유).
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

        // 화면에 보이는 근거(3단계)로 설명할 수 있을 때만 추천합니다.
        if (bestCondition.level.rank <= secondCondition.level.rank) return null

        // 차량 화면은 모두가 보므로 3단계만 표시합니다. 수면 시간 같은 원래 수치는 본인 폰에서만 보여 줍니다.
        val detail = sorted.joinToString(" / ") { (d, c) -> "${d.name} ${c.level.label}" }
        return DriverRecommendation(
            driverId = best.id,
            message = "오늘 장거리는 ${best.name}님이 운전하는 걸 추천해요 ($detail)",
        )
    }
}
