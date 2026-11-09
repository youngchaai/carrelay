package com.carrelay.vehicle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.carrelay.core.text.KoreanText
import com.carrelay.vehicle.ui.HandoffSummary
import com.carrelay.vehicle.ui.theme.CarIcons
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * 남은 시간 원: 시간이 지날수록 원이 줄어듭니다. 가운데에 남은 초.
 * 글자를 읽지 않아도 '곧 넘어간다'를 한눈에 알 수 있어 주행 중 화면에 씁니다(부드럽게 한 방향으로만 움직임).
 * 화면이 다시 그려져도 [startedAtMillis] 기준으로 이어서 줄어듭니다.
 */
@Composable
fun CountdownRing(
    durationMillis: Long,
    startedAtMillis: Long,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val left = remember(startedAtMillis, durationMillis) {
        Animatable(((durationMillis - (System.currentTimeMillis() - startedAtMillis)).toFloat() / durationMillis).coerceIn(0f, 1f))
    }
    LaunchedEffect(startedAtMillis, durationMillis) {
        val ms = (left.value * durationMillis).toInt()
        if (ms > 0) left.animateTo(0f, tween(ms, easing = LinearEasing))
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.10f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * left.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${ceil(left.value * durationMillis / 1000f).toInt()}", style = textStyle, color = color)
    }
}

/**
 * 인수 점검 끝: 체크가 그려지고 '인수 완료' 도장이 찍힙니다(약 1초, 주차 중 브리핑에서만).
 * 끝나면 [onDone]. 누르면 바로 넘어갑니다.
 */
@Composable
fun HandoverStamp(driverName: String, timeText: String, onDone: () -> Unit) {
    val ink = MaterialTheme.colorScheme.primary
    val check = remember { Animatable(0f) }
    val stamp = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) {
            finished = true
            onDone()
        }
    }
    LaunchedEffect(Unit) {
        launch { check.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        delay(300)
        stamp.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        delay(650)
        finish()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Canvas(Modifier.size(132.dp)) {
                val w = size.width
                val sw = w * 0.07f
                drawCircle(ink.copy(alpha = 0.14f), radius = w / 2)
                drawArc(ink, -90f, 360f * check.value, false, Offset(sw / 2, sw / 2), Size(w - sw, w - sw), style = Stroke(sw, cap = StrokeCap.Round))
                val path = Path().apply {
                    moveTo(w * 0.30f, w * 0.52f)
                    lineTo(w * 0.45f, w * 0.66f)
                    lineTo(w * 0.72f, w * 0.38f)
                }
                val pm = PathMeasure().apply { setPath(path, false) }
                val part = Path()
                pm.getSegment(0f, pm.length * ((check.value - 0.35f) / 0.65f).coerceIn(0f, 1f), part, true)
                drawPath(part, ink, style = Stroke(sw, cap = StrokeCap.Round))
            }
            StampMark("인수 완료", "$driverName · $timeText", stamp.value, ink)
        }
    }
}

/** 도장: 크게 떨어지며 비스듬히 찍힘. [progress] 0 = 안 보임, 1 = 찍힘 */
@Composable
private fun StampMark(title: String, sub: String, progress: Float, ink: Color) {
    Box(
        Modifier
            .alpha(progress.coerceIn(0f, 1f))
            .scale(1.6f - 0.6f * progress)
            .rotate(-8f)
            .border(6.dp, ink, RoundedCornerShape(18.dp))
            .padding(horizontal = 40.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.displaySmall, color = ink)
            Text(sub, style = MaterialTheme.typography.titleMedium, color = ink)
        }
    }
}

/**
 * 운전 끝(인계): '인계 완료' 도장과 이번 운전 요약(시간·거리·남긴 메모)을 약 3초 보여 줍니다.
 * 시작의 '인수 완료'와 짝을 이루어 바통을 넘기는 순간을 분명히 합니다(주차 중에만, 누르면 바로 넘어감).
 */
