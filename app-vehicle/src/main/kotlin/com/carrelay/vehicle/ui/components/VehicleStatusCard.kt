package com.carrelay.vehicle.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.carrelay.core.handover.HandoverBriefingBuilder
import com.carrelay.core.model.CarZone
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentStatus
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * 차 상태를 한눈에: 위에서 본 차 그림(사고·파손 위치 표시) + 꼭 필요한 숫자 두 개(연료, 갈 수 있는 거리).
 * 경고등은 켜져 있을 때만 한 줄로 보여 줍니다. 자세한 변화는 브리핑 문장으로 말합니다.
 * 카드 높이가 정해져 있어야(Row 안에서 fillMaxHeight 등) 차 그림이 남는 높이를 채웁니다.
 */
@Composable
fun VehicleStatusCard(
    current: VehicleSnapshot,
    modifier: Modifier = Modifier,
    incidents: List<Incident> = emptyList(),
    /** 차 그림에만 붙일 수식어 (예: 환영 장면에서 이어지는 공유 전환) */
    carModifier: Modifier = Modifier,
) {
    val status = CarRelayTheme.status
    val energy = current.energyPercent
    val low = energy != null && energy <= HandoverBriefingBuilder.LOW_ENERGY_PERCENT
    val shownEnergy by animateIntAsState(energy ?: 0, tween(700), label = "energy")
    val shownRange by animateIntAsState(current.rangeKm ?: 0, tween(700), label = "range")
    val warnings = current.activeWarnings.sortedByDescending { it.severity }

    SurfaceCard(modifier) {
        SectionTitle("차 상태")
        CarTopView(
            modifier = Modifier.weight(1f).aspectRatio(CarTopViewAspect).align(Alignment.CenterHorizontally).then(carModifier),
            marks = incidentMarks(incidents.filter { it.isOpen }),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
            BigNumber(
                label = current.energyLabel,
                value = if (energy != null) "$shownEnergy%" else "모름",
                color = if (low) status.warning.content else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                label = "갈 수 있는 거리",
                value = if (current.rangeKm != null) "${shownRange}km" else "모름",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.3f),
            )
        }
        if (warnings.isNotEmpty()) {
            val first = warnings.first()
            Text(
                "${first.label} 켜짐" + if (warnings.size > 1) " 외 ${warnings.size - 1}개" else "",
                style = MaterialTheme.typography.titleMedium,
                color = if (first.severity >= 3) status.danger.content else status.warning.content,
            )
        }
    }
}

@Composable
private fun BigNumber(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.displaySmall, color = color, maxLines = 1)
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
