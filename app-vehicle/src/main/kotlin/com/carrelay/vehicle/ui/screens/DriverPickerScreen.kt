package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.core.profile.LicenseInfo
import com.carrelay.core.profile.LicenseState
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.lastEndedSession
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

/** F2 운전자 선택: 프로필을 터치 한 번으로 고릅니다. 기어 P에서만 선택할 수 있습니다. */
@Composable
fun DriverPickerScreen(ui: UiState, onSelect: (String) -> Unit, onKeepSame: () -> Unit) {
    val drivers = ui.drivers + Driver.GUEST
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (ui.askingDriverChange) "운전자 바뀌셨나요?" else "누가 운전하시나요?",
            subtitle = when {
                !ui.canSelectDriver -> "기어 P에서 선택할 수 있어요"
                ui.askingDriverChange -> "운전석 문이 열렸다 닫혔어요. 짐만 꺼냈다면 그대로 진행하세요"
                else -> "고르면 지난 운전 이후 달라진 점과 남은 메모를 알려 드려요"
            },
        )

        val current = ui.currentDriver
        if (ui.askingDriverChange && current != null) {
            PrimaryButton("그대로 ${current.name}님이 운전해요", onKeepSame, Modifier.fillMaxWidth(), icon = CarIcons.Check)
        } else {
            ui.recommendation?.let { rec ->
                val level = ui.conditions[rec.driverId]?.level
                SurfaceCard(container = MaterialTheme.colorScheme.primaryContainer, border = null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IconCircle(CarIcons.Route, primaryTone(), size = 72.dp, iconSize = 38.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("오늘 장거리 추천", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "${ui.nameOf(rec.driverId)}님이 운전하는 걸 추천해요",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        if (level != null) StatusBadge("컨디션 ${level.label}", conditionTone(level), icon = CarIcons.Person)
                    }
                    Text(
                        "워치 수면 요약을 바탕으로 한 참고 정보예요. 가족에게는 3단계만 보여요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
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

        // 차 이름 · 마지막 주차 · 처리 안 된 사고·파손
        val last = ui.lastEndedSession
        val openCount = ui.incidents.count { it.isOpen }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(CarIcons.Car, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Text(
                buildString {
                    append(ui.vehicleName)
                    if (last != null) {
                        append(" · 마지막 주차 ${ui.nameOf(last.driverId)}")
                        last.endedAtMillis?.let { append(" ${formatDateTime(it)}") }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (openCount > 0) {
                StatusBadge("처리 완료 전 사고·파손 ${openCount}건 · 고른 뒤 브리핑에서 확인", CarRelayTheme.status.danger, icon = CarIcons.Warning)
            }
        }
    }
}

/** 면허 갱신 임박·지남, 초보 운전 배지 (면허 번호는 저장하지 않습니다) */
@Composable
private fun LicenseBadges(info: LicenseInfo?) {
    if (info == null) return
    val s = CarRelayTheme.status
    when (info.state) {
        LicenseState.RENEW_SOON -> StatusBadge("면허 ${info.renewalLabel.orEmpty()}", s.warning, icon = CarIcons.Clock)
        LicenseState.EXPIRED -> StatusBadge("면허 갱신 기한 지남", s.danger, icon = CarIcons.Warning)
        else -> Unit
    }
    if (info.isNovice) StatusBadge("초보 운전", s.info, icon = CarIcons.Info)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverCard(driver: Driver, ui: UiState, onSelect: (String) -> Unit) {
    val condition = ui.conditions[driver.id]
    val isLast = ui.lastDriverId == driver.id
    val recommended = ui.recommendation?.driverId == driver.id
    val status = CarRelayTheme.status
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().height(268.dp).alpha(if (ui.canSelectDriver) 1f else 0.45f),
        border = if (recommended) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        onClick = if (ui.canSelectDriver) ({ onSelect(driver.id) }) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Avatar(if (driver.isGuest) "?" else driver.name, ui.avatarTone(driver.id), size = 88.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(driver.name, style = MaterialTheme.typography.headlineLarge)
                when {
                    condition != null -> StatusBadge("컨디션 ${condition.level.label}", conditionTone(condition.level))
                    driver.isGuest -> Text("파티원이 아닐 때", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Text("컨디션 정보 없음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recommended) StatusBadge("추천", primaryTone(), icon = CarIcons.Route)
            if (isLast) StatusBadge("지난 운전자", status.info, icon = CarIcons.Clock)
            if (driver.careModeEnabled) StatusBadge("케어", status.memo, icon = CarIcons.Shield)
            LicenseBadges(ui.licenses[driver.id])
        }
    }
}
