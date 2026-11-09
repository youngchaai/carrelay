package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.core.model.LicenseClass
import com.carrelay.core.profile.LicenseCheck
import com.carrelay.core.profile.PartyRoster
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.QuietButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.SecondaryButton
import com.carrelay.vehicle.ui.components.SectionTitle
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.isCompany
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import java.time.LocalDate

/**
 * 운전자(가족·직원) 추가·고치기. 차가 서 있을 때만 열립니다(주행 중에는 주행 화면으로 바뀜).
 * 글자로 적는 칸은 이름·부서(보호자) 두 개뿐이고, 면허는 버튼과 -/+ 로 고릅니다. 면허 번호는 받지 않습니다.
 * 저장·취소는 위쪽에 둬서 화면 키보드가 아래를 가려도 누를 수 있게 합니다.
 */
@Composable
fun ProfileEditScreen(
    ui: UiState,
    onSave: (Driver) -> Unit,
    onCancel: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val edit = ui.profileEdit ?: return
    key(edit.driverId) { ProfileEditor(ui, edit.driverId?.let { id -> ui.drivers.firstOrNull { it.id == id } }, onSave, onCancel, onDelete) }
}

@Composable
private fun ProfileEditor(
    ui: UiState,
    original: Driver?,
    onSave: (Driver) -> Unit,
    onCancel: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var department by rememberSaveable { mutableStateOf(original?.department ?: "") }
    var guardian by rememberSaveable { mutableStateOf(original?.guardianName ?: "") }
    var licenseClass by rememberSaveable { mutableStateOf(original?.licenseClass) }
    var sinceYear by rememberSaveable { mutableStateOf(original?.licensedSinceYear) }
    var expiryDay by rememberSaveable { mutableStateOf(original?.licenseExpiryEpochDay) }
    var largeText by rememberSaveable { mutableStateOf(original?.largeText ?: false) }
    var triedSave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val problem = PartyRoster.nameProblem(name, ui.drivers, selfId = original?.id)
    val draft = (original ?: Driver(id = "", name = "")).copy(
        name = name,
        department = if (ui.isCompany) department else original?.department,
        guardianName = if (ui.isCompany) original?.guardianName else guardian,
        licenseClass = licenseClass,
        licensedSinceYear = sinceYear,
        licenseExpiryEpochDay = expiryDay,
        largeText = largeText,
    )
    val license = LicenseCheck.of(draft, today.toEpochDay(), today.year)
    val who = if (ui.isCompany) "직원" else "가족"

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ScreenHeader(
            title = if (original == null) "새 $who 추가" else "${original.name}님 프로필",
            subtitle = "차가 서 있을 때만 고칠 수 있어요 · 면허 번호는 받지 않아요",
            leading = {
                Avatar(
                    name.trim().ifEmpty { "+" },
                    if (original != null) ui.avatarTone(original.id)
                    else CarRelayTheme.status.avatars.let { it[PartyRoster.nextTone(ui.drivers) % it.size] },
                    size = 80.dp,
                )
            },
        ) {
            SecondaryButton("취소", onCancel, height = 88.dp, icon = CarIcons.Close)
            Spacer(Modifier.size(Dimens.gapS))
            PrimaryButton(
                if (original == null) "추가하기" else "저장",
                {
                    triedSave = true
                    if (problem == null) onSave(draft)
                },
                height = 88.dp,
                icon = CarIcons.Check,
            )
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            // 왼쪽: 기본 정보
            SurfaceCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                    SectionTitle("기본 정보")
                    BigField(
                        value = name,
                        onChange = { name = it.take(PartyRoster.MAX_NAME + 2) },
                        label = if (ui.isCompany) "이름 · 호칭" else "부르는 이름",
                        placeholder = if (ui.isCompany) "예: 정 대리" else "예: 삼촌",
                        error = problem?.takeIf { triedSave || name.isNotEmpty() },
                        imeAction = ImeAction.Next,
                    )
                    if (ui.isCompany) {
                        BigField(
                            value = department,
                            onChange = { department = it.take(PartyRoster.MAX_DEPARTMENT) },
                            label = "부서 (운행기록부의 사용자 부서)",
                            placeholder = "예: 영업1팀",
                        )
                    } else {
                        BigField(
                            value = guardian,
                            onChange = { guardian = it.take(PartyRoster.MAX_NAME) },
                            label = "보호자 (케어 모드 알림을 받을 분, 선택)",
                            placeholder = "예: 딸",
                        )
                    }
                    ToggleRow(
                        icon = CarIcons.TextSize,
                        title = "큰 글씨",
                        subtitle = if (largeText) "이 분이 운전할 때 화면 전체를 1.2배로" else "꺼짐",
                        checked = largeText,
                        onToggle = { largeText = it },
                    )
                    if (!ui.isCompany && original != null) {
                        Text(
                            "케어 모드는 본인 동의가 필요해서 설정 화면의 케어 모드에서 따로 켜요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (original != null) {
                    QuietButton("이 ${who} 삭제", { confirmDelete = true }, icon = CarIcons.Trash)
                }
            }

            // 오른쪽: 면허 (선택) — 키보드 없이 버튼과 -/+ 로
            SurfaceCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                    SectionTitle("면허 (선택)")
                    Text("종류", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    (LicenseClass.entries.map<LicenseClass, LicenseClass?> { it } + null).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                            row.forEach { c ->
                                Chip(c?.label ?: "모름", selected = licenseClass == c, modifier = Modifier.weight(1f)) { licenseClass = c }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }

                    Text("처음 받은 해", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Stepper(
                        value = sinceYear?.let { "${it}년" } ?: "모름",
                        onMinus = { sinceYear = ((sinceYear ?: today.year) - 1).coerceAtLeast(1950) },
                        onPlus = { sinceYear = ((sinceYear ?: (today.year - 1)) + 1).coerceAtMost(today.year) },
                        caption = license.yearsLicensed?.let { y -> if (license.isNovice) "초보 운전 (2년 미만)" else "운전 ${y}년" },
                        onClear = if (sinceYear != null) ({ sinceYear = null }) else null,
                    )

                    Text("갱신 기한 (적성검사)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val expiry = expiryDay?.let { LocalDate.ofEpochDay(it) }
                    // 갱신 기한은 대개 그해·그달 말까지라 연·월로 고르고, 고르면 그달 말일로 둡니다.
                    fun setExpiry(d: LocalDate) {
                        expiryDay = d.withDayOfMonth(d.lengthOfMonth()).toEpochDay()
                    }
                    val base = expiry ?: today.plusYears(5).withMonth(12)
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapS)) {
                        Stepper(
                            value = expiry?.let { "${it.year}년" } ?: "모름",
                            onMinus = { setExpiry(base.minusYears(if (expiry == null) 0 else 1)) },
                            onPlus = { setExpiry(base.plusYears(if (expiry == null) 0 else 1)) },
                            modifier = Modifier.weight(1f),
                        )
                        Stepper(
                            value = expiry?.let { if (it.dayOfMonth == it.lengthOfMonth()) "${it.monthValue}월 말" else "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "-",
                            onMinus = { setExpiry(base.minusMonths(if (expiry == null) 0 else 1)) },
                            onPlus = { setExpiry(base.plusMonths(if (expiry == null) 0 else 1)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val left = license.daysLeft
                        Text(
                            when {
                                left == null -> "모르면 비워 둬도 돼요"
                                left < 0 -> "갱신 기한이 ${-left}일 지났어요"
                                left <= LicenseCheck.RENEW_SOON_DAYS -> "갱신까지 ${left}일 · 운전자 선택 화면에 알려 드려요"
                                left < 365 -> "갱신까지 약 ${left / 30}개월 남았어요"
                                else -> "갱신까지 약 ${left / 365}년 ${(left % 365) / 30}개월 남았어요"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if ((license.daysLeft ?: 999) <= LicenseCheck.RENEW_SOON_DAYS) CarRelayTheme.status.warning.content else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (expiry != null) QuietButton("모름으로", { expiryDay = null })
                    }
                }
            }
        }
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("${original.name}님을 삭제할까요?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "운전자 선택 화면에서 빠져요. 지난 운전 기록" + (if (ui.isCompany) "과 운행기록부" else "") + "에는 이름이 그대로 남아요.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(original.id)
                }) { Text("삭제", style = MaterialTheme.typography.labelLarge, color = CarRelayTheme.status.danger.content) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

@Composable
private fun BigField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.titleLarge) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
    )
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .height(80.dp)
            .clip(shape)
            .background(if (selected) c.primaryContainer else c.surface)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) c.primary else c.outlineVariant), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = if (selected) c.onPrimaryContainer else c.onSurface, maxLines = 1)
    }
}

/** - 값 + : 키보드 없이 숫자 고르기 */
@Composable
private fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClear: (() -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(CarIcons.Minus, "줄이기", onMinus)
            Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            StepButton(CarIcons.Plus, "늘리기", onPlus)
        }
        if (caption != null || onClear != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(caption ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (onClear != null) QuietButton("모름으로", onClear)
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.scale(1.25f).padding(horizontal = 8.dp))
    }
}
