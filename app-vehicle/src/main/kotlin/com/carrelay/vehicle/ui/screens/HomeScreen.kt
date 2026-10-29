package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.carrelay.core.incident.IncidentLog
import com.carrelay.core.model.IncidentStatus
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.IncidentStatusDialog
import com.carrelay.vehicle.ui.components.NoticeRow
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.QuietButton
import com.carrelay.vehicle.ui.components.ResolveMemoDialog
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.SectionTitle
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.components.incidentTone
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

/**
 * 주차 중 기본 화면. 왼쪽: 차 상태 / 오른쪽: 알려 드릴 것(사고·파손, 남은 메모) / 아래: 큰 버튼 두 개.
 * 운전 기록처럼 자주 보지 않는 정보는 '기록' 화면에 둡니다.
 */
@Composable
fun HomeScreen(
    ui: UiState,
    onLeaveMemo: () -> Unit,
    onChangeDriver: () -> Unit,
    onReplayBriefing: () -> Unit,
    onHandOver: () -> Unit,
    onReport: () -> Unit,
    onResolveMemo: (memoId: String, note: String) -> Unit,
    onUpdateIncident: (id: String, IncidentStatus, note: String) -> Unit,
) {
    val driver = ui.currentDriver
    if (driver == null) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconCircle(CarIcons.Person, neutralTone(), size = 120.dp, iconSize = 60.dp)
            Text("누가 운전하는지 골라 주세요", style = MaterialTheme.typography.headlineMedium)
            PrimaryButton("운전자 고르기", onChangeDriver, Modifier.widthIn(min = 480.dp), icon = CarIcons.Person)
        }
        return
    }
    val unresolved = ui.memos.filter { !it.isResolved }.sortedByDescending { it.createdAtMillis }
    val openIncidents = IncidentLog.open(ui.incidents)
    var incidentFor by remember { mutableStateOf<String?>(null) }
    var memoFor by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = "${driver.name}님, 주차 중이에요",
            subtitle = if (!ui.sessionConfirmed) "운전자를 고르지 않고 출발했어요. 다른 분이면 '운전자 바꾸기'를 눌러 주세요" else null,
            leading = { Avatar(driver.name, ui.avatarTone(driver.id), size = 80.dp) },
        ) {
            QuietButton("운전자 바꾸기", onChangeDriver, icon = CarIcons.Handover)
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            ui.snapshot?.let { VehicleStatusCard(it, Modifier.weight(0.38f).fillMaxHeight(), incidents = ui.incidents) }
            Column(Modifier.weight(0.62f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                SectionTitle("알려 드릴 것")
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
                ) {
                    openIncidents.forEach { inc ->
                        NoticeRow(
                            CarIcons.Car, incidentTone(inc.status),
                            "${inc.summary} · ${inc.status.label}",
                            emphasized = true,
                            detail = "${ui.nameOf(inc.reporterId)} 신고 · ${formatDateTime(inc.createdAtMillis)}",
                            actionLabel = "처리하기", onClick = { incidentFor = inc.id },
                        )
                    }
                    unresolved.forEach { memo ->
                        NoticeRow(
                            CarIcons.Memo, CarRelayTheme.status.memo,
                            "${ui.nameOf(memo.authorId)}의 메모: ${memo.text}",
                            actionLabel = "해결했어요", onClick = { memoFor = memo.id },
                        )
                    }
                    if (openIncidents.isEmpty() && unresolved.isEmpty()) {
                        NoticeRow(CarIcons.CheckCircle, CarRelayTheme.status.ok, "알려 드릴 것이 없어요", emphasized = true)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                    QuietButton("다시 듣기", onReplayBriefing, icon = CarIcons.Speaker)
                    QuietButton("사고·파손 신고", onReport, icon = CarIcons.Warning)
                }
            }
        }

        ActionBar {
            SecondaryButton("운전 끝내기", onHandOver, Modifier.weight(1f), icon = CarIcons.Handover)
            PrimaryButton("메모 남기기", onLeaveMemo, Modifier.weight(1.4f), icon = CarIcons.Memo)
        }
    }

    incidentFor?.let { id -> ui.incidents.firstOrNull { it.id == id } }?.let { inc ->
        IncidentStatusDialog(
            incident = inc,
            nameOf = ui::nameOf,
            onDismiss = { incidentFor = null },
            onUpdate = { status, note ->
                onUpdateIncident(inc.id, status, note)
                incidentFor = null
            },
        )
    }
    memoFor?.let { id -> ui.memos.firstOrNull { it.id == id } }?.let { memo ->
        ResolveMemoDialog(
            memoText = memo.text,
            onDismiss = { memoFor = null },
            onResolve = { note ->
                onResolveMemo(memo.id, note)
                memoFor = null
            },
        )
    }
}
