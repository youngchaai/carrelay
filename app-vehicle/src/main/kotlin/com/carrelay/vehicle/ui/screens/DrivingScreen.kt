package com.carrelay.vehicle.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.carrelay.vehicle.ui.components.CountdownRing
import com.carrelay.vehicle.ui.components.StepTrack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carrelay.core.care.CareModeMonitor
import com.carrelay.core.care.CareReason
import com.carrelay.core.handover.HandoverBriefingBuilder
import com.carrelay.core.shift.ShiftAdvice
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.StatBlock
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.conditionTone
import com.carrelay.vehicle.ui.components.primaryTone
import com.carrelay.vehicle.ui.blockTouches
import com.carrelay.vehicle.ui.latestWhile
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

/**
 * 주행 중 화면(CarUxRestrictions). 한눈에 읽히는 큰 글자, 버튼은 최대 2개.
 * 우선순위: 케어 모드 안부 확인 > 휴식·교대 권유 > 기본 상태.
 * 속도·기어는 Pleos 계기판에 이미 있으므로 여기서는 보여 주지 않습니다.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DrivingScreen(
    ui: UiState,
    onAnswerCare: () -> Unit,
    onAcceptShift: () -> Unit,
    onDeclineShift: () -> Unit,
    onVoiceMemo: () -> Unit,
) {
    // 기본 → 휴식 권유 → 안부 확인이 바뀔 때 살짝 겹쳐 바꿉니다(주행 중이라 짧게).
    val mode = when {
        ui.careCheckIn != null -> "care-${ui.careStage}"
        ui.shiftAdvice != null -> "shift"
        else -> "normal"
    }
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.96f)) togetherWith fadeOut(tween(160))
        },
        label = "driving",
        modifier = Modifier.fillMaxSize(),
    ) { m ->
        val s = latestWhile(m == mode, ui)
        val settling = transition.currentState != EnterExitState.Visible || transition.targetState != EnterExitState.Visible
        Column(
            Modifier.fillMaxSize().blockTouches(settling),
            verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val checkIn = s.careCheckIn
            val advice = s.shiftAdvice
            when {
                checkIn != null -> CareCheckIn(s, checkIn, onAnswerCare)
                advice != null -> ShiftAdviceView(s, advice, onAcceptShift, onDeclineShift)
                else -> DrivingStatus(s, onVoiceMemo)
            }
        }
    }
}

@Composable
private fun DrivingStatus(ui: UiState, onVoiceMemo: () -> Unit) {
    val driver = ui.currentDriver
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        if (driver != null) Avatar(driver.name, ui.avatarTone(driver.id), size = 96.dp)
        Column {
            Text(
                if (driver != null) "${driver.name}님 운전 중" else "운전 중",
                style = MaterialTheme.typography.displaySmall,
            )
            if (!ui.sessionConfirmed) {
                Text("운전자를 고르지 않아 '미확인'으로 기록 중이에요", style = MaterialTheme.typography.bodyLarge, color = CarRelayTheme.status.warning.content)
            }
        }
    }
    val snapshot = ui.snapshot
    val energy = snapshot?.energyPercent
    val low = energy != null && energy <= HandoverBriefingBuilder.LOW_ENERGY_PERCENT
    val warning = CarRelayTheme.status.warning
    val isBattery = snapshot?.energyLabel == "배터리"

    // 연료가 부족하면 연료 카드를 앞에, 경고 색으로 크게. 아니면 연속 운전이 먼저.
    val energyCard: @Composable (Modifier) -> Unit = { m ->
        SurfaceCard(
            m.heightIn(min = 260.dp),
            container = if (low) warning.container else MaterialTheme.colorScheme.surface,
            border = if (low) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = 36.dp,
        ) {
            StatBlock(
                label = snapshot?.energyLabel ?: "연료",
                value = energy?.let { "$it%" } ?: "-",
                icon = if (isBattery) CarIcons.Bolt else CarIcons.Fuel,
                valueStyle = MaterialTheme.typography.displayMedium,
                valueColor = if (low) warning.content else MaterialTheme.colorScheme.onSurface,
                labelStyle = MaterialTheme.typography.labelLarge,
                captionStyle = if (low) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                secondaryColor = if (low) warning.content else MaterialTheme.colorScheme.onSurfaceVariant,
                caption = when {
                    low -> (if (isBattery) "충전 필요" else "주유 필요") + (snapshot?.rangeKm?.let { " · ${it}km 남음" } ?: "")
                    else -> snapshot?.rangeKm?.let { "주행 가능 ${it}km" }
                },
            )
        }
    }
    val drivingCard: @Composable (Modifier) -> Unit = { m ->
        // 교대·휴식 권유까지 차오르는 막대: 권유가 갑자기 뜨지 않고 미리 보입니다(1초에 한 번만 조금씩 늘어남).
        val interval = ui.shiftIntervalMillis ?: (120 * 60_000L)
        val fill = (ui.continuousMillis.toFloat() / interval).coerceIn(0f, 1f)
        val leftMillis = (interval - ui.continuousMillis).coerceAtLeast(0L)
        val near = fill >= 0.75f
        SurfaceCard(m.heightIn(min = 260.dp), contentPadding = 36.dp) {
            StatBlock(
                label = "연속 운전",
                value = KoreanText.duration(ui.continuousMillis),
                icon = CarIcons.Clock,
                valueStyle = if (low) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                labelStyle = MaterialTheme.typography.labelLarge,
                captionStyle = MaterialTheme.typography.bodyLarge,
                valueColor = if (near) warning.content else MaterialTheme.colorScheme.onSurface,
            )
            Box(
                Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier.fillMaxWidth(fill).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                        .background(if (near) warning.content else MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                if (leftMillis == 0L) "쉬어 갈 시간이에요" else "쉬어 가기까지 ${KoreanText.duration(leftMillis)}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (near) warning.content else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(Modifier.widthIn(max = 1080.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        if (low) {
            energyCard(Modifier.weight(1.2f))
            drivingCard(Modifier.weight(1f))
        } else {
            drivingCard(Modifier.weight(1f))
            energyCard(Modifier.weight(1f))
        }
    }
    // 주행 중에도 손을 쓰지 않고 다음 운전자에게 말을 남깁니다(큰 버튼 하나).
    SecondaryButton(
        "말로 메모 남기기", onVoiceMemo, Modifier.widthIn(min = 720.dp),
        icon = CarIcons.Mic, height = Dimens.drivingButton, textStyle = MaterialTheme.typography.headlineSmall,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(CarIcons.Speaker, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        Text("주행 중에는 화면 대신 음성으로 안내해요", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShiftAdviceView(ui: UiState, advice: ShiftAdvice, onAccept: () -> Unit, onDecline: () -> Unit) {
    IconCircle(CarIcons.Rest, CarRelayTheme.status.warning, size = 120.dp, iconSize = 64.dp)
    Text(advice.headline, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
    Text(
        advice.detail.ifBlank { "가까운 휴게소에서 쉬어 가실래요?" },
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    advice.reliefDriverId?.let { id ->
        val level = ui.conditions[id]?.level
        if (level != null) {
            StatusBadge("${ui.nameOf(id)}님 컨디션 ${level.label}", conditionTone(level), icon = CarIcons.Person)
        }
    }
    Row(Modifier.widthIn(max = 1080.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        PrimaryButton(
            "휴게소 추가", onAccept, Modifier.weight(1f),
            icon = CarIcons.Rest, height = Dimens.drivingButton, textStyle = MaterialTheme.typography.headlineSmall,
        )
        SecondaryButton(
            "나중에", onDecline, Modifier.weight(1f),
            height = Dimens.drivingButton, textStyle = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun CareCheckIn(ui: UiState, reason: CareReason, onAnswer: () -> Unit) {
    val status = CarRelayTheme.status
    val guardian = ui.currentDriver?.guardianName ?: "보호자"
    // 안부 확인 3단계 중 지금 어디인지 (움직임 없이 그대로 그림: 주행 중 화면)
    val step = when (ui.careStage) {
        CareModeMonitor.Stage.WAYPOINT_ADDED -> 1
        CareModeMonitor.Stage.GUARDIAN_NOTIFIED -> 2
        else -> 0
    }
    StepTrack(
        steps = listOf("괜찮으세요?", "쉼터 안내", "${guardian}님께 알림"),
        current = step,
        color = when (step) {
            1 -> status.warning.content
            2 -> status.danger.content
            else -> MaterialTheme.colorScheme.primary
        },
        modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
        labelStyle = MaterialTheme.typography.titleLarge,
    )
    when (ui.careStage) {
        CareModeMonitor.Stage.WAYPOINT_ADDED -> {
            IconCircle(CarIcons.Rest, status.warning, size = 120.dp, iconSize = 64.dp)
            Text("가까운 졸음쉼터로\n안내를 시작했어요", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text("괜찮으시면 아래 버튼을 눌러 주세요", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CareModeMonitor.Stage.GUARDIAN_NOTIFIED -> {
            IconCircle(CarIcons.Bell, status.danger, size = 120.dp, iconSize = 64.dp)
            Text("${guardian}님께 알렸어요", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text("현재 위치와 상황을 함께 보냈어요. 괜찮으시면 버튼을 눌러 주세요", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        else -> {
            IconCircle(CarIcons.Shield, primaryTone(), size = 120.dp, iconSize = 64.dp)
            Text("괜찮으세요?", style = MaterialTheme.typography.displayLarge)
            Text(
                when (reason) {
                    CareReason.LONG_STOP_NO_INPUT -> "정차가 길어지고 조작이 없어서 여쭤봐요"
                    CareReason.REPEATED_HARSH_DECEL -> "급감속이 반복돼서 여쭤봐요"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    PrimaryButton(
        "괜찮아요", onAnswer, Modifier.widthIn(min = 800.dp),
        icon = CarIcons.Check, height = 160.dp, textStyle = MaterialTheme.typography.headlineLarge,
    )
    // 응답이 없으면 다음 단계로 넘어가기까지 남은 시간 (재촉하지 않게 차분한 색, 원 하나와 짧은 한 줄)
    val window = ui.careStageWindowMillis
    if (window != null && ui.careStage != CareModeMonitor.Stage.GUARDIAN_NOTIFIED) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CountdownRing(
                durationMillis = window, startedAtMillis = ui.careStageSinceMillis, size = 64.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textStyle = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (ui.careStage == CareModeMonitor.Stage.ASKING) "응답이 없으면 가까운 졸음쉼터로 안내해요"
                else "응답이 없으면 ${guardian}님께 알려요",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
