package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.handover.BriefingItem
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.Memo
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.IncidentStatusDialog
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.SectionTitle
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.components.incidentTone
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone

private val RESOLUTION_NOTES = listOf("수리했어요", "확인해 보니 문제없어요", "주유·충전했어요", "정비 예약했어요")
private val MEMO_KINDS = setOf(BriefingItem.Kind.NEW_MEMO, BriefingItem.Kind.UNRESOLVED_MEMO)

/**
 * F3 인수인계 브리핑.
 * 왼쪽: 차량이 알려 주는 것(상위 3개 + 차량 상태) / 오른쪽: 사람이 남긴 메모 / 아래: 다시 듣기·확인.
 * 같은 메모를 양쪽에 반복하지 않습니다.
 */
@Composable
fun BriefingScreen(
    ui: UiState,
    onAcknowledge: () -> Unit,
    onResolve: (memoId: String, note: String) -> Unit,
    onReplay: () -> Unit,
    onReportIssue: () -> Unit,
    onUpdateIncident: (id: String, IncidentStatus, note: String) -> Unit,
) {
    val b = ui.briefing
    if (b == null) {
        Text("브리핑을 준비하고 있어요", style = MaterialTheme.typography.headlineSmall)
        return
    }
    var resolvingMemoId by remember { mutableStateOf<String?>(null) }
    var openIncidentId by remember { mutableStateOf<String?>(null) }
    val vehicleItems = b.items.filter { it.kind !in MEMO_KINDS }
    val incidentsById = ui.incidents.associateBy { it.id }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = "${b.driver.name}님, 인수인계 브리핑",
            subtitle = b.lastSeen?.let { "내 마지막 확인(${formatDateTime(it.capturedAtMillis)}) 이후 달라진 점이에요" }
                ?: "처음 확인하는 차량 상태예요",
            leading = { Avatar(b.driver.name, ui.avatarTone(b.driver.id), size = 80.dp) },
        )

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(
                Modifier.weight(0.55f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) {
                SectionTitle("지금 확인할 것")
                vehicleItems.take(3).forEach { item ->
                    val inc = item.incidentId?.let(incidentsById::get)
                    BriefingRow(item, incident = inc, onClick = inc?.let { i -> { openIncidentId = i.id } })
                }
                VehicleStatusCard(b.current, b.lastSeen, incidents = ui.incidents, carHeight = 260.dp)
                vehicleItems.drop(3).forEach { item ->
                    val inc = item.incidentId?.let(incidentsById::get)
                    BriefingRow(item, compact = true, incident = inc, onClick = inc?.let { i -> { openIncidentId = i.id } })
                }
            }
            Column(
                Modifier.weight(0.45f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) {
                SectionTitle("남은 메모") {
                    StatusBadge("${b.unresolvedMemos.size}개", if (b.unresolvedMemos.isEmpty()) CarRelayTheme.status.ok else CarRelayTheme.status.memo)
                }
                if (b.unresolvedMemos.isEmpty()) {
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            IconCircle(CarIcons.CheckCircle, CarRelayTheme.status.ok)
                            Text("해결 안 된 메모가 없어요", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                b.unresolvedMemos.forEach { memo ->
                    MemoCard(memo, ui, isNew = !memo.isReadBy(b.driver.id), onResolveClick = { resolvingMemoId = memo.id })
                }
            }
        }

        // 인수 점검: 넘겨받은 차를 둘러보고 '이상 발견' 또는 '이상 없어요'
        ActionBar {
            SecondaryButton("다시 듣기", onReplay, Modifier.weight(0.8f), icon = CarIcons.Speaker)
            SecondaryButton("이상 발견", onReportIssue, Modifier.weight(0.8f), icon = CarIcons.Warning)
            PrimaryButton("이상 없어요, 확인했어요", onAcknowledge, Modifier.weight(1.6f), icon = CarIcons.Check)
        }
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

    resolvingMemoId?.let { memoId ->
        AlertDialog(
            onDismissRequest = { resolvingMemoId = null },
            title = { Text("어떻게 해결했나요?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "메모를 해결해도 차량 경고등은 따로 계속 표시돼요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RESOLUTION_NOTES.forEach { note ->
                        SecondaryButton(note, { onResolve(memoId, note); resolvingMemoId = null }, Modifier.fillMaxWidth(), height = 88.dp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { resolvingMemoId = null }) { Text("닫기", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

private data class ItemLook(val icon: androidx.compose.ui.graphics.vector.ImageVector, val source: String)

@Composable
private fun BriefingRow(
    item: BriefingItem,
    compact: Boolean = false,
    incident: Incident? = null,
    onClick: (() -> Unit)? = null,
) {
    val s = CarRelayTheme.status
    // 경고 색은 경고등 심각도로 정합니다(차량 상태 카드와 같은 규칙). priority = 기준값 + severity
    val severeNew = item.priority - 100 >= 3
    val severeOngoing = item.priority - 80 >= 3
    val (look, tone) = when (item.kind) {
        BriefingItem.Kind.OPEN_INCIDENT -> ItemLook(CarIcons.Car, "사고·파손 · 처리 완료 전까지 모두에게 표시") to
            (incident?.let { incidentTone(it.status) } ?: s.danger)
        BriefingItem.Kind.NEW_WARNING -> ItemLook(CarIcons.Warning, "새 경고 · 차량에서 감지") to (if (severeNew) s.danger else s.warning)
        BriefingItem.Kind.ONGOING_WARNING -> ItemLook(CarIcons.Warning, "차량에서 감지") to (if (severeOngoing) s.danger else s.warning)
        BriefingItem.Kind.ENERGY_LOW -> ItemLook(CarIcons.Fuel, "차량에서 감지") to s.warning
        BriefingItem.Kind.ENERGY_CHANGE -> ItemLook(CarIcons.Fuel, "지난 확인과 비교") to s.info
        BriefingItem.Kind.DISTANCE_SINCE_LAST -> ItemLook(CarIcons.Gauge, "지난 확인과 비교") to s.info
        BriefingItem.Kind.RESOLVED_MEMO -> ItemLook(CarIcons.CheckCircle, "사람이 남긴 정보") to s.ok
        BriefingItem.Kind.STALE_DATA -> ItemLook(CarIcons.Clock, "데이터 상태") to s.warning
        BriefingItem.Kind.SIGNAL_UNAVAILABLE -> ItemLook(CarIcons.Info, "데이터 상태") to neutralTone()
        BriefingItem.Kind.ALL_CLEAR -> ItemLook(CarIcons.CheckCircle, "차량에서 감지") to s.ok
        BriefingItem.Kind.NEW_MEMO, BriefingItem.Kind.UNRESOLVED_MEMO -> ItemLook(CarIcons.Memo, "사람이 남긴 정보") to s.memo
    }
    val isIncident = item.kind == BriefingItem.Kind.OPEN_INCIDENT
    SurfaceCard(
        contentPadding = if (compact) 18.dp else 24.dp,
        border = if (isIncident) BorderStroke(2.dp, tone.content) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
    ) {
        Row(
            Modifier.heightIn(min = if (compact) 48.dp else 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            IconCircle(look.icon, tone, size = if (compact) 48.dp else 64.dp, iconSize = if (compact) 26.dp else 34.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.message, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium)
                if (!compact) {
                    Text(look.source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onClick != null) StatusBadge("처리하기", tone)
        }
    }
}

@Composable
private fun MemoCard(memo: Memo, ui: UiState, isNew: Boolean, onResolveClick: () -> Unit) {
    val memoTone: StatusTone = CarRelayTheme.status.memo
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconCircle(CarIcons.Memo, memoTone, size = 56.dp, iconSize = 30.dp)
            Column(Modifier.weight(1f)) {
                Text("${ui.nameOf(memo.authorId)}의 메모", style = MaterialTheme.typography.labelMedium, color = memoTone.content)
                Text(formatDateTime(memo.createdAtMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isNew) StatusBadge("새 메모", memoTone)
        }
        Text(memo.text, style = MaterialTheme.typography.titleLarge)
        Text(
            if (memo.readBy.isEmpty()) "아직 아무도 읽지 않았어요" else "읽음: " + memo.readBy.keys.joinToString { ui.nameOf(it) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton("해결했어요", onResolveClick, Modifier.fillMaxWidth(), height = 88.dp, icon = CarIcons.Check)
    }
}
