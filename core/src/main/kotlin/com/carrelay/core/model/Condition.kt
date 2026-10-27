package com.carrelay.core.model

import kotlinx.serialization.Serializable

/**
 * 출발 전 컨디션 참고 정보(3단계). 의료적 판정이 아닙니다.
 * 차량·다른 파티원에게는 이 3단계만 공유합니다.
 */
@Serializable
enum class ConditionLevel(val label: String, val rank: Int) {
    GOOD("좋음", 2),
    NORMAL("보통", 1),
    REST_NEEDED("휴식 필요", 0),
}

/**
 * 폰 앱(Health Connect)이 서버에 올린 하루 요약. 시연에서는 샘플 데이터로 대체합니다.
 *
 * @property restingHeartRateDelta 본인 평소 안정시 심박 대비 차이(bpm). 양수면 평소보다 높음.
 */
@Serializable
data class DriverCondition(
    val driverId: String,
    val sleepHours: Double? = null,
    val restingHeartRateDelta: Int? = null,
    val updatedAtMillis: Long = 0L,
) {
    val level: ConditionLevel
        get() {
            val bySleep = when {
                sleepHours == null -> ConditionLevel.NORMAL
                sleepHours >= 7.0 -> ConditionLevel.GOOD
                sleepHours >= 5.0 -> ConditionLevel.NORMAL
                else -> ConditionLevel.REST_NEEDED
            }
            val hrHigh = (restingHeartRateDelta ?: 0) >= 10
            return if (hrHigh) bySleep.downgrade() else bySleep
        }

    private fun ConditionLevel.downgrade(): ConditionLevel = when (this) {
        ConditionLevel.GOOD -> ConditionLevel.NORMAL
        ConditionLevel.NORMAL, ConditionLevel.REST_NEEDED -> ConditionLevel.REST_NEEDED
    }
}
