package com.carrelay.vehicle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import com.carrelay.core.model.Driver
import com.carrelay.core.model.MemoPreset
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * AnimatedContent 안에서 씁니다. 지금 보이는 쪽이면 최신 값을, 사라지는 쪽이면 마지막으로 보이던 값을 돌려줘
 * 나가는 화면이 바뀐 상태(예: 브리핑이 비워짐)로 잠깐 다시 그려지지 않게 합니다.
 */
@Composable
fun <T> latestWhile(active: Boolean, value: T): T {
    val holder = remember { arrayOf<Any?>(value) }
    if (active) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return holder[0] as T
}

/**
 * 화면이 바뀌는 짧은 동안 눌림을 막습니다.
 * 빠르게 두 번 누르면 두 번째 눌림이 새로 나타난 화면의 같은 자리 버튼(예: '이상 없어요, 확인했어요')을 누르는 일을 막습니다.
 */
fun Modifier.blockTouches(active: Boolean): Modifier =
    if (!active) {
        this
    } else {
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    }

/** 운전자마다 고정된 아바타 색 (파티 순서 기준). 게스트는 중립색. */
@Composable
fun UiState.avatarTone(driverId: String): StatusTone {
    if (driverId == Driver.GUEST_ID) return neutralTone()
    val index = drivers.indexOfFirst { it.id == driverId }.coerceAtLeast(0)
    val palette = CarRelayTheme.status.avatars
    return palette[index % palette.size]
}

/** 차량에서 누르는 메모 버튼의 짧은 제목과 아이콘 (메모 본문은 MemoPreset.text) */
val MemoPreset.shortLabel: String
    get() = when (this) {
        MemoPreset.FUEL_LOW -> "주유 필요"
        MemoPreset.CHARGE_NEEDED -> "충전 필요"
        MemoPreset.TIRE_CHECK -> "타이어 확인"
        MemoPreset.WIPER_NOISE -> "와이퍼 소음"
        MemoPreset.STRANGE_NOISE -> "이상한 소리"
        MemoPreset.WARNING_LIGHT -> "경고등 켜짐"
        MemoPreset.NEEDS_CLEANING -> "실내 청소"
        MemoPreset.LONG_TRIP_PLANNED -> "장거리 예정"
    }

val MemoPreset.icon: ImageVector
    get() = when (this) {
        MemoPreset.FUEL_LOW -> CarIcons.Fuel
        MemoPreset.CHARGE_NEEDED -> CarIcons.Bolt
        MemoPreset.TIRE_CHECK -> CarIcons.Tire
        MemoPreset.WIPER_NOISE -> CarIcons.Wiper
        MemoPreset.STRANGE_NOISE -> CarIcons.Sound
        MemoPreset.WARNING_LIGHT -> CarIcons.Warning
        MemoPreset.NEEDS_CLEANING -> CarIcons.Sparkle
        MemoPreset.LONG_TRIP_PLANNED -> CarIcons.Route
    }
