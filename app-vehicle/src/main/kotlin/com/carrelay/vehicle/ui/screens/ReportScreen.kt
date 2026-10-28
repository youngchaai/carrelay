package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.CarZone
import com.carrelay.core.model.IncidentSource
import com.carrelay.core.model.IncidentType
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.CarTopView
import com.carrelay.vehicle.ui.components.CarTopViewAspect
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.SectionTitle
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.lastEndedSession
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

/**
 * 사고·파손 신고. 키보드 없이: 무슨 일인지 고르고 → 차 그림에서 위치를 누르고 → 지금 상태를 고릅니다.
 * 처리 완료 전까지 모든 운전자의 브리핑에 계속 나옵니다.
 */
@Composable
fun ReportScreen(
    ui: UiState,
    onSubmit: (IncidentType, Set<CarZone>, alreadyHandled: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var type by remember { mutableStateOf<IncidentType?>(null) }
    var zones by remember { mutableStateOf(emptySet<CarZone>()) }
    var handled by remember { mutableStateOf(false) }
    val danger = CarRelayTheme.status.danger
    val atPickup = ui.reportSource == IncidentSource.AT_PICKUP
    val previous = ui.lastEndedSession?.takeIf { atPickup && it.driverId != ui.currentDriver?.id }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (atPickup) "넘겨받은 차에서 이상을 발견했어요" else "사고·파손 신고",
            subtitle = "처리 완료 전까지 모든 운전자에게 보여요. 키보드 없이 눌러서 남겨요",
            leading = { IconCircle(CarIcons.Warning, danger, size = 80.dp, iconSize = 42.dp) },
        )
        if (previous != null) {
            SurfaceCard(Modifier.fillMaxWidth(), container = CarRelayTheme.status.info.container, border = null, contentPadding = 20.dp) {
                Text(
                    "직전 운전: ${ui.nameOf(previous.driverId)}" +
                        (previous.endedAtMillis?.let { " · ${formatDateTime(it)} 주차" } ?: "") +
                        " — 신고하면 함께 기록돼 확인할 수 있어요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CarRelayTheme.status.info.content,
                )
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            // 왼쪽: 차 그림에서 위치 고르기
            SurfaceCard(Modifier.weight(0.42f).fillMaxHeight()) {
                SectionTitle("어디인가요?") {
                    StatusBadge(
                        if (zones.isEmpty()) "선택 안 함" else CarZone.entries.filter { it in zones }.joinToString("·") { it.label },
                        if (zones.isEmpty()) neutralTone() else danger,
                    )
                }
                CarTopView(
                    modifier = Modifier.weight(1f).aspectRatio(CarTopViewAspect).align(Alignment.CenterHorizontally),
                    selected = zones,
                    selectedTone = danger,
                    onZoneClick = { z -> zones = if (z in zones) zones - z else zones + z },
                )
                Text(
                    "차 그림을 눌러 앞·뒤·왼쪽·오른쪽·실내를 고르세요 (여러 곳 가능)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 오른쪽: 무슨 일인지 + 지금 상태
            Column(Modifier.weight(0.58f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                SectionTitle("무슨 일인가요?")
                IncidentType.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                        pair.forEach { t ->
                            ChoiceCard(t.label, selected = t == type, modifier = Modifier.weight(1f), selectedColor = danger.content, selectedContainer = danger.container) { type = t }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                SectionTitle("지금 상태")
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                    ChoiceCard("아직 처리 전", !handled, Modifier.weight(1f)) { handled = false }
                    ChoiceCard("보험·정비 접수함", handled, Modifier.weight(1f)) { handled = true }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "누가 원인인지 정하지 않아요. 언제·어디서 발견됐는지만 남겨 가족이 함께 확인해요. 기록 화면의 '사고·파손 이력'에서 다시 볼 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ActionBar {
            SecondaryButton("취소", onCancel, Modifier.weight(1f), icon = CarIcons.Close)
            PrimaryButton(
                if (type == null) "무슨 일인지 골라 주세요" else "신고하기",
                { type?.let { onSubmit(it, zones, handled) } },
                Modifier.weight(2f),
                icon = CarIcons.Bell,
                enabled = type != null,
                container = danger.content,
                contentColor = danger.container,
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    selectedColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    selectedContainer: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit,
) {
    SurfaceCard(
        modifier = modifier.height(104.dp),
        container = if (selected) selectedContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) selectedColor else MaterialTheme.colorScheme.outlineVariant),
        contentPadding = 24.dp,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (selected) IconCircle(CarIcons.Check, com.carrelay.vehicle.ui.theme.StatusTone(selectedContainer, selectedColor), size = 40.dp, iconSize = 24.dp)
            Text(text, style = MaterialTheme.typography.headlineSmall, color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface)
        }
    }
}
