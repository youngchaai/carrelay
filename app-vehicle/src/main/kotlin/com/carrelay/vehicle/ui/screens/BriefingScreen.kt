package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carrelay.core.handover.BriefingItem
import com.carrelay.core.model.Memo
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.BigButton
import com.carrelay.vehicle.ui.components.BigOutlinedButton
import com.carrelay.vehicle.ui.components.ScreenTitle
import com.carrelay.vehicle.ui.components.SectionCard
import com.carrelay.vehicle.ui.components.Tag
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf

private val RESOLUTION_NOTES = listOf("수리했어요", "확인해 보니 문제없어요", "주유·충전했어요", "정비 예약했어요")

/** F3 인수인계 브리핑: 상위 3개 + 차량 상태(왼쪽), 남은 메모와 나머지 정보(오른쪽). */
@Composable
fun BriefingScreen(
    ui: UiState,
    onAcknowledge: () -> Unit,
    onResolve: (memoId: String, note: String) -> Unit,
    onReplay: () -> Unit,
) {
    val b = ui.briefing
    if (b == null) {
        Text("브리핑을 준비하고 있어요", style = MaterialTheme.typography.titleLarge)
        return
    }
    var resolvingMemoId by remember { mutableStateOf<String?>(null) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle(
                title = "${b.driver.name}님, 인수인계 브리핑",
                subtitle = b.lastSeen?.let { "내 마지막 확인 ${formatDateTime(it.capturedAtMillis)} 이후 달라진 점" }
                    ?: "처음 확인하는 차량 상태예요",
            )
            b.topItems.forEach { BriefingRow(it, emphasized = true) }
            VehicleStatusCard(b.current, b.lastSeen)
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("남은 메모 ${b.unresolvedMemos.size}개", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (b.unresolvedMemos.isEmpty()) {
                    Text("해결 안 된 메모가 없어요", style = MaterialTheme.typography.bodyLarge)
                }
                b.unresolvedMemos.forEach { memo ->
                    MemoCard(memo, ui, isNew = !memo.isReadBy(b.driver.id), onResolveClick = { resolvingMemoId = memo.id })
                }
                val rest = b.items.drop(b.topItems.size).filter {
                    it.kind != BriefingItem.Kind.NEW_MEMO && it.kind != BriefingItem.Kind.UNRESOLVED_MEMO
                }
                if (rest.isNotEmpty()) {
                    Text("그 밖의 정보", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    rest.forEach { BriefingRow(it, emphasized = false) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigOutlinedButton("다시 듣기", onReplay, Modifier.weight(1f))
                BigButton("확인했어요", onAcknowledge, Modifier.weight(2f))
            }
        }
    }

    resolvingMemoId?.let { memoId ->
        AlertDialog(
            onDismissRequest = { resolvingMemoId = null },
            title = { Text("어떻게 해결했나요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("메모를 해결해도 차량 경고등 상태는 따로 계속 표시돼요.", style = MaterialTheme.typography.bodyMedium)
                    RESOLUTION_NOTES.forEach { note ->
                        BigOutlinedButton(note, { onResolve(memoId, note); resolvingMemoId = null }, Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = { TextButton(onClick = { resolvingMemoId = null }) { Text("닫기") } },
        )
    }
}

@Composable
private fun BriefingRow(item: BriefingItem, emphasized: Boolean) {
    val dot: Color = when (item.kind) {
        BriefingItem.Kind.NEW_WARNING, BriefingItem.Kind.ONGOING_WARNING -> MaterialTheme.colorScheme.error
        BriefingItem.Kind.ENERGY_LOW, BriefingItem.Kind.STALE_DATA -> MaterialTheme.colorScheme.tertiary
        BriefingItem.Kind.NEW_MEMO, BriefingItem.Kind.UNRESOLVED_MEMO, BriefingItem.Kind.RESOLVED_MEMO -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    SectionCard(container = if (emphasized) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(14.dp).background(dot, CircleShape))
            Text(
                item.message,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
        if (item.fromPerson) Tag("사람이 남긴 정보")
    }
}

@Composable
private fun MemoCard(memo: Memo, ui: UiState, isNew: Boolean, onResolveClick: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isNew) Tag("새 메모", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            Text(
                "${ui.nameOf(memo.authorId)} · ${formatDateTime(memo.createdAtMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(memo.text, style = MaterialTheme.typography.titleMedium)
        BigOutlinedButton("해결했어요", onResolveClick, Modifier.fillMaxWidth().height(64.dp))
        if (memo.readBy.isNotEmpty()) {
            Text(
                "읽음: " + memo.readBy.keys.joinToString { ui.nameOf(it) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
