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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
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
            // 도장: 크게 떨어지며 비스듬히 찍힘
            Box(
                Modifier
                    .alpha(stamp.value.coerceIn(0f, 1f))
                    .scale(1.6f - 0.6f * stamp.value)
                    .rotate(-8f)
                    .border(6.dp, ink, RoundedCornerShape(18.dp))
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("인수 완료", style = MaterialTheme.typography.displaySmall, color = ink)
                    Text("$driverName · $timeText", style = MaterialTheme.typography.titleMedium, color = ink)
                }
            }
        }
    }
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
