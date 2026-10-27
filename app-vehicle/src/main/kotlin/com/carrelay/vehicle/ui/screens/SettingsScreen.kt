package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.vehicle.BuildConfig
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.components.BigOutlinedButton
import com.carrelay.vehicle.ui.components.ScreenTitle
import com.carrelay.vehicle.ui.components.SectionCard

@Composable
fun SettingsScreen(
    ui: UiState,
    onCareMode: (driverId: String, enabled: Boolean) -> Unit,
    onDemoFast: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var consentFor by remember { mutableStateOf<Driver?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("설정")

        SectionCard {
            Text("파티원과 케어 모드", style = MaterialTheme.typography.titleLarge)
            Text(
                "케어 모드는 운전자 본인의 동의로만 켜고 끌 수 있어요. 보호자에게는 하루 요약만 보내고, " +
                    "위치는 안부 확인에 계속 응답이 없을 때만 보내요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ui.drivers.forEach { d ->
                val condition = ui.conditions[d.id]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.name, style = MaterialTheme.typography.titleMedium)
                        // 차량 화면은 모두가 보므로 3단계만 보여 줍니다(수면 시간 등 원래 수치는 본인 폰에서만).
                        Text(
                            condition?.let { c -> "컨디션 ${c.level.label}" } ?: "컨디션 정보 없음",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("케어 모드", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = d.careModeEnabled,
                        onCheckedChange = { on ->
                            if (on) {
                                consentFor = d
                            } else {
                                onCareMode(d.id, false)
                            }
                        },
                    )
                }
            }
        }

        SectionCard {
            Text("시연", style = MaterialTheme.typography.titleLarge)
            SettingSwitch(
                title = "빠른 판단",
                description = "케어 모드 안부 확인을 2분 대신 15초 정차로 시연해요",
                checked = ui.demoFastThresholds,
                onChange = onDemoFast,
            )
            SettingSwitch(
                title = "음성 안내",
                description = "브리핑·교대·안부 확인을 소리로 읽어 줘요",
                checked = ui.voiceEnabled,
                onChange = onVoice,
            )
        }

        SectionCard {
            Text("정보", style = MaterialTheme.typography.titleLarge)
            Text("차량 데이터: ${ui.sourceName}", style = MaterialTheme.typography.bodyLarge)
            Text("앱 버전 ${BuildConfig.VERSION_NAME} · Pleos SDK 연동 ${if (BuildConfig.USE_PLEOS_SDK) "켜짐" else "꺼짐"}", style = MaterialTheme.typography.bodyLarge)
            BigOutlinedButton("기록 초기화", { confirmReset = true }, Modifier.fillMaxWidth())
        }
    }

    consentFor?.let { d ->
        AlertDialog(
            onDismissRequest = { consentFor = null },
            title = { Text("${d.name}님 케어 모드 켜기") },
            text = {
                Text(
                    "${d.name}님 본인이 동의하셨나요?\n\n" +
                        "• 주행 중 장시간 정차·급감속 반복 시 \"괜찮으세요?\"라고 여쭤봐요\n" +
                        "• 계속 응답이 없으면 쉼터를 경유지로 추가하고 ${d.guardianName ?: "보호자"}님께 위치와 상황을 알려요\n" +
                        "• 평소에는 하루 요약만 공유하고, 의료적 판단은 하지 않아요",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCareMode(d.id, true)
                    consentFor = null
                }) { Text("본인이 동의했어요") }
            },
            dismissButton = { TextButton(onClick = { consentFor = null }) { Text("취소") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("기록을 모두 지울까요?") },
            text = { Text("메모, 운전 기록, 케어 기록이 지워지고 시연용 기본 파티로 돌아가요.") },
            confirmButton = {
                TextButton(onClick = {
                    onReset()
                    confirmReset = false
                }) { Text("지우기") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
