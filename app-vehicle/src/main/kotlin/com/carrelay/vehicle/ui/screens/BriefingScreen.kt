package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.handover.BriefingItem
import com.carrelay.core.model.IncidentStatus
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IncidentStatusDialog
import com.carrelay.vehicle.ui.components.NoticeRow
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.QuietButton
import com.carrelay.vehicle.ui.components.ResolveMemoDialog
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.components.incidentTone
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

/**
 * F3 인수인계 브리핑.
 * 왼쪽: 차 그림과 숫자 두 개 / 오른쪽: 달라진 점을 문장 목록으로(중요한 것부터) / 아래: 버튼 두 개.
 * 출처·시각 같은 보조 정보는 빼고, 사람이 읽는 순서대로 한 줄씩 보여 줍니다.
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
        Text("차 상태를 불러오고 있어요", style = MaterialTheme.typography.headlineSmall)
        return
    }
    var resolvingMemoId by remember { mutableStateOf<String?>(null) }
    var openIncidentId by remember { mutableStateOf<String?>(null) }
    val incidentsById = ui.incidents.associateBy { it.id }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (b.lastSeen != null) "${b.driver.name}님, 지난번과 달라진 점이에요" else "${b.driver.name}님, 지금 차 상태예요",
            leading = { Avatar(b.driver.name, ui.avatarTone(b.driver.id), size = 80.dp) },
        ) {
            QuietButton("다시 듣기", onReplay, icon = CarIcons.Speaker)
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            VehicleStatusCard(b.current, Modifier.weight(0.38f).fillMaxHeight(), incidents = ui.incidents)
            Column(
                Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) {
                b.items.forEach { item ->
                    val inc = item.incidentId?.let { incidentsById[it] }
                    BriefingNotice(
                        item = item,
                        incidentStatus = inc?.status,
                        incidentDetail = inc?.let { "${ui.nameOf(it.reporterId)} 신고 · ${formatDateTime(it.createdAtMillis)}" },
                        onIncident = { openIncidentId = it },
                        onMemo = { resolvingMemoId = it },
                    )
                }
            }
        }

        // 넘겨받은 차를 둘러보고: 이상이 있으면 왼쪽, 괜찮으면 오른쪽 큰 버튼
        ActionBar {
            SecondaryButton("이상이 있어요", onReportIssue, Modifier.weight(1f), icon = CarIcons.Warning)
            PrimaryButton("확인했어요", onAcknowledge, Modifier.weight(1.6f), icon = CarIcons.Check)
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
        BriefingItem.Kind.ENERGY_LOW -> NoticeRow(CarIcons.Fuel, s.warning, item.message, emphasized = true)
        BriefingItem.Kind.ENERGY_CHANGE -> NoticeRow(CarIcons.Fuel, s.info, item.message)
        BriefingItem.Kind.DISTANCE_SINCE_LAST -> NoticeRow(CarIcons.Gauge, s.info, item.message)
        BriefingItem.Kind.NEW_MEMO, BriefingItem.Kind.UNRESOLVED_MEMO -> NoticeRow(
            CarIcons.Memo, s.memo, item.message,
            actionLabel = "해결했어요", onClick = item.memoId?.let { id -> { onMemo(id) } },
        )
        BriefingItem.Kind.RESOLVED_MEMO -> NoticeRow(CarIcons.CheckCircle, s.ok, item.message)
        BriefingItem.Kind.STALE_DATA -> NoticeRow(CarIcons.Clock, s.warning, item.message)
        BriefingItem.Kind.SIGNAL_UNAVAILABLE -> NoticeRow(CarIcons.Info, neutralTone(), item.message)
        BriefingItem.Kind.ALL_CLEAR -> NoticeRow(CarIcons.CheckCircle, s.ok, item.message, emphasized = true)
    }
}
