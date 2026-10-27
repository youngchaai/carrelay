package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.BigButton
import com.carrelay.vehicle.ui.components.BigOutlinedButton
import com.carrelay.vehicle.ui.components.ScreenTitle
import com.carrelay.vehicle.ui.components.SectionCard
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.nameOf

/** 주차 중 기본 화면. */
@Composable
fun HomeScreen(
    ui: UiState,
    onLeaveMemo: () -> Unit,
    onChangeDriver: () -> Unit,
    onReplayBriefing: () -> Unit,
    onHandOver: () -> Unit,
) {
    val driver = ui.currentDriver
    val unresolved = ui.memos.filter { !it.isResolved }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenTitle(
            title = if (driver != null) "${driver.name}님, 주차 중이에요" else "운전자를 선택해 주세요",
            subtitle = if (driver != null && !ui.sessionConfirmed) "선택 없이 출발해 자동으로 지정된 기록이에요 (미확인)" else null,
        )

        ui.snapshot?.let { VehicleStatusCard(it, lastSeen = null) }

        SectionCard {
            Text("해결 안 된 메모 ${unresolved.size}개", style = MaterialTheme.typography.titleLarge)
            if (unresolved.isEmpty()) {
                Text("남아 있는 메모가 없어요", style = MaterialTheme.typography.bodyLarge)
            }
            unresolved.take(3).forEach { memo ->
                Text("· ${memo.text}  (${ui.nameOf(memo.authorId)})", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton("메모 남기기", onLeaveMemo, Modifier.weight(1f))
            BigOutlinedButton("브리핑 다시 보기", onReplayBriefing, Modifier.weight(1f), enabled = driver != null)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigOutlinedButton(if (driver != null) "운전자 바꾸기" else "운전자 선택", onChangeDriver, Modifier.weight(1f))
            BigOutlinedButton("운전 끝내기 (넘겨주기)", onHandOver, Modifier.weight(1f), enabled = driver != null)
        }
    }
}
