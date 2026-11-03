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
import com.carrelay.core.model.TripPurpose
import com.carrelay.vehicle.ui.theme.CarIcons

fun TripPurpose.icon() = when (this) {
    TripPurpose.COMMUTE -> CarIcons.Home
    TripPurpose.BUSINESS -> CarIcons.Route
    TripPurpose.PERSONAL -> CarIcons.Person
}

/**
 * 회사 차: 운행 목적 고르기. 키보드 없이 한 번 누르면 운행기록부의 '업무용 사용거리' 칸이 정해집니다.
 * (국세청 운행기록부 서식: 업무용 사용거리 = 출퇴근용 + 일반 업무용)
 */
@Composable
fun TripPurposeDialog(
    current: TripPurpose?,
    onPick: (TripPurpose) -> Unit,
    onDismiss: () -> Unit,
    title: String = "어떤 일로 운행하세요?",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "고르면 운행기록부에 자동으로 적혀요. 주행 거리는 계기판 값으로 채워요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TripPurpose.entries.forEach { p ->
                    val label = if (p.isBusinessUse) p.label else "${p.label} (업무 외)"
                    if (p == current) {
                        PrimaryButton(label, { onPick(p) }, Modifier.fillMaxWidth(), height = 96.dp, icon = p.icon())
                    } else {
                        SecondaryButton(label, { onPick(p) }, Modifier.fillMaxWidth(), height = 96.dp, icon = p.icon())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", style = MaterialTheme.typography.labelLarge) } },
    )
}
