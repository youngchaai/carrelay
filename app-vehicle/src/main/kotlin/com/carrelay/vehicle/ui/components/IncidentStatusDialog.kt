package com.carrelay.vehicle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Incident
import com.carrelay.core.model.IncidentStatus
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.theme.CarIcons

/** 사고·파손 처리 상태 바꾸기 (처리 중 / 처리 완료) */
@Composable
fun IncidentStatusDialog(
    incident: Incident,
    nameOf: (String) -> String,
    onDismiss: () -> Unit,
    onUpdate: (IncidentStatus, String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(incident.summary, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${nameOf(incident.reporterId)} · ${incident.source.label} · ${formatDateTime(incident.createdAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                incident.previousDriverId?.let { prev ->
                    Text(
                        "직전 운전: ${nameOf(prev)}" + (incident.previousParkedAtMillis?.let { " · ${formatDateTime(it)} 주차" } ?: "") + " — 확인이 필요해요",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text("지금 상태: ${incident.status.label}" + incident.statusNote.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                SecondaryButton("보험·정비 접수했어요", { onUpdate(IncidentStatus.IN_PROGRESS, "보험·정비 접수") }, Modifier.fillMaxWidth(), height = 88.dp, icon = CarIcons.Clock)
                PrimaryButton("처리 완료", { onUpdate(IncidentStatus.RESOLVED, "처리 완료") }, Modifier.fillMaxWidth(), height = 88.dp, icon = CarIcons.CheckCircle)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", style = MaterialTheme.typography.labelLarge) } },
    )
}
