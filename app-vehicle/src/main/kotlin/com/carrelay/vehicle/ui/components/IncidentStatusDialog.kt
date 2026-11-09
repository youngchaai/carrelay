package com.carrelay.vehicle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
                        "직전 운전: ${nameOf(prev)}" + (incident.previousParkedAtMillis?.let { " (${formatDateTime(it)} 주차)" } ?: "") + ". 함께 확인해 주세요",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // 택배 조회처럼: 신고 → 처리 중 → 처리 완료 중 어디까지 왔는지
                IncidentSteps(incident.status, Modifier.fillMaxWidth().padding(vertical = 8.dp))
                incident.statusNote.takeIf { it.isNotBlank() }?.let {
                    Text("메모: $it", style = MaterialTheme.typography.bodyLarge)
                }
                if (incident.status != IncidentStatus.IN_PROGRESS) {
                    SecondaryButton("수리·보험 맡겼어요", { onUpdate(IncidentStatus.IN_PROGRESS, "수리·보험 접수") }, Modifier.fillMaxWidth(), height = 96.dp, icon = CarIcons.Clock)
                }
                PrimaryButton("다 고쳤어요", { onUpdate(IncidentStatus.RESOLVED, "처리 완료") }, Modifier.fillMaxWidth(), height = 96.dp, icon = CarIcons.CheckCircle)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** 사고·파손 처리 단계. [compact] 면 점만(목록 한 줄용). */
@Composable
fun IncidentSteps(status: IncidentStatus, modifier: Modifier = Modifier, compact: Boolean = false) {
    StepTrack(
        steps = listOf("신고", "처리 중", "처리 완료"),
        current = status.ordinal,
        finished = status == IncidentStatus.RESOLVED,
        color = incidentTone(status).content,
        modifier = modifier,
        dot = if (compact) 18.dp else 40.dp,
        labelStyle = if (compact) null else MaterialTheme.typography.titleMedium,
    )
}
