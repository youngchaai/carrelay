package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.BigButton
import com.carrelay.vehicle.ui.components.ScreenTitle
import com.carrelay.vehicle.ui.components.SectionCard
import com.carrelay.vehicle.ui.components.Tag

/** F2 운전자 선택: 프로필을 터치 한 번으로 고릅니다. 기어 P에서만 선택할 수 있습니다. */
@Composable
fun DriverPickerScreen(ui: UiState, onSelect: (String) -> Unit, onKeepSame: () -> Unit) {
    val drivers = ui.drivers + Driver.GUEST
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenTitle(
            title = if (ui.askingDriverChange) "운전자 바뀌셨나요?" else "누가 운전하시나요?",
            subtitle = when {
                !ui.canSelectDriver -> "기어 P에서 선택할 수 있어요"
                ui.askingDriverChange -> "운전석 문이 열렸다 닫혔어요. 짐만 꺼냈다면 '그대로'를 눌러 주세요."
                else -> "선택하면 지난 운전 이후 달라진 점과 남은 메모를 알려 드려요"
            },
        )

        ui.recommendation?.let { rec ->
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("컨디션 참고", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rec.message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        val current = ui.currentDriver
        if (ui.askingDriverChange && current != null) {
            BigButton("그대로 ${current.name}님이 운전해요", onKeepSame, Modifier.fillMaxWidth())
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(drivers, key = { it.id }) { driver ->
                DriverTile(driver, ui, onSelect)
            }
        }
    }
}

@Composable
private fun DriverTile(driver: Driver, ui: UiState, onSelect: (String) -> Unit) {
    val condition = ui.conditions[driver.id]
    val isLast = ui.lastDriverId == driver.id
    val recommended = ui.recommendation?.driverId == driver.id
    val container = if (recommended) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .alpha(if (ui.canSelectDriver) 1f else 0.5f)
            .clickable(enabled = ui.canSelectDriver) { onSelect(driver.id) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(driver.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (condition != null) {
                Text("컨디션 ${condition.level.label}", style = MaterialTheme.typography.bodyLarge)
            } else if (!driver.isGuest) {
                Text("컨디션 정보 없음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLast) Tag("지난 운전자")
                if (recommended) Tag("추천")
                if (driver.careModeEnabled) {
                    Tag("케어", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}
