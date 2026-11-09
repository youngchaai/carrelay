package com.carrelay.vehicle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextLayoutResult
import com.carrelay.vehicle.ui.theme.CarIcons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carrelay.core.model.ConditionLevel
import com.carrelay.vehicle.ui.theme.CarRelayTheme
import com.carrelay.vehicle.ui.theme.Dimens
import com.carrelay.vehicle.ui.theme.StatusTone

// ── 버튼 ────────────────────────────────────────────────

/** 주요 행동 버튼 (기본 높이 104dp) */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.button,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    container: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(Dimens.buttonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
    ) {
        ButtonLabel(text, icon, textStyle)
    }
}

/** 보조 행동 버튼 (테두리) */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.button,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(Dimens.buttonRadius),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
    ) {
        ButtonLabel(text, icon, textStyle)
    }
}

/** 덜 중요한 행동 (글자 버튼, 최소 88dp) */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.touch),
        shape = RoundedCornerShape(Dimens.buttonRadius),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ButtonLabel(text, icon, MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?, textStyle: TextStyle) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size((textStyle.fontSize.value * 1.15f).dp))
        Spacer(Modifier.width(14.dp))
    }
    Text(text, style = textStyle, textAlign = TextAlign.Center, maxLines = 2)
}

// ── 카드·배지 ──────────────────────────────────────────

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    contentPadding: Dp = Dimens.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.cardRadius)
    var m = modifier.clip(shape).background(container)
    if (border != null) m = m.border(border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(Dimens.gapS), content = content)
}

/** 상태 배지: 아이콘 + 짧은 라벨 */
@Composable
fun StatusBadge(text: String, tone: StatusTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tone.container)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone.content)
    }
}

/** 원 안의 아이콘 */
@Composable
fun IconCircle(icon: ImageVector, tone: StatusTone, size: Dp = 64.dp, iconSize: Dp = 34.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(tone.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(iconSize))
    }
}

/** 운전자 머리글자 아바타 */
@Composable
fun Avatar(name: String, tone: StatusTone, size: Dp = 72.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(tone.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1),
            color = tone.content,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 강조 색(브랜드) 배지 톤 */
@Composable
fun primaryTone(): StatusTone =
    StatusTone(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)

/** 중립 배지 톤 */
@Composable
fun neutralTone(): StatusTone =
    StatusTone(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)

/** 컨디션 3단계 → 의미 색 */
@Composable
fun conditionTone(level: ConditionLevel): StatusTone = when (level) {
    ConditionLevel.GOOD -> CarRelayTheme.status.ok
    ConditionLevel.NORMAL -> CarRelayTheme.status.info
    ConditionLevel.REST_NEEDED -> CarRelayTheme.status.warning
}

// ── 머리말·수치 ────────────────────────────────────────

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** 큰 수치 하나: 라벨 / 값 / 보조 설명 */
@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    caption: String? = null,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    captionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = secondaryColor, modifier = Modifier.size((labelStyle.fontSize.value * 1.15f).dp))
            }
            Text(label, style = labelStyle, color = secondaryColor)
        }
        Text(value, style = valueStyle, color = valueColor)
        if (caption != null) {
            Text(caption, style = captionStyle, color = secondaryColor)
        }
    }
}

/**
 * 알림 한 줄: 아이콘 + 문장. 배지·작은 라벨 없이 문장으로 말합니다.
 * [actionLabel] 이 있으면 줄 전체를 누를 수 있고 오른쪽에 할 일을 적습니다.
 */
@Composable
fun NoticeRow(
    icon: ImageVector,
    tone: StatusTone,
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    detail: String? = null,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
    /** 해결 처리 중: 0..1 만큼 글자에 줄이 그어지고 옅어집니다 */
    struck: Float = 0f,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textColor = if (emphasized) tone.content else MaterialTheme.colorScheme.onSurface
    SurfaceCard(
        modifier.fillMaxWidth().alpha(1f - 0.45f * struck),
        container = if (emphasized) tone.container else MaterialTheme.colorScheme.surface,
        border = if (emphasized) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = 22.dp,
        onClick = if (struck > 0f) null else onClick,
    ) {
        Row(
            Modifier.heightIn(min = 68.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            IconCircle(if (struck >= 1f) CarIcons.CheckCircle else icon, tone, size = 60.dp, iconSize = 32.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    onTextLayout = { layout = it },
                    modifier = Modifier.strikeThrough(struck, { layout }, textColor),
                )
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (emphasized) tone.content else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (actionLabel != null && struck <= 0f) {
                Text(
                    "$actionLabel ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (emphasized) tone.content else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 화면 맨 아래 행동 버튼 줄 */
@Composable
fun ActionBar(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = Dimens.gapS),
        horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
