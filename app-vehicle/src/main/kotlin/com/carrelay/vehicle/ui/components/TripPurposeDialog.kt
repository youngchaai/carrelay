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
    /** 이 사람이 지난번에 고른 목적. 아직 고른 게 없으면 이것을 진하게 보여 줍니다(그래도 눌러야 적힘). */
    suggested: TripPurpose? = null,
) {
    // 순서는 그대로 두고 진하게만 표시합니다(늘 같은 자리에 같은 버튼).
    val highlight = current ?: suggested
    val fromLast = current == null && suggested != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (fromLast) "지난번에 고른 목적을 진하게 표시했어요. 다르면 다른 것을 눌러 주세요."
                    else "고르면 운행기록부에 자동으로 적혀요. 주행 거리는 계기판 값으로 채워요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TripPurpose.entries.forEach { p ->
                    val base = if (p.isBusinessUse) p.label else "${p.label} (업무 외)"
                    val label = if (fromLast && p == suggested) "$base · 지난번" else base
                    if (p == highlight) {
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
