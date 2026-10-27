package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.MemoPreset
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.BigButton
import com.carrelay.vehicle.ui.components.BigOutlinedButton
import com.carrelay.vehicle.ui.components.ScreenTitle

/** 주차 후 다음 운전자에게 남길 메모. 차량에서는 키보드 없이 프리셋 버튼만 씁니다. */
@Composable
fun MemoScreen(
    ui: UiState,
    onPreset: (MemoPreset) -> Unit,
    onDone: () -> Unit,
    onHandOver: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenTitle(
            title = if (ui.parkedPrompt) "주차하셨네요. 남길 메모가 있나요?" else "다음 운전자에게 메모 남기기",
            subtitle = "누르면 바로 저장돼요. 해결될 때까지 모든 파티원에게 보여요.",
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(260.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(MemoPreset.entries) { preset ->
                BigOutlinedButton(preset.text, { onPreset(preset) }, Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigOutlinedButton("닫기", onDone, Modifier.weight(1f))
            BigButton("운전 끝내기 (넘겨주기)", onHandOver, Modifier.weight(1f), enabled = ui.currentDriver != null)
        }
    }
}
