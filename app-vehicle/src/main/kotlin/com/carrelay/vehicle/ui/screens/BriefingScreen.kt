package com.carrelay.vehicle.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.carrelay.core.handover.Briefing
import com.carrelay.core.handover.BriefingItem
import com.carrelay.core.model.IncidentStatus
import com.carrelay.vehicle.ui.CarRelayViewModel
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.blockTouches
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.CarTopView
import com.carrelay.vehicle.ui.components.CarTopViewAspect
import com.carrelay.vehicle.ui.components.IncidentStatusDialog
import com.carrelay.vehicle.ui.components.NoticeRow
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.QuietButton
import com.carrelay.vehicle.ui.components.ResolveMemoDialog
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.components.incidentMarks
import com.carrelay.vehicle.ui.components.incidentTone
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import kotlinx.coroutines.delay

private const val GREETING_MILLIS = 2_600L

/**
 * F3 인수인계 브리핑.
 * 새 운전자가 막 타면 먼저 환영 장면(이름·인사·확인할 것 개수·큰 차 그림)을 잠깐 보여 주고,
 * 차 그림이 브리핑 왼쪽 자리로 옮겨 가며 달라진 점이 한 줄씩 나타납니다.
 * 왼쪽: 차 그림과 숫자 두 개 / 오른쪽: 달라진 점 문장 목록 / 아래: 버튼 두 개.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalAnimationApi::class)
@Composable
fun BriefingScreen(
    ui: UiState,
    onAcknowledge: () -> Unit,
    onResolve: (memoId: String, note: String) -> Unit,
    onReplay: () -> Unit,
    onReportIssue: () -> Unit,
    onUpdateIncident: (id: String, IncidentStatus, note: String) -> Unit,
    onGreetingDone: () -> Unit,
) {
    val b = ui.briefing
    if (b == null) {
        Text("차 상태를 불러오고 있어요", style = MaterialTheme.typography.headlineSmall)
        return
    }
    var resolvingMemoId by remember { mutableStateOf<String?>(null) }
    var openIncidentId by remember { mutableStateOf<String?>(null) }
    val incidentsById = ui.incidents.associateBy { it.id }

    if (ui.greeting) {
        LaunchedEffect(b.driver.id) {
            delay(GREETING_MILLIS)
            onGreetingDone()
        }
    }

    SharedTransitionLayout(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = ui.greeting,
            transitionSpec = { fadeIn(tween(450, delayMillis = 150)) togetherWith fadeOut(tween(350)) },
            label = "welcome",
        ) { welcome ->
            val settling = transition.currentState != EnterExitState.Visible || transition.targetState != EnterExitState.Visible
            val carModifier = Modifier.sharedElement(
                rememberSharedContentState(key = "car"),
                animatedVisibilityScope = this@AnimatedContent,
                boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
            )
            Box(Modifier.fillMaxSize().blockTouches(settling)) {
                if (welcome) {
                    WelcomeView(ui, b, carModifier, onSkip = onGreetingDone)
                } else {
                    BriefingBody(
                        ui = ui,
                        b = b,
                        carModifier = carModifier,
                        onReplay = onReplay,
                        onReportIssue = onReportIssue,
                        onAcknowledge = onAcknowledge,
                        onIncident = { openIncidentId = it },
                        onMemo = { resolvingMemoId = it },
                    )
                }
            }
        }
    }

    resolvingMemoId?.let { memoId ->
        ResolveMemoDialog(
            memoText = ui.memos.firstOrNull { it.id == memoId }?.text ?: "메모",
            onDismiss = { resolvingMemoId = null },
            onResolve = { note ->
                onResolve(memoId, note)
                resolvingMemoId = null
            },
        )
    }
    openIncidentId?.let(incidentsById::get)?.let { inc ->
        IncidentStatusDialog(
            incident = inc,
            nameOf = ui::nameOf,
            onDismiss = { openIncidentId = null },
            onUpdate = { status, note ->
                onUpdateIncident(inc.id, status, note)
                openIncidentId = null
            },
        )
    }
}

/** 환영 장면: 그 사람의 색으로 물든 배경, 큰 아바타와 인사, 확인할 것 개수, 큰 차 그림 */
@Composable
private fun WelcomeView(ui: UiState, b: Briefing, carModifier: Modifier, onSkip: () -> Unit) {
    val tone = ui.avatarTone(b.driver.id)
    val notable = b.items.count { it.kind != BriefingItem.Kind.ALL_CLEAR && it.kind != BriefingItem.Kind.SIGNAL_UNAVAILABLE }
    val energy = b.current.energyPercent
    val hour = CarRelayViewModel.localHour(System.currentTimeMillis())
    val greetingLine = when (hour) {
        in 5..10 -> "좋은 아침이에요"
        in 11..16 -> "오늘도 안전 운전하세요"
        in 17..21 -> "저녁 운전, 천천히 다녀오세요"
        else -> "늦은 시간이에요. 조심히 다녀오세요"
    }
    // 아바타가 살짝 커지며 등장
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }

    Row(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(Brush.verticalGradient(listOf(tone.container, MaterialTheme.colorScheme.background)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSkip)
            .padding(horizontal = 56.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(b.driver.name, tone, size = 152.dp, modifier = Modifier.scale(pop.value))
            Spacer(Modifier.height(12.dp))
            Staggered(0) { Text(greetingLine, style = MaterialTheme.typography.headlineSmall, color = tone.content) }
            Staggered(1) { Text("${b.driver.name}님, 어서 오세요", style = MaterialTheme.typography.displayMedium) }
            Staggered(2) {
                Text(
                    if (notable > 0) "확인할 것이 ${notable}가지 있어요" else "달라진 점이 없어요. 편하게 출발하세요",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (energy != null) {
                Staggered(3) {
                    Text(
                        "${b.current.energyLabel} $energy%" + (b.current.rangeKm?.let { " · ${it}km 갈 수 있어요" } ?: ""),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CarTopView(
            modifier = Modifier.fillMaxHeight(0.95f).aspectRatio(CarTopViewAspect).then(carModifier),
            marks = incidentMarks(ui.incidents.filter { it.isOpen }),
        )
    }
}

/** 차례로 나타나기: index 마다 조금씩 늦게 아래에서 떠오릅니다. */
@Composable
private fun Staggered(index: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(380, delayMillis = 120 + index * 110)) +
            slideInVertically(tween(420, delayMillis = 120 + index * 110)) { it / 3 },
    ) { content() }
}

@Composable
private fun BriefingBody(
    ui: UiState,
    b: Briefing,
    carModifier: Modifier,
    onReplay: () -> Unit,
    onReportIssue: () -> Unit,
    onAcknowledge: () -> Unit,
    onIncident: (String) -> Unit,
    onMemo: (String) -> Unit,
) {
    val incidentsById = ui.incidents.associateBy { it.id }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (b.lastSeen != null) "${b.driver.name}님, 지난번과 달라진 점이에요" else "${b.driver.name}님, 지금 차 상태예요",
            leading = { Avatar(b.driver.name, ui.avatarTone(b.driver.id), size = 80.dp) },
        ) {
            QuietButton("다시 듣기", onReplay, icon = CarIcons.Speaker)
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            VehicleStatusCard(b.current, Modifier.weight(0.38f).fillMaxHeight(), incidents = ui.incidents, carModifier = carModifier)
            Column(
                Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) {
                b.items.forEachIndexed { i, item ->
                    key(item.kind, item.memoId, item.incidentId, item.message) {
                        Staggered(i) {
                            val inc = item.incidentId?.let { incidentsById[it] }
                            BriefingNotice(
                                item = item,
                                incidentStatus = inc?.status,
                                incidentDetail = inc?.let { "${ui.nameOf(it.reporterId)} 신고 · ${formatDateTime(it.createdAtMillis)}" },
                                onIncident = onIncident,
                                onMemo = onMemo,
                            )
                        }
                    }
                }
            }
        }

        // 넘겨받은 차를 둘러보고: 이상이 있으면 왼쪽, 괜찮으면 오른쪽 큰 버튼
        ActionBar {
            SecondaryButton("이상이 있어요", onReportIssue, Modifier.weight(1f), icon = CarIcons.Warning)
            PrimaryButton("확인했어요", onAcknowledge, Modifier.weight(1.6f), icon = CarIcons.Check)
        }
    }
}

/** 전기차 문장이면 번개, 아니면 주유기 */
private fun energyIcon(message: String) = if (message.startsWith("배터리")) CarIcons.Bolt else CarIcons.Fuel

/** 브리핑 한 줄. 색은 심각한 정도, 아이콘은 종류를 나타냅니다. */
@Composable
private fun BriefingNotice(
    item: BriefingItem,
    incidentStatus: IncidentStatus?,
    incidentDetail: String?,
    onIncident: (String) -> Unit,
    onMemo: (String) -> Unit,
) {
    val s = CarRelayTheme.status
    // 경고 색은 경고등 심각도로 정합니다. priority = 기준값 + severity
    val severe = (item.kind == BriefingItem.Kind.NEW_WARNING && item.priority - 100 >= 3) ||
        (item.kind == BriefingItem.Kind.ONGOING_WARNING && item.priority - 80 >= 3)
    when (item.kind) {
        BriefingItem.Kind.OPEN_INCIDENT -> NoticeRow(
            CarIcons.Car, incidentStatus?.let { incidentTone(it) } ?: s.danger, item.message,
            emphasized = true, detail = incidentDetail, actionLabel = "처리하기",
            onClick = item.incidentId?.let { id -> { onIncident(id) } },
        )
        BriefingItem.Kind.NEW_WARNING -> NoticeRow(CarIcons.Warning, if (severe) s.danger else s.warning, item.message, emphasized = true)
        BriefingItem.Kind.ONGOING_WARNING -> NoticeRow(CarIcons.Warning, if (severe) s.danger else s.warning, item.message)
        BriefingItem.Kind.ENERGY_LOW -> NoticeRow(energyIcon(item.message), s.warning, item.message, emphasized = true)
        BriefingItem.Kind.ENERGY_CHANGE -> NoticeRow(energyIcon(item.message), s.info, item.message)
        BriefingItem.Kind.DISTANCE_SINCE_LAST -> NoticeRow(CarIcons.Gauge, s.info, item.message)
        BriefingItem.Kind.NEW_MEMO, BriefingItem.Kind.UNRESOLVED_MEMO -> NoticeRow(
            CarIcons.Memo, s.memo, item.message,
            actionLabel = "해결했어요", onClick = item.memoId?.let { id -> { onMemo(id) } },
        )
        BriefingItem.Kind.RESOLVED_MEMO -> NoticeRow(CarIcons.CheckCircle, s.ok, item.message)
        BriefingItem.Kind.STALE_DATA -> NoticeRow(CarIcons.Clock, s.warning, item.message)
        // 읽지 못한 신호는 숨기지 않되, 작은 글씨 한 줄로(매번 큰 카드로 보이면 정작 중요한 것이 묻힙니다)
        BriefingItem.Kind.SIGNAL_UNAVAILABLE -> Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(CarIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Text(item.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BriefingItem.Kind.ALL_CLEAR -> NoticeRow(CarIcons.CheckCircle, s.ok, item.message, emphasized = true)
    }
}
