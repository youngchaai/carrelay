package com.carrelay.vehicle.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carrelay.core.handover.HandoverBriefingBuilder
import com.carrelay.core.model.CarZone
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.formatTime
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * 차량 상태. 지금 값 옆에 '내 마지막 확인' 값을 함께 보여 주고(변화가 보이는 수치),
 * 출처(차량에서 감지)와 갱신 시각을 같은 자리에 둡니다(출처가 보이는 정보).
 *
 * [carHeight] 를 주면 위에서 본 차 그림에 처리 안 된 사고·파손 위치를 함께 표시하고,
 * 수치는 그림 옆에 세로로 쌓습니다.
 */
@Composable
fun VehicleStatusCard(
    current: VehicleSnapshot,
    lastSeen: VehicleSnapshot?,
    modifier: Modifier = Modifier,
    incidents: List<Incident> = emptyList(),
    carHeight: Dp? = null,
) {
    val status = CarRelayTheme.status
    val energy = current.energyPercent
    val low = energy != null && energy <= HandoverBriefingBuilder.LOW_ENERGY_PERCENT
    val before = lastSeen?.energyPercent
    val shownEnergy by animateIntAsState(energy ?: 0, tween(700), label = "energy")
    val shownRange by animateIntAsState(current.rangeKm ?: 0, tween(700), label = "range")
    val open = incidents.filter { it.isOpen }

    val energyValue = if (energy != null) "$shownEnergy%" else "정보 없음"
    val energyCaption = when {
        energy == null -> null
        lastSeen == null -> "첫 확인"
        before != null -> "지난 확인 $before%"
        else -> null
    }
    val energyIcon = if (current.energyLabel == "배터리") CarIcons.Bolt else CarIcons.Fuel
    val energyColor = if (low) status.warning.content else MaterialTheme.colorScheme.onSurface
    val rangeValue = if (current.rangeKm != null) "${shownRange}km" else "-"
    val warningValue = if (current.activeWarnings.isEmpty()) "없음" else "${current.activeWarnings.size}개"
    val warningColor = when {
        current.activeWarnings.isEmpty() -> status.ok.content
        current.activeWarnings.maxOf { it.severity } >= 3 -> status.danger.content
        else -> status.warning.content
    }

    SurfaceCard(modifier) {
        SectionTitle("차량 상태") {
            StatusBadge("차량에서 감지", neutralTone(), icon = CarIcons.Car)
        }
        if (carHeight != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                CarTopView(
                    modifier = Modifier.height(carHeight).aspectRatio(CarTopViewAspect),
                    marks = incidentMarks(open),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatRow(energyIcon, current.energyLabel, energyValue, energyColor, energyCaption)
                    StatRow(CarIcons.Gauge, "주행 가능", rangeValue, MaterialTheme.colorScheme.onSurface)
                    StatRow(CarIcons.Warning, "경고등", warningValue, warningColor)
                    StatusChips(current, open, showOkChip = true)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatBlock(current.energyLabel, energyValue, Modifier.weight(1f), icon = energyIcon, caption = energyCaption, valueColor = energyColor)
                StatBlock("주행 가능", rangeValue, Modifier.weight(1f), icon = CarIcons.Gauge)
                StatBlock("경고등", warningValue, Modifier.weight(1f), icon = CarIcons.Warning, valueColor = warningColor)
            }
            StatusChips(current, open, showOkChip = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CarIcons.Clock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Text(
                buildString {
                    append("갱신 ${formatTime(current.capturedAtMillis)}")
                    current.odometerKm?.let { append(" · 누적 ${"%,.0f".format(it)}km") }
                    lastSeen?.let { append(" · 내 마지막 확인 ${formatDateTime(it.capturedAtMillis)}") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 라벨(+보조 설명)은 왼쪽, 값은 오른쪽 끝에 크게 */
@Composable
private fun StatRow(icon: ImageVector, label: String, value: String, valueColor: Color, caption: String? = null) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = secondary, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = secondary)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = secondary)
        }
        Text(value, style = MaterialTheme.typography.headlineLarge, color = valueColor, maxLines = 1)
    }
}

/** 처리 안 된 사고·파손과 켜진 경고등 배지 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips(current: VehicleSnapshot, open: List<Incident>, showOkChip: Boolean) {
    val status = CarRelayTheme.status
    if (current.activeWarnings.isEmpty() && open.isEmpty()) {
        if (showOkChip) StatusBadge("외관 이상 신고 없음", status.ok, icon = CarIcons.CheckCircle)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        open.take(2).forEach { inc ->
            StatusBadge(inc.summary, incidentTone(inc.status), icon = CarIcons.Car)
        }
        current.activeWarnings.sortedByDescending { it.severity }.take(3).forEach { w ->
            StatusBadge(w.label, if (w.severity >= 3) status.danger else status.warning, icon = CarIcons.Warning)
        }
    }
}

/** 처리 필요 = 위험 색, 처리 중 = 주의 색, 처리 완료 = 정상 색 */
@Composable
fun incidentTone(status: IncidentStatus): StatusTone = when (status) {
    IncidentStatus.OPEN -> CarRelayTheme.status.danger
    IncidentStatus.IN_PROGRESS -> CarRelayTheme.status.warning
    IncidentStatus.RESOLVED -> CarRelayTheme.status.ok
}

/** 처리 안 된 기록의 위치 → 차 그림 표시 색. 한 위치에 여러 건이면 더 급한 쪽(처리 필요)을 씁니다. */
@Composable
fun incidentMarks(open: List<Incident>): Map<CarZone, StatusTone> {
    val marks = mutableMapOf<CarZone, StatusTone>()
    open.sortedBy { if (it.status == IncidentStatus.OPEN) 1 else 0 }.forEach { inc ->
        val tone = incidentTone(inc.status)
        inc.zones.forEach { marks[it] = tone }
    }
    return marks
}