@Composable
fun HandoffOverlay(summary: HandoffSummary, onDone: () -> Unit) {
    val ink = MaterialTheme.colorScheme.primary
    val fade = remember { Animatable(0f) }
    val stamp = remember { Animatable(0f) }
    val stats = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (finished) return
        finished = true
        scope.launch {
            fade.animateTo(0f, tween(220))
            onDone()
        }
    }
    LaunchedEffect(summary) {
        launch { fade.animateTo(1f, tween(200)) }
        launch { fill.animateTo(1f, tween(HANDOFF_MILLIS, easing = LinearEasing)) }
        delay(150)
        stamp.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        stats.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        delay((HANDOFF_MILLIS - 900).toLong())
        finish()
    }
    val time = remember(summary.endedAtMillis) {
        java.time.Instant.ofEpochMilli(summary.endedAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            .let { "%02d:%02d".format(it.hour, it.minute) }
    }
    val line = when {
        summary.company -> buildString {
            append("운행기록부에 적었어요 · ")
            append(summary.purpose?.label ?: "목적 미입력")
            val a = summary.startOdometerKm
            val b = summary.endOdometerKm
            if (a != null && b != null) append(" · %,.1f → %,.1fkm".format(a, b))
        }
        summary.memosLeft > 0 || summary.openIncidents > 0 -> "다음 분이 타면 남긴 메모와 처리할 일부터 알려 드려요"
        else -> "다음 분이 타면 차 상태를 바로 알려 드려요"
    }
    Box(
        Modifier
            .fillMaxSize()
            .alpha(fade.value)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Text("수고하셨어요, ${summary.driverName}님", style = MaterialTheme.typography.displaySmall)
            StampMark("인계 완료", "${summary.driverName} · $time", stamp.value, ink)
            Row(
                Modifier.alpha(stats.value).offset(y = (16 * (1f - stats.value)).dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SummaryStat(
                    CarIcons.Clock, "운전",
                    if (summary.drivingMillis < 60_000L) "1분 미만" else KoreanText.duration(summary.drivingMillis),
                )
                // 아래 운행기록부 줄과 같은 자릿수(소수 한 자리)로 맞춥니다.
                summary.distanceKm?.takeIf { it >= 0.1 }?.let { SummaryStat(CarIcons.Route, "거리", "%.1fkm".format(it)) }
                SummaryStat(
                    CarIcons.Memo, "다음 분께",
                    if (summary.memosLeft > 0) "메모 ${summary.memosLeft}개" else "메모 없음",
                )
            }
            Text(
                line,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(stats.value),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(360.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.15f))) {
                    Box(Modifier.fillMaxWidth(fill.value).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(ink.copy(alpha = 0.6f)))
                }
                Text("누르면 바로 넘어가요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 인계 요약이 보이는 시간 */
private const val HANDOFF_MILLIS = 3400

@Composable
private fun SummaryStat(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * 단계 표시: 지난 단계는 체크, 지금 단계는 진하게, 남은 단계는 옅게.
 * 움직임 없이 그대로 그려서 주행 중 화면(케어 모드)에도 씁니다.
 * [finished] 면 마지막 단계까지 모두 체크. [labelStyle] 이 null 이면 점만 그립니다(목록 한 줄용).
 */
@Composable
fun StepTrack(
    steps: List<String>,
    current: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    finished: Boolean = false,
    dot: Dp = 40.dp,
    labelStyle: TextStyle? = MaterialTheme.typography.titleMedium,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    // 아직 안 온 단계의 점 테두리: 창 배경 위에서도 보이게 선보다 진하게
    val pending = MaterialTheme.colorScheme.outline
    val n = steps.size
    val line = (dot.value * 0.12f).coerceAtLeast(3f).dp
    Row(
        modifier.drawBehind {
            // 점과 점 사이 선: 지나온 구간은 진하게
            val slot = size.width / n
            val r = dot.toPx() / 2
            val y = r
            for (i in 0 until n - 1) {
                val done = finished || i < current
                drawLine(
                    if (done) color else track,
                    Offset(slot * (i + 0.5f) + r + 4.dp.toPx(), y),
                    Offset(slot * (i + 1.5f) - r - 4.dp.toPx(), y),
                    strokeWidth = line.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        },
    ) {
        steps.forEachIndexed { i, label ->
            val done = finished || i < current
            val now = !finished && i == current
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(dot)
                        .clip(CircleShape)
                        .background(if (done || now) color else Color.Transparent)
                        .then(if (done || now) Modifier else Modifier.border(line, pending, CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        done -> Icon(CarIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(dot * 0.6f))
                        dot >= 28.dp -> Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (now) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (labelStyle != null) {
                    Text(
                        label,
                        style = labelStyle,
                        fontWeight = if (now) FontWeight.Bold else null,
                        color = when {
                            now -> color
                            done -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 진행 원: 0..1 만큼 원이 차오릅니다(처음 보일 때 0에서 올라감). 가운데에 [content].
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit = {},
) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(progress) { shown.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.11f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * shown.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

/** 숫자 올라가기: 처음 보일 때 0에서 [target] 까지, 값이 바뀌면 거기서 이어서 움직입니다. */
@Composable
fun animatedCount(target: Double, durationMillis: Int = 900): Double {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { a.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing)) }
    return a.value.toDouble()
}

/**
 * 할 일 줄긋기: 글자 위에 줄이 왼쪽에서 오른쪽으로 그어집니다. 여러 줄이면 윗줄부터 차례로.
 * [progress] 0 = 줄 없음, 1 = 끝까지.
 */
fun Modifier.strikeThrough(progress: Float, layout: () -> TextLayoutResult?, color: Color): Modifier =
    if (progress <= 0f) this else drawWithContent {
        drawContent()
        val l = layout() ?: return@drawWithContent
        val lines = l.lineCount
        if (lines == 0) return@drawWithContent
        val perLine = 1f / lines
        for (i in 0 until lines) {
            val p = ((progress - i * perLine) / perLine).coerceIn(0f, 1f)
            if (p <= 0f) break
            val left = l.getLineLeft(i)
            val right = l.getLineRight(i)
            val y = (l.getLineTop(i) + l.getLineBottom(i)) / 2
            drawLine(color, Offset(left, y), Offset(left + (right - left) * p, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
