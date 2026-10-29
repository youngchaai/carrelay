package com.carrelay.vehicle.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carrelay.vehicle.feature.VOICE_MEMO_EXAMPLES
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.VoiceMemoUi
import com.carrelay.vehicle.ui.icon
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

/**
 * 말로 남기는 메모. 듣기 → "이렇게 남길게요" 확인 → 저장.
 * 주행 중에는 버튼 두 개만 두고, 누르지 않아도 몇 초 뒤 저장합니다.
 */
@Composable
fun VoiceMemoOverlay(
    ui: UiState,
    voice: VoiceMemoUi,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onPickExample: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            // 뒤 화면이 눌리지 않게 막습니다.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        SurfaceCard(
            Modifier.widthIn(max = 1240.dp).fillMaxWidth(0.82f),
            border = null,
            contentPadding = 48.dp,
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                when (voice.stage) {
                    VoiceMemoUi.Stage.LISTENING -> Listening(voice, onStop)
                    VoiceMemoUi.Stage.EXAMPLES -> Examples(onPickExample, onClose)
                    VoiceMemoUi.Stage.CONFIRM -> Confirm(ui, voice, onSave, onRetry, onClose)
                    VoiceMemoUi.Stage.FAILED -> Failed(voice, onRetry, onClose)
                }
            }
        }
    }
}

@Composable
private fun Listening(voice: VoiceMemoUi, onStop: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    Box(Modifier.scale(pulse)) { IconCircle(CarIcons.Mic, primaryTone(), size = 144.dp, iconSize = 76.dp) }
    Text("듣고 있어요", style = MaterialTheme.typography.displaySmall)
    Text(
        voice.heard.ifBlank { "예: \"다음 사람한테 주유하라고 전해 줘\"" },
        style = if (voice.heard.isBlank()) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
        color = if (voice.heard.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    SecondaryButton("그만 듣기", onStop, Modifier.widthIn(min = 480.dp), icon = CarIcons.Close, height = 120.dp)
}

@Composable
private fun Examples(onPick: (String) -> Unit, onClose: () -> Unit) {
    IconCircle(CarIcons.Mic, primaryTone(), size = 120.dp, iconSize = 64.dp)
    Text("어떤 말을 남길까요?", style = MaterialTheme.typography.displaySmall)
    Text(
        "가상 차량에는 마이크가 없어 예시 문장을 골라요. 실제 차에서는 이렇게 말하면 돼요.",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
        VOICE_MEMO_EXAMPLES.forEach { sentence ->
            SurfaceCard(
                Modifier.fillMaxWidth().heightIn(min = 104.dp),
                container = MaterialTheme.colorScheme.surfaceContainer,
                border = null,
                contentPadding = 24.dp,
                onClick = { onPick(sentence) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    androidx.compose.material3.Icon(
                        CarIcons.Mic, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp),
                    )
                    Text("\"$sentence\"", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
    SecondaryButton("닫기", onClose, Modifier.widthIn(min = 480.dp), icon = CarIcons.Close)
}

@Composable
private fun Confirm(ui: UiState, voice: VoiceMemoUi, onSave: () -> Unit, onRetry: () -> Unit, onClose: () -> Unit) {
    val draft = voice.draft ?: return
    val memo = CarRelayTheme.status.memo
    Text("이렇게 남길게요", style = MaterialTheme.typography.displaySmall)
    SurfaceCard(Modifier.fillMaxWidth(), container = memo.container, border = null, contentPadding = 36.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            IconCircle(draft.preset?.icon ?: CarIcons.Memo, memo, size = 104.dp, iconSize = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    draft.toDriverId?.let { "${ui.nameOf(it)}에게" } ?: "다음 운전자 모두에게",
                    style = MaterialTheme.typography.titleLarge,
                    color = memo.content,
                )
                Text(draft.text, style = MaterialTheme.typography.displaySmall, color = memo.content)
            }
        }
    }
    Text(
        "들은 말: \"${voice.heard}\"",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    val auto = voice.autoSaveIn
    if (auto != null) {
        // 주행 중: 누르지 않아도 저장됩니다. 버튼은 두 개만.
        Text("${auto}초 뒤에 저장돼요", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            SecondaryButton(
                "취소", onClose, Modifier.weight(1f),
                icon = CarIcons.Close, height = Dimens.drivingButton, textStyle = MaterialTheme.typography.headlineSmall,
            )
            PrimaryButton(
                "지금 남기기", onSave, Modifier.weight(1f),
                icon = CarIcons.Check, height = Dimens.drivingButton, textStyle = MaterialTheme.typography.headlineSmall,
            )
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            SecondaryButton("다시 말하기", onRetry, Modifier.weight(1f), icon = CarIcons.Mic, height = 120.dp)
            PrimaryButton("남기기", onSave, Modifier.weight(1.4f), icon = CarIcons.Check, height = 120.dp)
        }
        QuietButton("닫기", onClose, icon = CarIcons.Close)
    }
}

@Composable
private fun Failed(voice: VoiceMemoUi, onRetry: () -> Unit, onClose: () -> Unit) {
    IconCircle(CarIcons.Mic, neutralTone(), size = 120.dp, iconSize = 64.dp)
    Text(voice.message ?: "잘 듣지 못했어요", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
    if (voice.heard.isNotBlank()) {
        Text("들은 말: \"${voice.heard}\"", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
        "예: \"다음 사람한테 주유하라고 전해 줘\"",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        SecondaryButton("닫기", onClose, Modifier.weight(1f), icon = CarIcons.Close, height = 120.dp)
        PrimaryButton("다시 말하기", onRetry, Modifier.weight(1.4f), icon = CarIcons.Mic, height = 120.dp)
    }
}
