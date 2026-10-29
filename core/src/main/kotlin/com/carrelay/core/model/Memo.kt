package com.carrelay.core.model

import kotlinx.serialization.Serializable

/** 차량에서 키보드 없이 누를 수 있는 메모 프리셋. */
@Serializable
enum class MemoPreset(val text: String) {
    FUEL_LOW("연료 부족, 주유 필요해요"),
    CHARGE_NEEDED("충전이 필요해요"),
    TIRE_CHECK("타이어 바람 확인이 필요해요"),
    WIPER_NOISE("와이퍼에서 소리가 나요"),
    STRANGE_NOISE("주행 중 이상한 소리가 나요"),
    WARNING_LIGHT("경고등이 켜졌어요"),
    NEEDS_CLEANING("실내 청소가 필요해요"),
    LONG_TRIP_PLANNED("다음에 장거리 사용 예정이에요"),
}

/** 메모를 해결 처리한 기록. 해결은 '읽음'과 별개입니다. */
@Serializable
data class Resolution(
    val byDriverId: String,
    val note: String,
    val atMillis: Long,
)

/**
 * 운전자가 다음 운전자에게 남기는 메모.
 *
 * 규칙
 * - 읽음([readBy])과 해결([resolution])을 분리합니다. 읽어도 해결 전까지 모두에게 계속 보입니다.
 * - 메모를 해결해도 차량의 현재 경고등 상태는 바뀌지 않습니다(브리핑에서 따로 표시).
 */
@Serializable
data class Memo(
    val id: String,
    val authorId: String,
    val text: String,
    val createdAtMillis: Long,
    val preset: MemoPreset? = null,
    val readBy: Map<String, Long> = emptyMap(),
    val resolution: Resolution? = null,
    /** 특정 가족에게 남긴 말("엄마한테 …")이면 그 사람. 그래도 차 문제이므로 모두에게 보입니다. */
    val toDriverId: String? = null,
    /** 말로 남긴 메모인지 (화면에 마이크 표시) */
    val spoken: Boolean = false,
) {
    val isResolved: Boolean get() = resolution != null
    fun isReadBy(driverId: String): Boolean = driverId == authorId || readBy.containsKey(driverId)
}
