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
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.isCompany
import com.carrelay.vehicle.ui.lastEndedSession
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * 사고·파손 신고. 두 단계만: ① 무슨 일인지 고르고 ② 차 그림에서 어디인지 누르기(모르면 건너뛰기).
 * 처리 상태(수리·보험 접수 등)는 나중에 알림을 눌러 바꿉니다.
 */
@Composable
fun ReportScreen(
    ui: UiState,
    onSubmit: (IncidentType, Set<CarZone>, alreadyHandled: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var type by remember { mutableStateOf<IncidentType?>(null) }
    var zones by remember { mutableStateOf(emptySet<CarZone>()) }
    val danger = CarRelayTheme.status.danger
    val atPickup = ui.reportSource == IncidentSource.AT_PICKUP
    val previous = ui.lastEndedSession?.takeIf { atPickup && it.driverId != ui.currentDriver?.id }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (atPickup) "어디가 이상한가요?" else "무슨 일이 있었나요?",
            subtitle = buildString {
                append(if (ui.isCompany) "관리자에게 보고되고, 고칠 때까지 모든 직원에게 보여요" else "고칠 때까지 가족 모두에게 알려 드려요")
                if (previous != null) {
                    append(" · 직전 운전 ${ui.nameOf(previous.driverId)}")
                    previous.endedAtMillis?.let { append(" (${formatDateTime(it)} 주차)") }
                }
            },
            leading = { IconCircle(CarIcons.Warning, danger, size = 80.dp, iconSize = 42.dp) },
        )

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            // ① 무슨 일인가요?
            Column(Modifier.weight(0.56f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                SectionTitle("① 무슨 일인가요?")
                IncidentType.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                        pair.forEach { t ->
                            ChoiceTile(t.label, selected = t == type, tone = danger, modifier = Modifier.weight(1f)) { type = t }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (ui.isCompany) "누구 탓인지 따지지 않아요. 언제 발견했는지와 직전 운전자만 남겨 함께 확인해요."
                    else "누구 탓인지 따지지 않아요. 언제 발견했는지만 남겨 가족이 함께 확인해요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // ② 어디인가요?
            SurfaceCard(Modifier.weight(0.44f).fillMaxHeight()) {
                SectionTitle("② 어디인가요?")
                Text(
                    if (zones.isEmpty()) "차 그림을 눌러 주세요. 모르면 건너뛰어도 돼요"
                    else "고른 곳: " + CarZone.entries.filter { it in zones }.joinToString("·") { it.label },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (zones.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else danger.content,
                )
                CarTopView(
                    modifier = Modifier.weight(1f).aspectRatio(CarTopViewAspect).align(Alignment.CenterHorizontally),
                    selected = zones,
                    selectedTone = danger,
                    onZoneClick = { z -> zones = if (z in zones) zones - z else zones + z },
                )
            }
        }

        ActionBar {
            SecondaryButton("취소", onCancel, Modifier.weight(1f), icon = CarIcons.Close)
            PrimaryButton(
                if (type == null) "무슨 일인지 먼저 골라 주세요" else "남기기",
                { type?.let { onSubmit(it, zones, false) } },
                Modifier.weight(1.6f),
                icon = CarIcons.Check,
                enabled = type != null,
                container = danger.content,
                contentColor = danger.container,
            )
        }
    }
}

@Composable
private fun ChoiceTile(text: String, selected: Boolean, tone: StatusTone, modifier: Modifier, onClick: () -> Unit) {
    SurfaceCard(
        modifier = modifier.height(112.dp),
        container = if (selected) tone.container else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) tone.content else MaterialTheme.colorScheme.outlineVariant),
        contentPadding = 24.dp,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (selected) IconCircle(CarIcons.Check, StatusTone(tone.container, tone.content), size = 44.dp, iconSize = 26.dp)
            Text(text, style = MaterialTheme.typography.headlineSmall, color = if (selected) tone.content else MaterialTheme.colorScheme.onSurface)
        }
    }
}
