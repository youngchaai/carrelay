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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
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
import com.carrelay.core.model.CarUsage
import com.carrelay.core.model.Driver
import com.carrelay.core.profile.LicenseState
import com.carrelay.vehicle.BuildConfig
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.QuietButton
import com.carrelay.vehicle.ui.isCompany
import androidx.compose.foundation.layout.Spacer
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
    onLargeText: (driverId: String, enabled: Boolean) -> Unit,
    onDemoFast: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    onReset: () -> Unit,
    onUsage: (CarUsage) -> Unit = {},
    /** null = 새 사람 추가 */
    onEditDriver: (String?) -> Unit = {},
) {
    var consentFor by remember { mutableStateOf<Driver?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var usageTo by remember { mutableStateOf<CarUsage?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.gap),
    ) {
        ScreenHeader("설정", "${ui.vehicleName} · ${ui.usage.label} · ${if (ui.isCompany) "직원" else "파티"} ${ui.drivers.size}명")

        // 가족(직원) 프로필: 면허 종류·갱신·경력, 사고·파손 기록
        SurfaceCard(Modifier.fillMaxWidth()) {
            SectionTitle(if (ui.isCompany) "직원 프로필 · ${ui.drivers.size}명" else "가족 프로필 · ${ui.drivers.size}명") {
                StatusBadge("면허 번호는 저장하지 않아요", neutralTone(), icon = CarIcons.Shield)
                Spacer(Modifier.size(Dimens.gapS))
                QuietButton(if (ui.isCompany) "직원 추가" else "가족 추가", { onEditDriver(null) }, icon = CarIcons.PersonAdd)
            }
            // 두 명씩 두 줄: 한 칸이 넓어야 글자가 중간에서 끊기지 않습니다.
            ui.drivers.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                    pair.forEach { d -> ProfileCard(d, ui, Modifier.weight(1f), onLargeText = { on -> onLargeText(d.id, on) }, onEdit = { onEditDriver(d.id) }) }
                    if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
            Text(
                if (ui.isCompany) "면허 정보는 각자 폰 앱에서 본인이 입력해요. 관리자는 면허 갱신이 가까운 직원만 알 수 있어요."
                else "면허 정보는 각자 폰 앱에서 본인이 입력해요. 컨디션은 워치 수면 요약을 바탕으로 한 참고 정보이고, 가족에게는 3단계(좋음·보통·휴식 필요)만 보여요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 왼쪽: 가족별 케어 모드(회사 차는 회사 차 안내) / 오른쪽: 차량 용도·안내·시연·정보. 한 화면에 모두 보이게 2열로 둡니다.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        if (ui.isCompany) {
            CompanyModeCard(Modifier.weight(1.15f))
        } else SurfaceCard(Modifier.weight(1.15f)) {
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
                SectionTitle("차량 용도")
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                    CarUsage.entries.forEach { u ->
                        val selected = ui.usage == u
                        val icon = if (u == CarUsage.COMPANY) CarIcons.Route else CarIcons.Home
                        if (selected) {
                            PrimaryButton(u.label, {}, Modifier.weight(1f), height = 88.dp, icon = icon)
                        } else {
                            SecondaryButton(u.label, { usageTo = u }, Modifier.weight(1f), height = 88.dp, icon = icon)
                        }
                    }
                }
                Text(
                    if (ui.isCompany) "회사 차: 운행 목적을 받아 운행기록부를 자동으로 적어요"
                    else "가족 차: 케어 모드와 컨디션 추천을 써요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

    usageTo?.let { u ->
        AlertDialog(
            onDismissRequest = { usageTo = null },
            title = { Text("${u.label}로 바꿀까요?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    if (u == CarUsage.COMPANY) {
                        "함께 쓰는 사람이 직원으로 바뀌어요(시연용 직원 4명).\n\n" +
                            "• 출발 전에 운행 목적(출퇴근·일반 업무·개인 용무)을 한 번 눌러요\n" +
                            "• 운행기록부가 계기판 거리로 자동으로 채워져요\n" +
                            "• 케어 모드·컨디션 추천은 쓰지 않아요\n\n지금 기록은 지워져요."
                    } else {
                        "함께 쓰는 사람이 가족으로 바뀌어요(시연용 가족 4명). 지금 기록은 지워져요."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUsage(u)
                    usageTo = null
                }) { Text("바꾸기", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { usageTo = null }) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
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

/** 회사 차에서는 케어 모드 대신: 회사 차에서 달라지는 점 */
@Composable
private fun CompanyModeCard(modifier: Modifier) {
    val s = CarRelayTheme.status
    SurfaceCard(modifier) {
        SectionTitle("회사 차에서는")
        listOf(
            Triple(CarIcons.Route, s.info, "출발 전에 운행 목적을 한 번 눌러요. 출퇴근 · 일반 업무 · 개인 용무"),
            Triple(CarIcons.Records, s.ok, "운행기록부가 자동으로 채워져요. 사용자·주행 전후 계기판 거리·업무용 거리"),
            Triple(CarIcons.Warning, s.warning, "사고·파손 신고는 관리자에게 보고되고, 처리될 때까지 모든 직원에게 보여요"),
            Triple(CarIcons.Shield, s.memo, "케어 모드와 컨디션 추천은 쓰지 않아요. 직원 감시로 오해되지 않게요"),
        ).forEach { (icon, tone, text) ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 88.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                IconCircle(icon, tone, size = 56.dp, iconSize = 30.dp)
                Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(d: Driver, ui: UiState, modifier: Modifier, onLargeText: (Boolean) -> Unit, onEdit: () -> Unit) {
    val s = CarRelayTheme.status
    val license = ui.licenses[d.id]
    val stats = IncidentLog.statsFor(ui.incidents, d.id)
    SurfaceCard(modifier, container = MaterialTheme.colorScheme.surfaceContainer, border = null, contentPadding = 24.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(d.name, ui.avatarTone(d.id), size = 64.dp)
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        d.department?.let { append(it).append(" · ") }
                        append(d.licenseClass?.label ?: "면허 정보 없음")
                        license?.yearsLicensed?.let { y -> append(if (y <= 0) " · 운전 1년 미만" else " · 운전 ${y}년") }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QuietButton("편집", onEdit, icon = CarIcons.Edit)
        }
        // 알아야 할 것만: 갱신이 가까우면 갱신, 초보면 초보. 문제없으면 아무것도 붙이지 않습니다.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (license?.state) {
                LicenseState.RENEW_SOON -> StatusBadge("면허 갱신 ${license?.daysLeft ?: 0}일 남음", s.warning, icon = CarIcons.Clock)
                LicenseState.EXPIRED -> StatusBadge("면허 갱신 기한 지남", s.danger, icon = CarIcons.Warning)
                else -> Unit
            }
            if (license?.isNovice == true) StatusBadge("초보 운전", s.info, icon = CarIcons.Info)
        }
        val total = stats.selfReported + stats.foundAfterMyDrive
        Text(
            if (total == 0) "사고·파손 없음" else "사고·파손 ${total}건" + if (stats.open > 0) " · 아직 안 끝난 것 ${stats.open}건" else "",
            style = MaterialTheme.typography.bodyLarge,
            color = if (stats.open > 0) s.danger.content else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 큰 글씨: 필요한 사람이 고르면 그 사람이 운전할 때만 차 화면이 커집니다.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .toggleable(value = d.largeText, role = Role.Switch, onValueChange = onLargeText)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.material3.Icon(
                CarIcons.TextSize, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("큰 글씨", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (d.largeText) "${d.name}님이 운전할 때 크게 보여요" else "꺼짐",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = d.largeText, onCheckedChange = null, modifier = Modifier.scale(1.25f).padding(horizontal = 8.dp))
        }
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
