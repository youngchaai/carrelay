package com.carrelay.vehicle.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carrelay.core.model.IncidentSource
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.QuietButton
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.VoiceMemoOverlay
import com.carrelay.vehicle.ui.screens.BriefingScreen
import com.carrelay.vehicle.ui.screens.DriverPickerScreen
import com.carrelay.vehicle.ui.screens.DrivingScreen
import com.carrelay.vehicle.ui.screens.HomeScreen
import com.carrelay.vehicle.ui.screens.MemoScreen
import com.carrelay.vehicle.ui.screens.RecordsScreen
import com.carrelay.vehicle.ui.screens.ReportScreen
import com.carrelay.vehicle.ui.screens.SettingsScreen
import com.carrelay.vehicle.ui.screens.VirtualConsolePanel
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CarRelayApp(vm: CarRelayViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var consoleOpen by rememberSaveable { mutableStateOf(vm.simulatedCar != null) }

    // 큰 글씨 모드인 사람이 운전하면 앱 화면 전체(글씨·버튼·간격)를 같은 비율로 키웁니다.
    // 비율을 함께 키워야 글씨만 커져 버튼 밖으로 넘치는 일이 없습니다. (가상 차량 콘솔은 그대로)
    val base = LocalDensity.current
    val largeText = ui.currentDriver?.largeText == true
    val appDensity = if (largeText) Density(base.density * LARGE_TEXT_SCALE, base.fontScale) else base

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            CompositionLocalProvider(LocalDensity provides appDensity) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    ui = ui,
                    consoleAvailable = vm.simulatedCar != null,
                    consoleOpen = consoleOpen,
                    onToggleConsole = { consoleOpen = !consoleOpen },
                    onNavigate = vm::navigate,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = Dimens.page, end = Dimens.page, top = 8.dp, bottom = Dimens.gap),
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        // 화면이 바뀔 때만 부드럽게 겹쳐 바꿉니다. 나가는 화면은 바뀌기 직전 상태로 그립니다.
                        AnimatedContent(
                            targetState = ui.screen,
                            transitionSpec = { fadeIn(tween(260, delayMillis = 60)) togetherWith fadeOut(tween(180)) },
                            label = "screen",
                            modifier = Modifier.fillMaxSize(),
                        ) { screen ->
                            val settling = transition.currentState != EnterExitState.Visible ||
                                transition.targetState != EnterExitState.Visible
                            Box(Modifier.fillMaxSize().blockTouches(settling)) {
                                ScreenContent(latestWhile(ui.screen == screen, ui), vm)
                            }
                        }
                    }
                    ToastBar(message = ui.toast, onDismiss = vm::clearToast, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
            // 말로 남기는 메모는 위쪽 메뉴까지 덮어, 듣는 동안 다른 화면으로 넘어가지 않게 합니다.
            ui.voiceMemo?.let { voice ->
                VoiceMemoOverlay(
                    ui = ui,
                    voice = voice,
                    onStop = vm::cancelVoiceMemo,
                    onRetry = vm::startVoiceMemo,
                    onSave = vm::saveVoiceMemo,
                    onPickExample = vm::pickVoiceExample,
                    onClose = vm::cancelVoiceMemo,
                )
            }
            }
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

/** 큰 글씨 모드 비율 (본문 28sp → 약 34sp, 버튼 104dp → 약 125dp) */
private const val LARGE_TEXT_SCALE = 1.2f

