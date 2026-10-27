package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.ScreenTitle
import com.carrelay.vehicle.ui.components.SectionCard
import com.carrelay.vehicle.ui.components.Tag
import com.carrelay.vehicle.ui.formatDateTime
import com.carrelay.vehicle.ui.nameOf

/** 운전자별 기록(F5 기초)과 케어 모드 하루 요약·안부 확인 기록(F6). */
@Composable
fun RecordsScreen(ui: UiState) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle("운전자별 기록", "계기판 주행거리 기준. 선택 없이 출발한 기록은 '미확인'으로 따로 표시해요.")
            val byDriver = ui.sessions.groupBy { it.driverId }
            if (byDriver.isEmpty()) {
                Text(
                    "아직 끝난 운전 기록이 없어요. 운전자를 바꾸거나 '운전 끝내기'를 누르면 기록돼요.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            (ui.drivers + Driver.GUEST).forEach { driver ->
                val sessions = byDriver[driver.id].orEmpty()
                if (sessions.isEmpty()) return@forEach
                val distance = sessions.mapNotNull { it.distanceKm }.sum()
                val duration = sessions.sumOf { ((it.endedAtMillis ?: it.startedAtMillis) - it.startedAtMillis).coerceAtLeast(0L) }
                val unconfirmed = sessions.count { !it.confirmed }
                SectionCard {
                    Text(driver.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "운전 ${sessions.size}회 · ${KoreanText.distance(distance)} · ${KoreanText.duration(duration)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (unconfirmed > 0) Tag("미확인 ${unconfirmed}회 포함")
                }
            }
        }

        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle("케어 모드 하루 요약", "보호자에게는 실시간 위치 대신 이 요약만 보내요.")
            if (ui.careSummaries.isEmpty()) {
                Text("케어 모드를 켠 파티원이 없어요", style = MaterialTheme.typography.bodyLarge)
            }
            ui.careSummaries.forEach { summary ->
                SectionCard { Text(summary.message, style = MaterialTheme.typography.titleMedium) }
            }

            Text("보호자 알림", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (ui.guardianAlerts.isEmpty()) {
                Text("보낸 알림이 없어요", style = MaterialTheme.typography.bodyLarge)
            }
            ui.guardianAlerts.asReversed().take(5).forEach { alert ->
                SectionCard(container = MaterialTheme.colorScheme.errorContainer) {
                    Text(formatDateTime(alert.atMillis), style = MaterialTheme.typography.bodyMedium)
                    Text(alert.message, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text("안부 확인 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            ui.careEvents.asReversed().take(20).forEach { e ->
                Text(
                    "${formatDateTime(e.atMillis)}  ${ui.nameOf(e.driverId)} · ${e.type.label}" +
                        (e.reason?.let { " (${it.label})" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
