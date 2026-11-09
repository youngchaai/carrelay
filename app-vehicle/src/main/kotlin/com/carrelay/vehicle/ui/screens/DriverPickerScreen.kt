package com.carrelay.vehicle.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.core.profile.LicenseState
import com.carrelay.core.profile.PartyRoster
import com.carrelay.core.text.KoreanText
import com.carrelay.core.triplog.TripLog
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.components.conditionTone
import com.carrelay.vehicle.ui.components.primaryTone
import com.carrelay.vehicle.ui.formatKm
import com.carrelay.vehicle.ui.formatWhen
import com.carrelay.vehicle.ui.isCompany
import com.carrelay.vehicle.ui.lastEndedSession
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone
import com.carrelay.vehicle.ui.tripLogRows
import kotlin.math.roundToInt

/**
 * F2 운전자 선택 = 차를 넘겨받는 순간.
 * 왼쪽: 이 차가 지금 어떤 상태로 기다리는지(차 그림·숫자·마지막 주차·남은 메모·처리 안 된 사고).
 * 오른쪽: 내 이름을 한 번 누르면 끝. 카드에는 그 사람 색, 지난 운전, 꼭 알아야 할 것 두 가지까지만.
 * 기어 P에서만 고를 수 있습니다.
 */
@Composable
fun DriverPickerScreen(ui: UiState, onSelect: (String) -> Unit, onKeepSame: () -> Unit, onAdd: () -> Unit = {}) {
    val drivers = ui.drivers + Driver.GUEST
    // "바뀌었나요?"를 묻는 중에는 고르기만 하게, 새 사람 추가는 평소에만 보여 줍니다.
    val showAdd = !ui.askingDriverChange && PartyRoster.canAdd(ui.drivers)
    // 누르면 고른 카드만 테두리·체크로 남고 나머지는 옅어진 뒤 넘어갑니다(로그인 버튼이 바뀌는 순간처럼).
    var chosen by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chosen) {
        val id = chosen ?: return@LaunchedEffect
        delay(CHOOSE_MILLIS)
        onSelect(id)
        // 넘어가지 못했으면(예: 그사이 기어가 P가 아님) 다시 고를 수 있게 풉니다.
        delay(1_500)
        chosen = null
    }
    val pick: (String) -> Unit = { id -> if (chosen == null) chosen = id }
    // 왼쪽 차 카드는 옆에서 밀려 들어오고, 오른쪽 사람 카드는 한 장씩 아래에서 올라옵니다.
    val slide = remember { Animatable(0f) }
    LaunchedEffect(Unit) { slide.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        val snap = ui.snapshot
        if (snap != null) {
            VehicleStatusCard(
                snap,
                Modifier
                    .weight(0.31f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = slide.value
                        translationX = (1f - slide.value) * -64.dp.toPx()
                    },
                incidents = ui.incidents,
                title = ui.vehicleName.ifBlank { "차 상태" },
            ) { HandoverFacts(ui) }
        }

        Column(Modifier.weight(0.69f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            ScreenHeader(
                title = if (ui.askingDriverChange) "운전하는 분이 바뀌었나요?" else "누가 운전하시나요?",
                subtitle = when {
                    !ui.canSelectDriver -> "차를 세우고(P) 골라 주세요"
                    ui.askingDriverChange -> "문이 열렸다 닫혔어요. 같은 분이면 아래 버튼을 눌러 주세요"
                    ui.isCompany -> "내 이름을 눌러 주세요. 운행기록부의 사용자 칸이 자동으로 채워져요"
                    else -> "내 이름을 눌러 주세요. 지난번과 달라진 점을 알려 드려요"
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
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
                verticalArrangement = Arrangement.spacedBy(Dimens.gap),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(drivers, key = { _, d -> d.id }) { i, driver -> DriverCard(driver, ui, i, chosen, pick) }
                if (showAdd) item(key = "add") { AddDriverCard(ui, drivers.size, dimmed = chosen != null, onAdd) }
            }
        }
    }
}

/** 고른 카드를 보여 주는 시간 (그다음 환영 장면) */
private const val CHOOSE_MILLIS = 360L

/** 카드 등장: [index] 순서대로 조금씩 늦게 아래에서 떠오릅니다(화면에 처음 들어올 때 한 번). */
@Composable
private fun rememberRise(index: Int): Animatable<Float, AnimationVector1D> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L * index + 120L)
        a.animateTo(1f, tween(440, easing = FastOutSlowInEasing))
    }
    return a
}