@Composable
private fun ScreenContent(ui: UiState, vm: CarRelayViewModel) {
    when (ui.screen) {
        Screen.PICKER -> DriverPickerScreen(ui, onSelect = vm::selectDriver, onKeepSame = vm::keepSameDriver)
        Screen.BRIEFING -> BriefingScreen(
            ui = ui,
            onAcknowledge = vm::acknowledgeBriefing,
            onResolve = vm::resolveMemo,
            onReplay = vm::replayBriefing,
            onReportIssue = { vm.startReport(IncidentSource.AT_PICKUP) },
            onUpdateIncident = vm::updateIncidentStatus,
            onGreetingDone = vm::greetingDone,
        )
        Screen.HOME -> HomeScreen(
            ui = ui,
            onLeaveMemo = { vm.navigate(Screen.MEMO) },
            onChangeDriver = { vm.navigate(Screen.PICKER) },
            onReplayBriefing = vm::replayBriefing,
            onHandOver = vm::handOver,
            onReport = { vm.startReport(IncidentSource.AFTER_DRIVE) },
            onResolveMemo = vm::resolveMemo,
            onUpdateIncident = vm::updateIncidentStatus,
        )
        Screen.MEMO -> MemoScreen(
            ui = ui,
            onPreset = vm::addMemo,
            onVoice = vm::startVoiceMemo,
            onDone = { vm.navigate(Screen.HOME) },
            onHandOver = vm::handOver,
            onReport = { vm.startReport(IncidentSource.AFTER_DRIVE) },
            onAfterDriveOk = vm::markAfterDriveOk,
        )
        Screen.REPORT -> ReportScreen(
            ui = ui,
            onSubmit = vm::submitIncident,
            onCancel = vm::cancelReport,
        )
        Screen.DRIVING -> DrivingScreen(
            ui = ui,
            onAnswerCare = vm::answerCareCheckIn,
            onAcceptShift = vm::acceptShift,
            onDeclineShift = vm::declineShift,
            onVoiceMemo = vm::startVoiceMemo,
        )
        Screen.RECORDS -> RecordsScreen(ui, onUpdateIncident = vm::updateIncidentStatus)
        Screen.SETTINGS -> SettingsScreen(
            ui = ui,
            onCareMode = vm::setCareMode,
            onLargeText = vm::setLargeText,
            onDemoFast = vm::setDemoFast,
            onVoice = vm::setVoiceEnabled,
            onReset = vm::resetAllData,
        )
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
    // 큰 글씨 + 가상 차량 콘솔처럼 폭이 좁으면 앱 이름을 접고 케어 모드 표시를 아이콘만 남겨 메뉴가 잘리지 않게 합니다.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 1280.dp
    Row(
        Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = Dimens.page, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CarIcons.Handover, contentDescription = "카릴레이", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            if (!compact) Text("카릴레이", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        ui.currentDriver?.let { d ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(36.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(d.name, ui.avatarTone(d.id), size = 52.dp)
                Text(d.name, style = MaterialTheme.typography.titleMedium)
                if (!ui.sessionConfirmed) StatusBadge("확인 필요", CarRelayTheme.status.warning)
            }
            if (d.careModeEnabled) {
                if (compact) IconCircle(CarIcons.Shield, CarRelayTheme.status.memo, size = 44.dp, iconSize = 26.dp)
                else StatusBadge("케어 모드", CarRelayTheme.status.memo, icon = CarIcons.Shield)
            }
        }
        Spacer(Modifier.weight(1f))
        if (!ui.drivingUi) {
            NavItem("홈", CarIcons.Home, selected = ui.screen == Screen.HOME, compact) { onNavigate(Screen.HOME) }
            NavItem("기록", CarIcons.Records, selected = ui.screen == Screen.RECORDS, compact) { onNavigate(Screen.RECORDS) }
            NavItem("설정", CarIcons.Settings, selected = ui.screen == Screen.SETTINGS, compact) { onNavigate(Screen.SETTINGS) }
        }
        if (consoleAvailable) {
            QuietButton(if (consoleOpen) "콘솔 닫기" else "가상 차량", onToggleConsole, icon = CarIcons.Car)
        }
    }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, compact: Boolean = false, onClick: () -> Unit) {
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (compact) 14.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(32.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun ToastBar(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // 사라지는 동안에도 글자가 남아 있도록 마지막 메시지를 기억합니다.
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        if (message != null) {
            shown = message
            delay(3_500)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 2 },
        exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 2 },
        modifier = modifier,
    ) {
        Row(
            Modifier
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(CarIcons.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(32.dp))
            Text(message ?: shown, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}
