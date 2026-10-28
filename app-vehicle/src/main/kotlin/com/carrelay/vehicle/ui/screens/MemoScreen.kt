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

/**
 * 주차 후 다음 운전자에게 남길 메모. 키보드 없이 큰 버튼만 씁니다.
 * 운행 중 급정거가 있었다면 먼저 "차에 이상이 있나요?"를 묻습니다(사고 후 그냥 넘어가지 않게).
 */
@Composable
fun MemoScreen(
    ui: UiState,
    onPreset: (MemoPreset) -> Unit,
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

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (ui.parkedPrompt) "주차하셨네요. 남길 메모가 있나요?" else "다음 운전자에게 메모 남기기",
            subtitle = "누르면 바로 저장돼요. 해결될 때까지 모든 가족에게 보여요",
        )
        AnimatedVisibility(askCheck, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            SurfaceCard(Modifier.fillMaxWidth(), container = warning.container, border = null, contentPadding = 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconCircle(CarIcons.Warning, warning, size = 72.dp, iconSize = 38.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "운행 중 급정거가 ${ui.parkedHarshDecel}번 있었어요. 차에 이상이 있나요?",
                            style = MaterialTheme.typography.titleLarge,
                            color = warning.content,
                        )
                        Text(
                            "지금 남기면 다음 운전자가 헷갈리지 않아요. 긁힘처럼 작은 것도 괜찮아요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = warning.content,
                        )
                    }
                    SecondaryButton("이상 없어요", onAfterDriveOk, height = 88.dp, icon = CarIcons.Check)
                    PrimaryButton(
                        "사고·파손 신고", onReport, height = 88.dp, icon = CarIcons.Bell,
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
            items(MemoPreset.entries) { preset ->
                val left = preset in alreadyLeft
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentPadding = 24.dp,
                    onClick = if (left) null else ({ onPreset(preset) }),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        IconCircle(preset.icon, CarRelayTheme.status.memo, size = 68.dp, iconSize = 36.dp)
                        Text(preset.shortLabel, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        if (left) StatusBadge("남겼어요", CarRelayTheme.status.ok, icon = CarIcons.Check)
                    }
                }
            }
        }
        ActionBar {
            SecondaryButton("닫기", onDone, Modifier.weight(0.8f), icon = CarIcons.Close)
            SecondaryButton("사고·파손 신고", onReport, Modifier.weight(1f), icon = CarIcons.Warning)
            PrimaryButton("운전 끝내기 (넘겨주기)", onHandOver, Modifier.weight(1.2f), icon = CarIcons.Handover, enabled = ui.currentDriver != null)
        }
    }
}
