package com.carrelay.vehicle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carrelay.core.model.Driver
import com.carrelay.core.profile.LicenseState
import com.carrelay.core.profile.PartyRoster
import com.carrelay.core.text.KoreanText
import com.carrelay.core.triplog.TripLog
import com.carrelay.vehicle.ui.UiState
import com.carrelay.vehicle.ui.avatarTone
import com.carrelay.vehicle.ui.components.Avatar
import com.carrelay.vehicle.ui.components.IconCircle
import com.carrelay.vehicle.ui.components.PrimaryButton
import com.carrelay.vehicle.ui.components.ScreenHeader
import com.carrelay.vehicle.ui.components.StatusBadge
import com.carrelay.vehicle.ui.components.SurfaceCard
import com.carrelay.vehicle.ui.components.VehicleStatusCard
import com.carrelay.vehicle.ui.components.conditionTone
import com.carrelay.vehicle.ui.components.primaryTone
import com.carrelay.vehicle.ui.formatWhen
import com.carrelay.vehicle.ui.isCompany
import com.carrelay.vehicle.ui.lastEndedSession
import com.carrelay.vehicle.ui.nameOf
import com.carrelay.vehicle.ui.theme.CarIcons
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone
import com.carrelay.vehicle.ui.tripLogRows
import kotlin.math.roundToInt

/**
 * F2 운전자 선택 = 차를 넘겨받는 순간.
 * 왼쪽: 이 차가 지금 어떤 상태로 기다리는지(차 그림·숫자·마지막 주차·남은 메모·처리 안 된 사고).
 * 오른쪽: 내 이름을 한 번 누르면 끝. 카드에는 그 사람 색, 지난 운전, 꼭 알아야 할 것 두 가지까지만.
 * 기어 P에서만 고를 수 있습니다.
 */
