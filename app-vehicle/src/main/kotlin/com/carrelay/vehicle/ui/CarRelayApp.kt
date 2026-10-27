package com.carrelay.vehicle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carrelay.vehicle.ui.components.Tag
import com.carrelay.vehicle.ui.screens.BriefingScreen
import com.carrelay.vehicle.ui.screens.DriverPickerScreen
import com.carrelay.vehicle.ui.screens.DrivingScreen
import com.carrelay.vehicle.ui.screens.HomeScreen
import com.carrelay.vehicle.ui.screens.MemoScreen
import com.carrelay.vehicle.ui.screens.RecordsScreen
import com.carrelay.vehicle.ui.screens.SettingsScreen
import com.carrelay.vehicle.ui.screens.VirtualConsolePanel
import kotlinx.coroutines.delay

@Composable
fun CarRelayApp(vm: CarRelayViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var consoleOpen by rememberSaveable { mutableStateOf(vm.simulatedCar != null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                TopBar(
                    ui = ui,
                    consoleAvailable = vm.simulatedCar != null,
                    consoleOpen = consoleOpen,
                    onToggleConsole = { consoleOpen = !consoleOpen },
                    onNavigate = vm::navigate,
                )
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp)) {
                    if (ui.loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        when (ui.screen) {
                            Screen.PICKER -> DriverPickerScreen(ui, onSelect = vm::selectDriver, onKeepSame = vm::keepSameDriver)
                            Screen.BRIEFING -> BriefingScreen(
                                ui = ui,
                                onAcknowledge = vm::acknowledgeBriefing,
                                onResolve = vm::resolveMemo,
                                onReplay = vm::replayBriefing,
                            )
                            Screen.HOME -> HomeScreen(
                                ui = ui,
                                onLeaveMemo = { vm.navigate(Screen.MEMO) },
                                onChangeDriver = { vm.navigate(Screen.PICKER) },
                                onReplayBriefing = vm::replayBriefing,
                                onHandOver = vm::handOver,
                            )
                            Screen.MEMO -> MemoScreen(
                                ui = ui,
                                onPreset = vm::addMemo,
                                onDone = { vm.navigate(Screen.HOME) },
                                onHandOver = vm::handOver,
                            )
                            Screen.DRIVING -> DrivingScreen(
                                ui = ui,
                                onAnswerCare = vm::answerCareCheckIn,
                                onAcceptShift = vm::acceptShift,
                                onDeclineShift = vm::declineShift,
                            )
                            Screen.RECORDS -> RecordsScreen(ui)
                            Screen.SETTINGS -> SettingsScreen(
                                ui = ui,
                                onCareMode = vm::setCareMode,
                                onDemoFast = vm::setDemoFast,
                                onVoice = vm::setVoiceEnabled,
                                onReset = vm::resetAllData,
                            )
                        }
                    }
                }
                ToastBar(message = ui.toast, onDismiss = vm::clearToast)
            }

            val fake = vm.simulatedCar
            if (consoleOpen && fake != null) {
                VirtualConsolePanel(
                    fake = fake,
                    onAddDrivingTime = vm::debugAddDrivingTime,
                    modifier = Modifier.width(360.dp).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    ui: UiState,
    consoleAvailable: Boolean,
    consoleOpen: Boolean,
    onToggleConsole: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("카릴레이", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        ui.currentDriver?.let { d ->
            Tag(if (ui.sessionConfirmed) "운전자: ${d.name}" else "운전자: ${d.name} (미확인)")
            if (d.careModeEnabled) {
                Tag("케어 모드", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        Spacer(Modifier.weight(1f))
        Tag(if (ui.isSimulated) "데이터: 가상 차량" else "데이터: ${ui.sourceName}")
        if (!ui.drivingUi) {
            TextButton(onClick = { onNavigate(Screen.HOME) }) { Text("홈") }
            TextButton(onClick = { onNavigate(Screen.RECORDS) }) { Text("기록") }
            TextButton(onClick = { onNavigate(Screen.SETTINGS) }) { Text("설정") }
        }
        if (consoleAvailable) {
            TextButton(onClick = onToggleConsole) { Text(if (consoleOpen) "가상 차량 닫기" else "가상 차량") }
        }
    }
}

@Composable
private fun ToastBar(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    LaunchedEffect(message) {
        delay(3_500)
        onDismiss()
    }
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}