/** 넘겨받기 전에 알아 둘 것: 누가 언제 세워 뒀는지, 남은 메모, 처리 안 된 사고, (회사 차) 업무사용비율 */
@Composable
private fun HandoverFacts(ui: UiState) {
    val s = CarRelayTheme.status
    val last = ui.lastEndedSession
    val memos = ui.memos.count { !it.isResolved }
    val open = ui.incidents.count { it.isOpen }
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (last != null) {
            FactRow(
                CarIcons.Clock, MaterialTheme.colorScheme.onSurfaceVariant,
                "${ui.nameOf(last.driverId)}님 · ${formatWhen(last.endedAtMillis ?: last.startedAtMillis)} 주차",
            )
        }
        if (open > 0) FactRow(CarIcons.Warning, s.danger.content, "처리 안 된 사고·파손 ${open}건")
        if (memos > 0) FactRow(CarIcons.Memo, s.memo.content, "남은 메모 ${memos}개")
        if (ui.isCompany) {
            val ratio = TripLog.summary(ui.tripLogRows()).businessRatio
            if (ratio != null) FactRow(CarIcons.Records, s.info.content, "운행기록부 업무사용비율 ${(ratio * 100).roundToInt()}%")
        }
        if (last == null && open == 0 && memos == 0) FactRow(CarIcons.CheckCircle, s.ok.content, "남겨진 메모나 사고가 없어요")
    }
}

