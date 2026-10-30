package com.carrelay.vehicle.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.MemoPreset
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.icon
import com.carrelay.vehicle.ui.shortLabel
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * 다음 운전자에게 남길 말. 키보드 없이 큰 버튼 하나만 누르면 저장됩니다.
 * 사고·파손 신고도 같은 자리에서(맨 끝 빨간 칸). 운행 중 급정거가 있었다면 먼저 "차에 이상이 있나요?"를 묻습니다.
 */
@Composable
fun MemoScreen(
    ui: UiState,
    onPreset: (MemoPreset) -> Unit,
    onVoice: () -> Unit,
    onDone: () -> Unit,
    onHandOver: () -> Unit,
    onReport: () -> Unit,
    onAfterDriveOk: () -> Unit,
) {
    val me = ui.currentDriver?.id
    val alreadyLeft = ui.memos.filter { !it.isResolved && it.authorId == me }.mapNotNull { it.preset }.toSet()
    val askCheck = ui.parkedPrompt && ui.parkedHarshDecel > 0 && !ui.afterDriveChecked
    val warning = CarRelayTheme.status.warning
    val danger = CarRelayTheme.status.danger
    // 이 차에 맞는 것만: 전기차면 '충전 필요', 아니면 '주유 필요'
    val presets = when (ui.snapshot?.energyLabel) {
        "배터리" -> MemoPreset.entries - MemoPreset.FUEL_LOW
        null -> MemoPreset.entries
        else -> MemoPreset.entries - MemoPreset.CHARGE_NEEDED
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (ui.parkedPrompt) "주차하셨네요. 남길 말이 있나요?" else "다음 운전자에게 남길 말",
            // 예시 문장은 중간에서 끊기지 않게 붙는 띄어쓰기( )로 묶습니다.
            subtitle = "누르면 바로 저장돼요. 말로 해도 돼요: " + "\"다음 사람한테 주유하라고 전해 줘\"".replace(' ', ' '),
        ) {
            // 말로 남기기: 키보드 없이 하고 싶은 말을 그대로 (Pleos 음성 인식)
            PrimaryButton("말로 남기기", onVoice, icon = CarIcons.Mic, height = 104.dp)
        }
        AnimatedVisibility(askCheck, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            SurfaceCard(Modifier.fillMaxWidth(), container = warning.container, border = null, contentPadding = 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconCircle(CarIcons.Warning, warning, size = 72.dp, iconSize = 38.dp)
                    Text(
                        "운전 중 급정거가 ${ui.parkedHarshDecel}번 있었어요.\n차에 이상이 있나요?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = warning.content,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton("없어요", onAfterDriveOk, height = 96.dp, icon = CarIcons.Check)
                    PrimaryButton(
                        "있어요", onReport, height = 96.dp, icon = CarIcons.Warning,
                        container = danger.content, contentColor = danger.container,
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            modifier = Modifier.weight(1f),
        ) {

            items(presets) { preset ->
                val left = preset in alreadyLeft
                Tile(
                    text = preset.shortLabel,
                    icon = preset.icon,
                    tone = CarRelayTheme.status.memo,
                    done = left,
                    onClick = if (left) null else ({ onPreset(preset) }),
                )
            }
            item { Tile(text = "사고·파손 신고", icon = CarIcons.Warning, tone = danger, done = false, emphasized = true, onClick = onReport) }
        }
        ActionBar {
            SecondaryButton("닫기", onDone, Modifier.weight(1f), icon = CarIcons.Close)
            PrimaryButton("운전 끝내기", onHandOver, Modifier.weight(1.4f), icon = CarIcons.Handover, enabled = ui.currentDriver != null)
        }
    }
}

@Composable
private fun Tile(
    text: String,
    icon: ImageVector,
    tone: StatusTone,
    done: Boolean,
    onClick: (() -> Unit)?,
    emphasized: Boolean = false,
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        container = if (emphasized) tone.container else MaterialTheme.colorScheme.surface,
        border = if (emphasized) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = 24.dp,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconCircle(icon, tone, size = 68.dp, iconSize = 36.dp)
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall,
                color = if (emphasized) tone.content else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (done) StatusBadge("남겼어요", CarRelayTheme.status.ok, icon = CarIcons.Check)
        }
    }
}
