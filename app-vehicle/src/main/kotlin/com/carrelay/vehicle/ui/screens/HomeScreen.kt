package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import com.carrelay.core.model.DriveSession
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.ActionBar
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.IncidentStatusDialog
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.QuietButton
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

/**
 * 주차 중 기본 화면.
 * 왼쪽: 차량 상태(차 그림 + 사고·파손 위치) · 최근 운행 / 오른쪽: 남은 메모 · 사고·파손 처리.
 */
@Composable
fun HomeScreen(
    ui: UiState,
    onLeaveMemo: () -> Unit,
    onChangeDriver: () -> Unit,
    onReplayBriefing: () -> Unit,
    onHandOver: () -> Unit,
    onReport: () -> Unit,
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
            Text("운전자를 선택해 주세요", style = MaterialTheme.typography.headlineMedium)
            PrimaryButton("운전자 선택", onChangeDriver, Modifier.widthIn(min = 480.dp), icon = CarIcons.Person)
        }
        return
    }
    val unresolved = ui.memos.filter { !it.isResolved }.sortedByDescending { it.createdAtMillis }
    val openIncidents = IncidentLog.open(ui.incidents)
    var dialogFor by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = "${driver.name}님, 주차 중이에요",
            subtitle = if (!ui.sessionConfirmed) "선택 없이 출발해 자동으로 지정된 기록이에요 (미확인)" else "${ui.vehicleName} · 넘겨주기 전에 메모를 남길 수 있어요",
            leading = { Avatar(driver.name, ui.avatarTone(driver.id), size = 80.dp) },
        ) {
            QuietButton("운전자 바꾸기", onChangeDriver, icon = CarIcons.Handover)
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            Column(Modifier.weight(1.15f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                ui.snapshot?.let {
                    VehicleStatusCard(it, lastSeen = null, modifier = Modifier.fillMaxWidth(), incidents = ui.incidents, carHeight = 280.dp)
                }
                RecentDrivesCard(ui, Modifier.fillMaxWidth().weight(1f))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                SurfaceCard(Modifier.fillMaxWidth().weight(1f)) {
                    SectionTitle("남은 메모") {
                        StatusBadge("${unresolved.size}개", if (unresolved.isEmpty()) CarRelayTheme.status.ok else CarRelayTheme.status.memo)
                    }
                    if (unresolved.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            IconCircle(CarIcons.CheckCircle, CarRelayTheme.status.ok)
                            Text("남아 있는 메모가 없어요", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    unresolved.take(3).forEach { memo ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconCircle(CarIcons.Memo, CarRelayTheme.status.memo, size = 52.dp, iconSize = 28.dp)
                            Column(Modifier.weight(1f)) {
                                Text(memo.text, style = MaterialTheme.typography.titleMedium)
                                Text("${ui.nameOf(memo.authorId)}의 메모", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    QuietButton("브리핑 다시 듣기", onReplayBriefing, icon = CarIcons.Speaker)
                }
                SurfaceCard(Modifier.fillMaxWidth()) {
                    SectionTitle("사고·파손") {
                        StatusBadge(
                            if (openIncidents.isEmpty()) "없음" else "완료 전 ${openIncidents.size}건",
                            if (openIncidents.isEmpty()) CarRelayTheme.status.ok else CarRelayTheme.status.danger,
                        )
                    }
                    if (openIncidents.isEmpty()) {
                        Text(
                            "처리할 사고·파손이 없어요. 운행 중 문제가 있었다면 바로 남겨 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    openIncidents.take(2).forEach { inc ->
                        val tone = incidentTone(inc.status)
                        SurfaceCard(
                            Modifier.fillMaxWidth(),
                            container = tone.container,
                            border = null,
                            contentPadding = 18.dp,
                            onClick = { dialogFor = inc.id },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(inc.summary, style = MaterialTheme.typography.titleMedium, color = tone.content)
                                    Text(
                                        "${ui.nameOf(inc.reporterId)} 신고 · ${formatDateTime(inc.createdAtMillis)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tone.content,
                                    )
                                }
                                StatusBadge(inc.status.label, tone)
                            }
                        }
                    }
                    SecondaryButton("사고·파손 신고", onReport, Modifier.fillMaxWidth(), height = 88.dp, icon = CarIcons.Warning)
                }
            }
        }

        ActionBar {
            PrimaryButton("메모 남기기", onLeaveMemo, Modifier.weight(1f), icon = CarIcons.Memo)
            SecondaryButton("운전 끝내기 (넘겨주기)", onHandOver, Modifier.weight(1f), icon = CarIcons.Handover)
        }
    }

    dialogFor?.let { id -> ui.incidents.firstOrNull { it.id == id } }?.let { inc ->
        IncidentStatusDialog(
            incident = inc,
            nameOf = ui::nameOf,
            onDismiss = { dialogFor = null },
            onUpdate = { status, note ->
                onUpdateIncident(inc.id, status, note)
                dialogFor = null
            },
        )
    }
}

/** 최근 운행 3건: 누가, 언제, 얼마나, 급정거·급가속이 있었는지 */
@Composable
private fun RecentDrivesCard(ui: UiState, modifier: Modifier) {
    val recent = ui.sessions.asReversed().take(3)
    SurfaceCard(modifier) {
        SectionTitle("최근 운행") {
            StatusBadge("운전자별 기록", neutralTone(), icon = CarIcons.Records)
        }
        if (recent.isEmpty()) {
            Text(
                "아직 끝난 운전이 없어요. '운전 끝내기'를 누르면 여기에 쌓여요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        recent.forEach { s -> DriveRow(ui, s) }
    }
}

@Composable
private fun DriveRow(ui: UiState, s: DriveSession) {
    val status = CarRelayTheme.status
    val duration = ((s.endedAtMillis ?: s.startedAtMillis) - s.startedAtMillis).coerceAtLeast(0L)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(ui.nameOf(s.driverId), ui.avatarTone(s.driverId), size = 52.dp)
        Column(Modifier.weight(1f)) {
            Text(ui.nameOf(s.driverId), style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append(formatDateTime(s.endedAtMillis ?: s.startedAtMillis))
                    s.distanceKm?.let { append(" · ${KoreanText.distance(it)}") }
                    if (duration > 0) append(" · ${KoreanText.duration(duration)}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (s.harshDecelCount > 0) StatusBadge("급정거 ${s.harshDecelCount}", status.warning)
        if (s.harshAccelCount > 0) StatusBadge("급가속 ${s.harshAccelCount}", status.warning)
        if (s.harshDecelCount == 0 && s.harshAccelCount == 0) StatusBadge("급정거·급가속 없음", status.ok)
        if (!s.confirmed) StatusBadge("미확인", neutralTone())
    }
}