@Composable
private fun FactRow(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 새 사람 추가: 점선 대신 옅은 테두리와 큰 + 로, 다른 카드보다 한 단계 조용하게 */
@Composable
private fun AddDriverCard(ui: UiState, index: Int, dimmed: Boolean, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.cardRadius)
    val c = MaterialTheme.colorScheme
    val rise = rememberRise(index)
    val dim by animateFloatAsState(if (dimmed) 0.3f else 1f, tween(260), label = "dim")
    Row(
        Modifier
            .fillMaxWidth()
            .height(232.dp)
            .graphicsLayer {
                alpha = (if (ui.canSelectDriver) 1f else 0.45f) * rise.value * dim
                translationY = (1f - rise.value) * 48.dp.toPx()
            }
            .clip(shape)
            .border(BorderStroke(2.dp, c.primary.copy(alpha = 0.35f)), shape)
            .clickable(enabled = ui.canSelectDriver && !dimmed, onClick = onAdd)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IconCircle(CarIcons.PersonAdd, primaryTone(), size = 88.dp, iconSize = 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (ui.isCompany) "새 직원 추가" else "새 가족 추가", style = MaterialTheme.typography.headlineSmall, color = c.primary)
            Text(
                "이름만 적으면 바로 탈 수 있어요. 면허 정보는 나중에 넣어도 돼요",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class CardNote(val text: String, val tone: StatusTone, val icon: ImageVector)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverCard(driver: Driver, ui: UiState, index: Int, chosen: String?, onSelect: (String) -> Unit) {
    val recommended = ui.recommendation?.driverId == driver.id
    val s = CarRelayTheme.status
    val tone = ui.avatarTone(driver.id)
    val license = ui.licenses[driver.id]
    val memoForMe = ui.memos.count { !it.isResolved && it.toDriverId == driver.id }
    val lastDrive = ui.sessions.lastOrNull { it.driverId == driver.id }
    // 중요한 것부터 두 개까지만
    val notes = buildList {
        if (memoForMe > 0) add(CardNote("나에게 메모 ${memoForMe}개", s.memo, CarIcons.Memo))
        if (recommended) add(CardNote("오늘 추천", primaryTone(), CarIcons.Route))
        when (license?.state) {
            LicenseState.EXPIRED -> add(CardNote("면허 갱신 기한 지남", s.danger, CarIcons.Warning))
            LicenseState.RENEW_SOON -> add(CardNote("면허 갱신 ${license?.daysLeft ?: 0}일 남음", s.warning, CarIcons.Clock))
            else -> Unit
        }
        if (license?.isNovice == true) add(CardNote("초보 운전", s.info, CarIcons.Info))
        if (ui.lastDriverId == driver.id) add(CardNote("방금 운전한 분", s.info, CarIcons.Handover))
    }.take(2)

    val shape = RoundedCornerShape(Dimens.cardRadius)
    val enabled = ui.canSelectDriver
    val isChosen = chosen == driver.id
    // 추천 받은 분, 또는 "바뀌었나요?"를 묻는 중이면 지금 운전하던 분을 테두리로 짚어 줍니다.
    val outlined = recommended || (ui.askingDriverChange && ui.currentDriver?.id == driver.id)
    val rise = rememberRise(index)
    val dim by animateFloatAsState(if (chosen != null && !isChosen) 0.3f else 1f, tween(260), label = "dim")
    val lift by animateFloatAsState(if (isChosen) 1.03f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "lift")
    val pop by animateFloatAsState(if (isChosen) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "pop")
    // 그 사람 색으로 위쪽을 살짝 물들여 이름보다 먼저 '내 카드'가 눈에 들어오게 합니다.
    Column(
        Modifier
            .fillMaxWidth()
            .height(232.dp)
            .graphicsLayer {
                alpha = (if (enabled) 1f else 0.45f) * rise.value * dim
                translationY = (1f - rise.value) * 48.dp.toPx()
                scaleX = lift
                scaleY = lift
            }
            .clip(shape)
            // 그 사람 색은 위쪽에 옅게만 (넓게 번지는 색은 줄임)
            .background(Brush.verticalGradient(0f to lerp(MaterialTheme.colorScheme.surface, tone.container, 0.7f), 0.5f to MaterialTheme.colorScheme.surface))
            .border(
                when {
                    isChosen -> BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
                    outlined -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                shape,
            )
            .clickable(enabled = enabled && chosen == null) { onSelect(driver.id) }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Avatar(if (driver.isGuest) "?" else driver.name, tone, size = 88.dp)
            Column(Modifier.weight(1f)) {
                Text(driver.name, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
                val sub = when {
                    driver.isGuest -> if (ui.isCompany) "등록 안 된 분" else "가족이 아닌 분"
                    else -> driver.department
                }
                if (sub != null) Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // 고르면 넘기기 표시가 체크로 바뀌며 톡 튀어나옵니다.
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                if (pop > 0.02f) {
                    Box(
                        Modifier.size(52.dp).scale(pop).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(CarIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Icon(CarIcons.Handover, contentDescription = null, tint = tone.content.copy(alpha = 0.55f), modifier = Modifier.size(32.dp))
                }
            }
        }
        // 지난 운전: 언제, 얼마나. 게스트는 기록이 어떻게 남는지 알려 줍니다.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(CarIcons.Clock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Text(
                when {
                    driver.isGuest -> "운전 기록은 게스트로 남아요"
                    lastDrive != null -> "지난 운전 ${formatWhen(lastDrive.endedAtMillis ?: lastDrive.startedAtMillis)}" +
                        (lastDrive.distanceKm?.takeIf { it > 0.0 }?.let { " · ${formatKm(it)}" } ?: "")
                    else -> "아직 운전 기록이 없어요"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { StatusBadge(it.text, it.tone, icon = it.icon) }
        }
    }
}