@Composable
fun DriverPickerScreen(ui: UiState, onSelect: (String) -> Unit, onKeepSame: () -> Unit, onAdd: () -> Unit = {}) {
    val drivers = ui.drivers + Driver.GUEST
    // "바뀌었나요?"를 묻는 중에는 고르기만 하게, 새 사람 추가는 평소에만 보여 줍니다.
    val showAdd = !ui.askingDriverChange && PartyRoster.canAdd(ui.drivers)
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        val snap = ui.snapshot
        if (snap != null) {
            VehicleStatusCard(
                snap,
                Modifier.weight(0.31f).fillMaxHeight(),
                incidents = ui.incidents,
                title = ui.vehicleName.ifBlank { "차 상태" },
            ) { HandoverFacts(ui) }
        }

        Column(Modifier.weight(0.69f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            ScreenHeader(
                title = if (ui.askingDriverChange) "운전하는 분이 바뀌었나요?" else "누가 운전하시나요?",
                subtitle = when {
                    !ui.canSelectDriver -> "차를 세우고(P) 골라 주세요"
                    ui.askingDriverChange -> "문이 열렸다 닫혔어요. 같은 분이면 아래 버튼을 눌러 주세요"
                    ui.isCompany -> "내 이름을 눌러 주세요. 운행기록부의 사용자 칸이 자동으로 채워져요"
                    else -> "내 이름을 눌러 주세요. 지난번과 달라진 점을 알려 드려요"
                },
            )

            val current = ui.currentDriver
            if (ui.askingDriverChange && current != null) {
                PrimaryButton("그대로 ${current.name}님이 운전해요", onKeepSame, Modifier.fillMaxWidth(), icon = CarIcons.Check)
            } else {
                ui.recommendation?.let { rec ->
                    val level = ui.conditions[rec.driverId]?.level
                    SurfaceCard(container = MaterialTheme.colorScheme.primaryContainer, border = null, contentPadding = 24.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            IconCircle(CarIcons.Route, primaryTone(), size = 64.dp, iconSize = 34.dp)
                            Text(
                                "오늘 장거리는 ${ui.nameOf(rec.driverId)}님이 운전하면 좋아요",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            if (level != null) StatusBadge("컨디션 ${level.label}", conditionTone(level))
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
                verticalArrangement = Arrangement.spacedBy(Dimens.gap),
                modifier = Modifier.weight(1f),
            ) {
                items(drivers, key = { it.id }) { driver -> DriverCard(driver, ui, onSelect) }
                if (showAdd) item(key = "add") { AddDriverCard(ui, onAdd) }
            }
        }
    }
}

/** 넘겨받기 전에 알아 둘 것: 누가 언제 세워 뒀는지, 남은 메모, 처리 안 된 사고, (회사 차) 업무사용비율 */
@Composable
private fun HandoverFacts(ui: UiState) {
    val s = CarRelayTheme.status
    val last = ui.lastEndedSession
    val memos = ui.memos.count { !it.isResolved }
    val open = ui.incidents.count { it.isOpen }
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (last != null) {
            FactRow(
                CarIcons.Clock, MaterialTheme.colorScheme.onSurfaceVariant,
                "${ui.nameOf(last.driverId)}님 · ${formatWhen(last.endedAtMillis ?: last.startedAtMillis)} 주차",
            )
        }
        if (open > 0) FactRow(CarIcons.Warning, s.danger.content, "처리 안 된 사고·파손 ${open}건")
        if (memos > 0) FactRow(CarIcons.Memo, s.memo.content, "남은 메모 ${memos}개")
        if (ui.isCompany) {
            val ratio = TripLog.summary(ui.tripLogRows()).businessRatio
            if (ratio != null) FactRow(CarIcons.Records, s.info.content, "운행기록부 업무사용비율 ${(ratio * 100).roundToInt()}%")
        }
        if (last == null && open == 0 && memos == 0) FactRow(CarIcons.CheckCircle, s.ok.content, "남겨진 메모나 사고가 없어요")
    }
}

@Composable
private fun FactRow(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 새 사람 추가: 점선 대신 옅은 테두리와 큰 + 로, 다른 카드보다 한 단계 조용하게 */
@Composable
private fun AddDriverCard(ui: UiState, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.cardRadius)
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(232.dp)
            .alpha(if (ui.canSelectDriver) 1f else 0.45f)
            .clip(shape)
            .border(BorderStroke(2.dp, c.primary.copy(alpha = 0.35f)), shape)
            .clickable(enabled = ui.canSelectDriver, onClick = onAdd)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IconCircle(CarIcons.PersonAdd, primaryTone(), size = 88.dp, iconSize = 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (ui.isCompany) "새 직원 추가" else "새 가족 추가", style = MaterialTheme.typography.headlineSmall, color = c.primary)
            Text(
                "이름만 적으면 바로 탈 수 있어요. 면허 정보는 나중에 넣어도 돼요",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class CardNote(val text: String, val tone: StatusTone, val icon: ImageVector)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverCard(driver: Driver, ui: UiState, onSelect: (String) -> Unit) {
    val recommended = ui.recommendation?.driverId == driver.id
    val s = CarRelayTheme.status
    val tone = ui.avatarTone(driver.id)
    val license = ui.licenses[driver.id]
    val memoForMe = ui.memos.count { !it.isResolved && it.toDriverId == driver.id }
    val lastDrive = ui.sessions.lastOrNull { it.driverId == driver.id }
    // 중요한 것부터 두 개까지만
    val notes = buildList {
        if (memoForMe > 0) add(CardNote("나에게 메모 ${memoForMe}개", s.memo, CarIcons.Memo))
        if (recommended) add(CardNote("오늘 추천", primaryTone(), CarIcons.Route))
        when (license?.state) {
            LicenseState.EXPIRED -> add(CardNote("면허 갱신 기한 지남", s.danger, CarIcons.Warning))
            LicenseState.RENEW_SOON -> add(CardNote("면허 갱신 ${license?.daysLeft ?: 0}일 남음", s.warning, CarIcons.Clock))
            else -> Unit
        }
        if (license?.isNovice == true) add(CardNote("초보 운전", s.info, CarIcons.Info))
        if (ui.lastDriverId == driver.id) add(CardNote("방금 운전한 분", s.info, CarIcons.Handover))
    }.take(2)

    val shape = RoundedCornerShape(Dimens.cardRadius)
    val enabled = ui.canSelectDriver
    // 추천 받은 분, 또는 "바뀌었나요?"를 묻는 중이면 지금 운전하던 분을 테두리로 짚어 줍니다.
    val outlined = recommended || (ui.askingDriverChange && ui.currentDriver?.id == driver.id)
    // 그 사람 색으로 위쪽을 살짝 물들여 이름보다 먼저 '내 카드'가 눈에 들어오게 합니다.
    Column(
        Modifier
            .fillMaxWidth()
            .height(232.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Brush.verticalGradient(0f to tone.container, 0.75f to MaterialTheme.colorScheme.surface))
            .border(
                if (outlined) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape,
            )
            .clickable(enabled = enabled) { onSelect(driver.id) }
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Avatar(if (driver.isGuest) "?" else driver.name, tone, size = 88.dp)
            Column(Modifier.weight(1f)) {
                Text(driver.name, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
                val sub = when {
                    driver.isGuest -> if (ui.isCompany) "등록 안 된 분" else "가족이 아닌 분"
                    else -> driver.department
                }
                if (sub != null) Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(CarIcons.Handover, contentDescription = null, tint = tone.content.copy(alpha = 0.55f), modifier = Modifier.size(32.dp))
        }
        // 지난 운전: 언제, 얼마나. 게스트는 기록이 어떻게 남는지 알려 줍니다.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CarIcons.Clock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Text(
                when {
                    driver.isGuest -> "운전 기록은 게스트로 남아요"
                    lastDrive != null -> "지난 운전 ${formatWhen(lastDrive.endedAtMillis ?: lastDrive.startedAtMillis)}" +
                        (lastDrive.distanceKm?.takeIf { it > 0.0 }?.let { " · ${KoreanText.distance(it)}" } ?: "")
                    else -> "아직 운전 기록이 없어요"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { StatusBadge(it.text, it.tone, icon = it.icon) }
        }
    }
}
