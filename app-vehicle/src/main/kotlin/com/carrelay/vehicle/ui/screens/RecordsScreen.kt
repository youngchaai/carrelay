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
import com.carrelay.core.triplog.TripLog
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.icon
import com.carrelay.vehicle.ui.components.primaryTone
import com.carrelay.vehicle.ui.isCompany
import com.carrelay.vehicle.ui.tripLogRows
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.HorizontalDivider
import java.util.Locale
import kotlin.math.roundToInt
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

/**
 * 가족 차: 운전자별 기록(F5 기초)과 케어 모드 하루 요약·안부 확인 기록(F6).
 * 회사 차: 운행기록부(자동 작성)와 직원별 기록·사고 이력. 케어 모드는 회사 차에서 쓰지 않습니다.
 */
@Composable
fun RecordsScreen(ui: UiState, onUpdateIncident: (id: String, IncidentStatus, note: String) -> Unit) {
    var dialogFor by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        if (ui.isCompany) {
            Column(
                Modifier.weight(1.9f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) { TripLogSection(ui) }
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) {
                DriverRecords(ui, title = "직원별 기록")
                IncidentHistory(ui) { dialogFor = it }
            }
        } else {
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) {
                DriverRecords(ui, title = "운전 기록")
                IncidentHistory(ui) { dialogFor = it }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.gapS),
            ) { CareSection(ui) }
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

// ── 회사 차: 운행기록부 ─────────────────────────────────

private fun km(v: Double?): String = v?.let { String.format(Locale.KOREA, "%,.1f", it) } ?: "-"

