package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.carrelay.core.incident.IncidentLog
import com.carrelay.core.model.Driver
import com.carrelay.core.profile.LicenseState
import com.carrelay.vehicle.BuildConfig
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.SectionTitle
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.conditionTone
import com.carrelay.vehicle.ui.components.neutralTone
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens

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
        verticalArrangement = Arrangement.spacedBy(Dimens.gap),
    ) {
        ScreenHeader("설정", "${ui.vehicleName} · 파티 ${ui.drivers.size}명")

        // 가족 프로필: 면허 종류·갱신·경력, 사고·파손 기록
        SurfaceCard(Modifier.fillMaxWidth()) {
            SectionTitle("가족 프로필") {
                StatusBadge("면허 번호는 저장하지 않아요", neutralTone(), icon = CarIcons.Shield)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                ui.drivers.forEach { d -> ProfileCard(d, ui, Modifier.weight(1f)) }
            }
            Text(
                "면허 종류·갱신일·취득 연도는 각자 폰 앱에서 본인이 입력해요. 사고·파손은 차에서 신고한 기록만 모아요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 왼쪽: 가족별 케어 모드 / 오른쪽: 안내·시연·정보. 한 화면에 모두 보이게 2열로 둡니다.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        SurfaceCard(Modifier.weight(1.15f)) {
            SectionTitle("케어 모드")
            Text(
                "운전자 본인의 동의로만 켜고 끌 수 있어요. 보호자에게는 하루 요약만 보내고, 위치는 안부 확인에 계속 응답이 없을 때만 보내요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ui.drivers.forEach { d ->
                val condition = ui.conditions[d.id]
                SettingRow(
                    leading = { Avatar(d.name, ui.avatarTone(d.id), size = 64.dp) },
                    title = d.name,
                    trailingBadge = {
                        if (condition != null) StatusBadge("컨디션 ${condition.level.label}", conditionTone(condition.level))
                    },
                    subtitle = if (d.careModeEnabled) "케어 모드 켜짐 · 보호자 ${d.guardianName ?: "미지정"}" else "케어 모드 꺼짐",
                    checked = d.careModeEnabled,
                    onToggle = { on -> if (on) consentFor = d else onCareMode(d.id, false) },
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            SurfaceCard {
                SectionTitle("안내")
                SettingRow(
                    leading = { IconCircle(CarIcons.Speaker, neutralTone(), size = 56.dp, iconSize = 30.dp) },
                    title = "음성 안내",
                    subtitle = "브리핑·교대·안부 확인을 소리로 읽어 줘요",
                    checked = ui.voiceEnabled,
                    onToggle = onVoice,
                )
            }
            SurfaceCard {
                SectionTitle("시연")
                SettingRow(
                    leading = { IconCircle(CarIcons.Clock, neutralTone(), size = 56.dp, iconSize = 30.dp) },
                    title = "빠른 판단",
                    subtitle = "안부 확인을 2분 대신 15초 정차로 보여 줘요",
                    checked = ui.demoFastThresholds,
                    onToggle = onDemoFast,
                )
            }
            SurfaceCard {
                SectionTitle("정보")
                Text("차량 데이터: ${ui.sourceName}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "앱 버전 ${BuildConfig.VERSION_NAME} · Pleos SDK 연동 ${if (BuildConfig.USE_PLEOS_SDK) "켜짐" else "꺼짐"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryButton("기록 초기화", { confirmReset = true }, Modifier.fillMaxWidth(), height = 88.dp, icon = CarIcons.Records)
            }
        }
        }
    }

    consentFor?.let { d ->
        AlertDialog(
            onDismissRequest = { consentFor = null },
            title = { Text("${d.name}님 케어 모드 켜기", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "${d.name}님 본인이 동의하셨나요?\n\n" +
                        "• 주행 중 장시간 정차·급감속 반복 시 \"괜찮으세요?\"라고 여쭤봐요\n" +
                        "• 계속 응답이 없으면 쉼터를 경유지로 추가하고 ${d.guardianName ?: "보호자"}님께 위치와 상황을 알려요\n" +
                        "• 평소에는 하루 요약만 공유하고, 의료적 판단은 하지 않아요",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCareMode(d.id, true)
                    consentFor = null
                }) { Text("본인이 동의했어요", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { consentFor = null }) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("기록을 모두 지울까요?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("메모, 운전 기록, 케어 기록이 지워지고 시연용 기본 파티로 돌아가요.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    onReset()
                    confirmReset = false
                }) { Text("지우기", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(d: Driver, ui: UiState, modifier: Modifier) {
    val s = CarRelayTheme.status
    val license = ui.licenses[d.id]
    val stats = IncidentLog.statsFor(ui.incidents, d.id)
    SurfaceCard(modifier, container = MaterialTheme.colorScheme.surfaceContainer, border = null, contentPadding = 24.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(d.name, ui.avatarTone(d.id), size = 64.dp)
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    d.licenseClass?.label ?: "면허 정보 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                license?.yearsLicensed?.let { y ->
                    Text(
                        if (y <= 0) "운전 경력 1년 미만" else "운전 경력 ${y}년",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (license?.state) {
                LicenseState.RENEW_SOON -> StatusBadge("면허 ${license?.renewalLabel.orEmpty()}", s.warning, icon = CarIcons.Clock)
                LicenseState.EXPIRED -> StatusBadge("면허 ${license?.renewalLabel.orEmpty()}", s.danger, icon = CarIcons.Warning)
                LicenseState.OK -> StatusBadge("면허 유효", s.ok, icon = CarIcons.Check)
                else -> Unit // 정보 없음은 이름 아래에 이미 표시
            }
            if (license?.isNovice == true) StatusBadge("초보 운전", s.info, icon = CarIcons.Info)
            if (d.careModeEnabled) StatusBadge("케어", s.memo, icon = CarIcons.Shield)
        }
        Text("사고·파손 기록", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (stats.selfReported == 0 && stats.foundAfterMyDrive == 0) {
                "없음"
            } else {
                "직접 신고 ${stats.selfReported}건 · 운전 후 발견 ${stats.foundAfterMyDrive}건"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (stats.open > 0) StatusBadge("처리 완료 전 ${stats.open}건", s.danger)
    }
}

/** 행 전체를 눌러도 켜고 끌 수 있는 설정 줄 (최소 104dp) */
@Composable
private fun SettingRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    trailingBadge: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(20.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                trailingBadge()
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.scale(1.35f).padding(horizontal = 8.dp))
    }
}
