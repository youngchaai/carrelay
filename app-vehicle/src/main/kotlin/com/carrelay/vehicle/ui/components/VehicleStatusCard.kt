package com.carrelay.vehicle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.VehicleSnapshot
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.formatTime

/**
 * 현재 차량 상태 + (있으면) 마지막 확인 시점 값. 값마다 관측 시각을 함께 보여 줍니다.
 */
@Composable
fun VehicleStatusCard(current: VehicleSnapshot, lastSeen: VehicleSnapshot?, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        Text("차량 상태", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            val energy = current.energyPercent
            LabeledValue(
                label = current.energyLabel,
                value = energy?.let { "$it%" } ?: "정보 없음",
                modifier = Modifier.weight(1f),
            )
            LabeledValue(
                label = "주행 가능",
                value = current.rangeKm?.let { "${it}km" } ?: "-",
                modifier = Modifier.weight(1f),
            )
            LabeledValue(
                label = "경고등",
                value = if (current.activeWarnings.isEmpty()) "없음" else "${current.activeWarnings.size}개",
                modifier = Modifier.weight(1f),
            )
        }
        if (current.activeWarnings.isNotEmpty()) {
            Text(
                current.activeWarnings.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        current.odometerKm?.let {
            Text("누적 주행거리 ${KoreanText.distance(it)}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "갱신 ${formatTime(current.capturedAtMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lastSeen != null) {
            val before = lastSeen.energyPercent?.let { "${current.energyLabel} $it%" } ?: "기록 없음"
            Text(
                "내 마지막 확인 (${formatDateTime(lastSeen.capturedAtMillis)}): $before",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