/** 업무용승용차 운행기록부(국세청 고시 서식)의 칸을 운전자 선택·계기판 누적거리·운행 목적으로 채운 표 */
@Composable
private fun TripLogSection(ui: UiState) {
    val rows = ui.tripLogRows()
    val sum = TripLog.summary(rows)
    val status = CarRelayTheme.status
    ScreenHeader("운행기록부", "${ui.vehicleName} · 손으로 적을 칸 없이 자동으로 채워요")
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBlock("총 주행", "${km(sum.totalKm)}km", Modifier.weight(1f), valueStyle = MaterialTheme.typography.headlineMedium)
            StatBlock(
                "업무용 사용거리", "${km(sum.businessUseKm)}km", Modifier.weight(1.2f),
                valueStyle = MaterialTheme.typography.headlineMedium,
                caption = "출퇴근 ${km(sum.commuteKm)} · 일반 업무 ${km(sum.businessKm)}",
            )
            StatBlock(
                "업무사용비율", sum.businessRatio?.let { "${(it * 100).roundToInt()}%" } ?: "-", Modifier.weight(1f),
                valueStyle = MaterialTheme.typography.headlineMedium,
                valueColor = MaterialTheme.colorScheme.primary,
                caption = if (sum.unknownKm > 0) "목적 미입력 ${km(sum.unknownKm)}km 제외" else null,
            )
        }
    }
    if (rows.isEmpty()) {
        EmptyCard("아직 운행이 없어요. 운전자를 고르고 운행 목적을 누르면 여기에 적혀요.")
        return
    }
    SurfaceCard(contentPadding = 20.dp) {
        TripRowLayout(
            { Text("날짜", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            { Text("사용자", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            { Text("주행 전", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            { Text("주행 후", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            { Text("주행거리", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            { Text("목적", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        rows.asReversed().take(8).forEach { r ->
            TripRowLayout(
                { Text(formatDateTime(r.startedAtMillis), style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false) },
                {
                    Column {
                        Text(r.driverName, style = MaterialTheme.typography.bodyLarge)
                        val sub = listOfNotNull(r.department) + r.notes.filter { it != TripLog.NOTE_NO_PURPOSE }
                        if (sub.isNotEmpty()) {
                            Text(
                                sub.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (r.notes.any { it != TripLog.NOTE_NO_PURPOSE }) status.warning.content else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                { Text(km(r.startOdometerKm), style = MaterialTheme.typography.bodyMedium) },
                { Text(if (r.inProgress) "운행 중" else km(r.endOdometerKm), style = MaterialTheme.typography.bodyMedium) },
                { Text(r.distanceKm?.let { "${km(it)}km" } ?: "-", style = MaterialTheme.typography.bodyLarge) },
                {
                    val p = r.purpose
                    when {
                        p == null -> StatusBadge("미입력", status.warning)
                        p.isBusinessUse -> StatusBadge(p.label, primaryTone(), icon = p.icon())
                        else -> StatusBadge(p.label, neutralTone(), icon = p.icon())
                    }
                },
            )
        }
    }
    Text(
        "주행 전·후 거리는 Pleos 차량 SDK의 누적 주행거리, 사용자는 운전자 선택, 목적은 출발 전에 누른 버튼에서 가져와요. " +
            "업무용 사용거리 = 출퇴근 + 일반 업무. 관리자 화면으로 보내기는 동기화 서버를 붙이는 다음 단계예요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 운행기록부 한 줄의 칸 너비 (머리글과 내용이 같은 줄을 씁니다) */
@Composable
private fun TripRowLayout(
    date: @Composable () -> Unit,
    user: @Composable () -> Unit,
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
    distance: @Composable () -> Unit,
    purpose: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1.1f)) { date() }
        Box(Modifier.weight(1.4f)) { user() }
        Box(Modifier.weight(1.15f)) { before() }
        Box(Modifier.weight(1.15f)) { after() }
        Box(Modifier.weight(1.0f)) { distance() }
        Box(Modifier.weight(1.45f)) { purpose() }
    }
}

// ── 공통: 운전자별 기록, 사고·파손 이력 ─────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverRecords(ui: UiState, title: String) {
    ScreenHeader(title, "누가 얼마나 운전했는지 보여 드려요")
    val byDriver = ui.sessions.groupBy { it.driverId }
    if (byDriver.isEmpty()) {
        EmptyCard("아직 끝난 운전이 없어요. 운전자를 바꾸거나 '운전 끝내기'를 누르면 기록돼요.")
    }
    // 삭제한 사람도 지난 운전이 있으면 이름과 함께 남깁니다(회사 차는 운행기록부 증빙과 맞춰야 함).
    val formerIds = ui.formerDrivers.map { it.id }.toSet()
    (ui.drivers + ui.formerDrivers + Driver.GUEST).forEach { driver ->
        val sessions = byDriver[driver.id].orEmpty()
        if (sessions.isEmpty()) return@forEach
        val removed = driver.id in formerIds
        val distance = sessions.mapNotNull { it.distanceKm }.sum()
        val duration = sessions.sumOf { ((it.endedAtMillis ?: it.startedAtMillis) - it.startedAtMillis).coerceAtLeast(0L) }
        val unconfirmed = sessions.count { !it.confirmed }
        val decel = sessions.sumOf { it.harshDecelCount }
        val accel = sessions.sumOf { it.harshAccelCount }
        val stats = IncidentLog.statsFor(ui.incidents, driver.id)
        val status = CarRelayTheme.status
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Avatar(driver.name, if (removed) neutralTone() else ui.avatarTone(driver.id), size = 72.dp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(driver.name, style = MaterialTheme.typography.titleLarge)
                        if (removed) StatusBadge("삭제됨", neutralTone())
                    }
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
}

/** 사고·파손 이력: 처리 안 된 것부터 */
@Composable
private fun IncidentHistory(ui: UiState, onOpen: (String) -> Unit) {
    SectionTitle("사고·파손 이력") {
        val open = ui.incidents.count { it.isOpen }
        StatusBadge(if (open == 0) "모두 처리됨" else "완료 전 ${open}건", if (open == 0) CarRelayTheme.status.ok else CarRelayTheme.status.danger)
    }
    if (ui.incidents.isEmpty()) {
        EmptyCard(
            if (ui.isCompany) "신고된 사고·파손이 없어요. 신고하면 관리자에게 보고되고 처리될 때까지 모두에게 보여요."
            else "신고된 사고·파손이 없어요. 주차 후나 넘겨받을 때 차에서 바로 남길 수 있어요.",
        )
    }
    ui.incidents
        .sortedWith(compareBy<Incident> { !it.isOpen }.thenByDescending { it.createdAtMillis })
        .take(6)
        .forEach { inc -> IncidentRow(inc, ui) { onOpen(inc.id) } }
}

// ── 가족 차: 케어 모드 ──────────────────────────────────

@Composable
private fun CareSection(ui: UiState) {
    var expandedAlert by remember { mutableStateOf<Long?>(null) }
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
