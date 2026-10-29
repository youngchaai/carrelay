package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.care.CareDailySummary
import com.carrelay.core.incident.IncidentLog
import com.carrelay.core.model.Driver
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.IncidentStatusDialog
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SectionTitle
import com.carrelay.vehicle.ui.components.StatBlock
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.incidentTone
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

/** 운전자별 기록(F5 기초)과 케어 모드 하루 요약·안부 확인 기록(F6). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(ui: UiState, onUpdateIncident: (id: String, IncidentStatus, note: String) -> Unit) {
    var expandedAlert by remember { mutableStateOf<Long?>(null) }
    var dialogFor by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
        ) {
            ScreenHeader("운전 기록", "누가 얼마나 운전했는지 보여 드려요")
            val byDriver = ui.sessions.groupBy { it.driverId }
            if (byDriver.isEmpty()) {
                EmptyCard("아직 끝난 운전이 없어요. 운전자를 바꾸거나 '운전 끝내기'를 누르면 기록돼요.")
            }
            (ui.drivers + Driver.GUEST).forEach { driver ->
                val sessions = byDriver[driver.id].orEmpty()
                if (sessions.isEmpty()) return@forEach
                val distance = sessions.mapNotNull { it.distanceKm }.sum()
                val duration = sessions.sumOf { ((it.endedAtMillis ?: it.startedAtMillis) - it.startedAtMillis).coerceAtLeast(0L) }
                val unconfirmed = sessions.count { !it.confirmed }
                val decel = sessions.sumOf { it.harshDecelCount }
                val accel = sessions.sumOf { it.harshAccelCount }
                val stats = IncidentLog.statsFor(ui.incidents, driver.id)
                val status = CarRelayTheme.status
                SurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Avatar(driver.name, ui.avatarTone(driver.id), size = 72.dp)
                        Column(Modifier.weight(1f)) {
                            Text(driver.name, style = MaterialTheme.typography.titleLarge)
                            Text("운전 ${sessions.size}회 · ${KoreanText.duration(duration)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(KoreanText.distance(distance), style = MaterialTheme.typography.headlineMedium)
                            if (unconfirmed > 0) StatusBadge("운전자 확인 필요 ${unconfirmed}회", status.warning)
                        }
                    }
                    // 0회는 적지 않습니다. 있는 것만 보여 줘야 눈에 들어옵니다.
                    if (decel + accel + stats.selfReported + stats.foundAfterMyDrive > 0) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (decel > 0) StatusBadge("급정거 ${decel}회", status.warning)
                            if (accel > 0) StatusBadge("급가속 ${accel}회", status.warning)
                            if (stats.selfReported > 0) StatusBadge("직접 신고 ${stats.selfReported}건", status.info)
                            if (stats.foundAfterMyDrive > 0) StatusBadge("운전 후 발견 ${stats.foundAfterMyDrive}건", status.danger)
                        }
                    }
                }
            }

            // 사고·파손 이력: 처리 안 된 것부터
            SectionTitle("사고·파손 이력") {
                val open = ui.incidents.count { it.isOpen }
                StatusBadge(if (open == 0) "모두 처리됨" else "완료 전 ${open}건", if (open == 0) CarRelayTheme.status.ok else CarRelayTheme.status.danger)
            }
            if (ui.incidents.isEmpty()) EmptyCard("신고된 사고·파손이 없어요. 주차 후나 넘겨받을 때 차에서 바로 남길 수 있어요.")
            ui.incidents
                .sortedWith(compareBy<Incident> { !it.isOpen }.thenByDescending { it.createdAtMillis })
                .take(6)
                .forEach { inc -> IncidentRow(inc, ui) { dialogFor = inc.id } }
        }

        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
        ) {
            ScreenHeader("케어 모드 오늘", "보호자에게는 실시간 위치 대신 이 요약만 보내요")
            if (ui.careSummaries.isEmpty()) EmptyCard("케어 모드를 켠 가족이 없어요")
            ui.careSummaries.forEach { CareSummaryCard(it) }

            if (ui.guardianAlerts.isNotEmpty()) {
                SectionTitle("보호자 알림")
                // 요약 한 줄(시각·대상)로 접어 두고, 누르면 자세한 내용을 펼칩니다.
                ui.guardianAlerts.asReversed().take(3).forEach { alert ->
                    val open = expandedAlert == alert.atMillis
                    SurfaceCard(
                        contentPadding = 20.dp,
                        onClick = { expandedAlert = if (open) null else alert.atMillis },
                    ) {
                        Row(
                            Modifier.heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            StatusBadge("알림 보냄", CarRelayTheme.status.danger, icon = CarIcons.Bell)
                            Text(
                                "${ui.nameOf(alert.driverId)} · ${formatDateTime(alert.atMillis)}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(if (open) "접기" else "자세히", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (open) Text(alert.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (ui.careEvents.isNotEmpty()) {
                SectionTitle("안부 확인 기록")
                SurfaceCard(contentPadding = 24.dp) {
                    ui.careEvents.asReversed().take(8).forEach { e ->
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(formatDateTime(e.atMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(150.dp))
                            Text("${ui.nameOf(e.driverId)} · ${e.type.label}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    dialogFor?.let { id -> ui.incidents.firstOrNull { it.id == id } }?.let { inc ->
        IncidentStatusDialog(
            incident = inc,
            nameOf = ui::nameOf,
            onDismiss = { dialogFor = null },
            onUpdate = { st, note ->
                onUpdateIncident(inc.id, st, note)
                dialogFor = null
            },
        )
    }
}

@Composable
private fun IncidentRow(inc: Incident, ui: UiState, onClick: () -> Unit) {
    val tone = incidentTone(inc.status)
    SurfaceCard(contentPadding = 20.dp, onClick = if (inc.isOpen) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconCircle(if (inc.isOpen) CarIcons.Warning else CarIcons.CheckCircle, tone, size = 52.dp, iconSize = 28.dp)
            Column(Modifier.weight(1f)) {
                Text(inc.summary, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append("${ui.nameOf(inc.reporterId)} · ${inc.source.label} · ${formatDateTime(inc.createdAtMillis)}")
                        inc.previousDriverId?.let { append(" · 직전 운전 ${ui.nameOf(it)}") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(inc.status.label, tone)
        }
    }
}

@Composable
private fun CareSummaryCard(s: CareDailySummary) {
    val status = CarRelayTheme.status
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconCircle(CarIcons.Shield, status.memo, size = 56.dp, iconSize = 30.dp)
            Text("${s.driverName}님", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (s.guardianAlerts > 0) StatusBadge("보호자 알림 ${s.guardianAlerts}회", status.danger, icon = CarIcons.Bell)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBlock("운전", KoreanText.duration(s.drivingMillis), Modifier.weight(1f), valueStyle = MaterialTheme.typography.headlineSmall)
            StatBlock(
                "급감속", "${s.harshDecelCount}회", Modifier.weight(1f),
                valueStyle = MaterialTheme.typography.headlineSmall,
                valueColor = if (s.harshDecelCount > 0) status.warning.content else MaterialTheme.colorScheme.onSurface,
            )
            StatBlock("야간 운전", if (s.nightDriving) "있음" else "없음", Modifier.weight(1f), valueStyle = MaterialTheme.typography.headlineSmall)
            StatBlock(
                "안부 확인", "${s.checkInsAsked}회", Modifier.weight(1f),
                valueStyle = MaterialTheme.typography.headlineSmall,
                caption = if (s.checkInsAsked > 0) "응답 ${s.checkInsAnswered}회" else null,
            )
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconCircle(CarIcons.Info, neutralTone(), size = 56.dp, iconSize = 30.dp)
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}
