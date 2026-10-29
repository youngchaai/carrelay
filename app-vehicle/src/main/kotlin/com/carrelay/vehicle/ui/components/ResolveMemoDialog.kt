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

private val RESOLUTION_NOTES = listOf("고쳤어요", "확인해 보니 괜찮아요", "주유·충전했어요", "정비 예약했어요")

/** 메모 해결: 어떻게 해결했는지 큰 버튼 하나로 고릅니다. */
@Composable
fun ResolveMemoDialog(memoText: String, onDismiss: () -> Unit, onResolve: (note: String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(memoText, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("어떻게 해결했나요?", style = MaterialTheme.typography.bodyLarge)
                RESOLUTION_NOTES.forEach { note ->
                    SecondaryButton(note, { onResolve(note) }, Modifier.fillMaxWidth(), height = 88.dp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", style = MaterialTheme.typography.labelLarge) } },
    )
}
