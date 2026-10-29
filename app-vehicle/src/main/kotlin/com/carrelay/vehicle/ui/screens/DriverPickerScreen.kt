package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.core.profile.LicenseState
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.conditionTone
import com.carrelay.vehicle.ui.components.primaryTone
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * F2 운전자 선택: 내 이름을 한 번 누르면 끝. 기어 P에서만 고를 수 있습니다.
 * 카드에는 이름과 꼭 알아야 할 것 두 가지까지만 적습니다.
 */
@Composable
fun DriverPickerScreen(ui: UiState, onSelect: (String) -> Unit, onKeepSame: () -> Unit) {
    val drivers = ui.drivers + Driver.GUEST
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (ui.askingDriverChange) "운전하는 분이 바뀌었나요?" else "누가 운전하시나요?",
            subtitle = when {
                !ui.canSelectDriver -> "차를 세우고(P) 골라 주세요"
                ui.askingDriverChange -> "문이 열렸다 닫혔어요. 같은 분이면 위 버튼을 눌러 주세요"
                else -> "내 이름을 눌러 주세요"
            },
        )

        val current = ui.currentDriver
        if (ui.askingDriverChange && current != null) {
            PrimaryButton("그대로 ${current.name}님이 운전해요", onKeepSame, Modifier.fillMaxWidth(), icon = CarIcons.Check)
        } else {
            ui.recommendation?.let { rec ->
                val level = ui.conditions[rec.driverId]?.level
                SurfaceCard(container = MaterialTheme.colorScheme.primaryContainer, border = null, contentPadding = 24.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        IconCircle(CarIcons.Route, primaryTone(), size = 64.dp, iconSize = 34.dp)
                        Text(
                            "오늘 장거리는 ${ui.nameOf(rec.driverId)}님이 운전하면 좋아요",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        if (level != null) StatusBadge("컨디션 ${level.label}", conditionTone(level))
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
            modifier = Modifier.weight(1f),
        ) {
            items(drivers, key = { it.id }) { driver -> DriverCard(driver, ui, onSelect) }
        }

        // 아직 처리 안 된 사고·파손이 있으면 고르기 전에 알려 둡니다.
        val openCount = ui.incidents.count { it.isOpen }
        if (openCount > 0) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(CarIcons.Warning, contentDescription = null, tint = CarRelayTheme.status.danger.content, modifier = Modifier.size(32.dp))
                Text(
                    "아직 처리하지 않은 사고·파손이 ${openCount}건 있어요. 고르면 자세히 알려 드려요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CarRelayTheme.status.danger.content,
                )
            }
        }
    }
}

private data class CardNote(val text: String, val tone: StatusTone, val icon: ImageVector)

@Composable
private fun DriverCard(driver: Driver, ui: UiState, onSelect: (String) -> Unit) {
    val recommended = ui.recommendation?.driverId == driver.id
    val s = CarRelayTheme.status
    val license = ui.licenses[driver.id]
    // 중요한 것부터 두 개까지만
    val notes = buildList {
        if (recommended) add(CardNote("오늘 추천", primaryTone(), CarIcons.Route))
        when (license?.state) {
            LicenseState.EXPIRED -> add(CardNote("면허 갱신 기한 지남", s.danger, CarIcons.Warning))
            LicenseState.RENEW_SOON -> add(CardNote("면허 갱신 ${license?.daysLeft ?: 0}일 남음", s.warning, CarIcons.Clock))
            else -> Unit
        }
        if (license?.isNovice == true) add(CardNote("초보 운전", s.info, CarIcons.Info))
        if (ui.lastDriverId == driver.id) add(CardNote("지난번 운전", s.info, CarIcons.Clock))
    }.take(2)

    SurfaceCard(
        modifier = Modifier.fillMaxWidth().height(256.dp).alpha(if (ui.canSelectDriver) 1f else 0.45f),
        border = if (recommended) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = if (ui.canSelectDriver) ({ onSelect(driver.id) }) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Avatar(if (driver.isGuest) "?" else driver.name, ui.avatarTone(driver.id), size = 96.dp)
            Column {
                Text(driver.name, style = MaterialTheme.typography.headlineLarge)
                if (driver.isGuest) {
                    Text("가족이 아닌 분", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { StatusBadge(it.text, it.tone, icon = it.icon) }
        }
    }
}
