package com.carrelay.vehicle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.carrelay.core.model.CarZone
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * 위에서 내려다본 차 (앞이 위쪽). 사고·파손 위치를 표시하고, [onZoneClick] 을 주면
 * 앞·뒤·왼쪽·오른쪽·실내를 눌러 고를 수 있습니다.
 *
 * 그림: 바닥 그림자 → 바퀴 → 차체(가운데가 밝은 금속 느낌) → 유리(앞·지붕·뒤) → 픽셀 전조등·후미등.
 * 표시 점은 화면에 처음 나올 때 세 번 퍼지듯 깜빡여 눈에 띄게 하고, 그 뒤에는 멈춥니다(계속 움직이지 않게).
 *
 * @param marks 위치별 표시 색 (예: 처리 안 된 파손 = 위험 색)
 * @param selected 신고 화면에서 고른 위치
 */
@Composable
fun CarTopView(
    modifier: Modifier = Modifier,
    marks: Map<CarZone, StatusTone> = emptyMap(),
    selected: Set<CarZone> = emptySet(),
    selectedTone: StatusTone? = null,
    onZoneClick: ((CarZone) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.4f
    val paint = remember(dark) { if (dark) CarPaint.Dark else CarPaint.Light }
    val pick = selectedTone ?: StatusTone(colors.primary, colors.primaryContainer)

    // 표시 점: 처음 보일 때 세 번만 퍼지는 고리
    val pulse = remember { Animatable(0f) }
    val markKey = marks.keys.sorted().joinToString()
    LaunchedEffect(markKey) {
        if (marks.isEmpty()) return@LaunchedEffect
        repeat(3) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(900))
        }
        pulse.snapTo(0f)
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val body = bodyPath(w, h)

            // 바닥 그림자: 차체 모양을 조금씩 내려 겹쳐 부드럽게
            listOf(0.006f to 1f, 0.014f to 0.6f, 0.022f to 0.3f).forEach { (dy, a) ->
                translate(left = w * 0.006f, top = h * dy) { drawPath(body, Color.Black.copy(alpha = paint.shadowAlpha * a)) }
            }

            // 바퀴 (차체가 안쪽을 덮습니다)
            listOf(0.16f to 0.31f, 0.70f to 0.85f).forEach { (top, bottom) ->
                drawRoundRect(paint.tire, Offset(w * 0.075f, h * top), Size(w * 0.13f, h * (bottom - top)), CornerRadius(w * 0.05f))
                drawRoundRect(paint.tire, Offset(w * 0.795f, h * top), Size(w * 0.13f, h * (bottom - top)), CornerRadius(w * 0.05f))
            }

            // 사이드미러
            drawRoundRect(paint.bodyEdge, Offset(w * 0.055f, h * 0.30f), Size(w * 0.11f, h * 0.035f), CornerRadius(w * 0.03f))
            drawRoundRect(paint.bodyEdge, Offset(w * 0.835f, h * 0.30f), Size(w * 0.11f, h * 0.035f), CornerRadius(w * 0.03f))

            // 차체
            drawPath(
                body,
                Brush.horizontalGradient(
                    listOf(paint.bodyEdge, paint.bodyMid, paint.bodyHighlight, paint.bodyMid, paint.bodyEdge),
                    startX = w * 0.12f,
                    endX = w * 0.88f,
                ),
            )
            drawPath(body, paint.outline, style = Stroke(width = w * 0.012f))

            // 보닛 주름선
            drawLine(paint.crease, Offset(w * 0.33f, h * 0.075f), Offset(w * 0.30f, h * 0.235f), strokeWidth = w * 0.008f)
            drawLine(paint.crease, Offset(w * 0.67f, h * 0.075f), Offset(w * 0.70f, h * 0.235f), strokeWidth = w * 0.008f)

            // 유리: 앞유리 → 파노라마 지붕 → 뒷유리
            val glass = Brush.verticalGradient(listOf(paint.glassTop, paint.glassBottom), startY = h * 0.24f, endY = h * 0.80f)
            drawPath(trapezoid(w * 0.20f, w * 0.80f, h * 0.265f, w * 0.255f, w * 0.745f, h * 0.345f), glass)
            drawRoundRect(glass, Offset(w * 0.255f, h * 0.355f), Size(w * 0.49f, h * 0.33f), CornerRadius(w * 0.06f))
            drawPath(trapezoid(w * 0.27f, w * 0.73f, h * 0.695f, w * 0.30f, w * 0.70f, h * 0.775f), glass)
            // 옆 유리
            drawRoundRect(paint.glassBottom.copy(alpha = 0.55f), Offset(w * 0.19f, h * 0.36f), Size(w * 0.045f, h * 0.31f), CornerRadius(w * 0.02f))
            drawRoundRect(paint.glassBottom.copy(alpha = 0.55f), Offset(w * 0.765f, h * 0.36f), Size(w * 0.045f, h * 0.31f), CornerRadius(w * 0.02f))
            // 앞유리 반사광
            drawLine(Color.White.copy(alpha = paint.reflection), Offset(w * 0.30f, h * 0.335f), Offset(w * 0.40f, h * 0.275f), strokeWidth = w * 0.014f)

            // 픽셀 전조등 / 후미등: 어두운 램프 띠 위에 작은 네모
            drawRoundRect(paint.lampStrip, Offset(w * 0.21f, h * 0.040f), Size(w * 0.58f, h * 0.032f), CornerRadius(w * 0.02f))
            drawRoundRect(paint.lampStrip, Offset(w * 0.21f, h * 0.928f), Size(w * 0.58f, h * 0.032f), CornerRadius(w * 0.02f))
            pixels(w * 0.235f, h * 0.048f, w, paint.headlight)
            pixels(w * 0.625f, h * 0.048f, w, paint.headlight)
            pixels(w * 0.235f, h * 0.935f, w, paint.taillight)
            pixels(w * 0.625f, h * 0.935f, w, paint.taillight)

            // 고른 위치 / 표시
            CarZone.entries.forEach { zone ->
                val c = zoneCenter(zone, w, h)
                val r = w * 0.085f
                if (zone in selected) {
                    drawCircle(pick.container, r * 1.25f, c)
                    drawCircle(pick.content, r * 1.25f, c, style = Stroke(w * 0.02f))
                }
                marks[zone]?.let { tone ->
                    val p = pulse.value
                    if (p > 0f) drawCircle(tone.content.copy(alpha = (1f - p) * 0.6f), r * (1f + p * 0.9f), c, style = Stroke(w * 0.016f))
                    drawCircle(tone.container, r, c)
                    drawCircle(tone.content, r, c, style = Stroke(w * 0.022f))
                    drawCircle(tone.content, r * 0.32f, c)
                }
            }
        }
        if (onZoneClick != null) {
            ZoneHit(CarZone.FRONT, selected, onZoneClick, Modifier.fillMaxWidth().fillMaxHeight(0.22f).align(Alignment.TopCenter))
            ZoneHit(CarZone.REAR, selected, onZoneClick, Modifier.fillMaxWidth().fillMaxHeight(0.22f).align(Alignment.BottomCenter))
            ZoneHit(CarZone.LEFT, selected, onZoneClick, Modifier.fillMaxWidth(0.3f).fillMaxHeight(0.56f).align(Alignment.CenterStart))
            ZoneHit(CarZone.RIGHT, selected, onZoneClick, Modifier.fillMaxWidth(0.3f).fillMaxHeight(0.56f).align(Alignment.CenterEnd))
            ZoneHit(CarZone.INTERIOR, selected, onZoneClick, Modifier.fillMaxWidth(0.4f).fillMaxHeight(0.4f).align(Alignment.Center))
        }
    }
}

/** 낮·밤 차체 색 (금속 느낌을 내는 가장자리·중간·하이라이트 세 단계) */
private data class CarPaint(
    val bodyEdge: Color,
    val bodyMid: Color,
    val bodyHighlight: Color,
    val outline: Color,
    val crease: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val tire: Color,
    val headlight: Color,
    val taillight: Color,
    val lampStrip: Color,
    val shadowAlpha: Float,
    val reflection: Float,
) {
    companion object {
        val Light = CarPaint(
            bodyEdge = Color(0xFFB7C2C8), bodyMid = Color(0xFFDCE3E6), bodyHighlight = Color(0xFFF7F9FA),
            outline = Color(0xFF8E9BA2), crease = Color(0x338E9BA2),
            glassTop = Color(0xFF3A4A54), glassBottom = Color(0xFF1C272E),
            tire = Color(0xFF2A3136), headlight = Color(0xFFDDF3FF), taillight = Color(0xFFFF4D52), lampStrip = Color(0xFF26323A),
            shadowAlpha = 0.12f, reflection = 0.28f,
        )
        val Dark = CarPaint(
            bodyEdge = Color(0xFF2C363B), bodyMid = Color(0xFF46535A), bodyHighlight = Color(0xFF6B7A82),
            outline = Color(0xFF7C8D95), crease = Color(0x337C8D95),
            glassTop = Color(0xFF18232A), glassBottom = Color(0xFF0C1216),
            tire = Color(0xFF0B0F12), headlight = Color(0xFFCFF3FF), taillight = Color(0xFFFF5A5F), lampStrip = Color(0xFF0E1418),
            shadowAlpha = 0.35f, reflection = 0.18f,
        )
    }
}

/** 차체 윤곽: 왼쪽 반을 그리고 좌우를 뒤집어 붙입니다. */
private fun bodyPath(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.5f, h * 0.025f)
    cubicTo(w * 0.30f, h * 0.025f, w * 0.17f, h * 0.035f, w * 0.15f, h * 0.10f) // 앞 모서리
    cubicTo(w * 0.135f, h * 0.18f, w * 0.13f, h * 0.30f, w * 0.135f, h * 0.40f) // 앞바퀴 쪽
    cubicTo(w * 0.14f, h * 0.48f, w * 0.14f, h * 0.56f, w * 0.135f, h * 0.66f) // 허리
    cubicTo(w * 0.13f, h * 0.80f, w * 0.14f, h * 0.93f, w * 0.20f, h * 0.965f) // 뒤 모서리
    cubicTo(w * 0.30f, h * 0.978f, w * 0.70f, h * 0.978f, w * 0.80f, h * 0.965f) // 뒤 끝
    cubicTo(w * 0.86f, h * 0.93f, w * 0.87f, h * 0.80f, w * 0.865f, h * 0.66f)
    cubicTo(w * 0.86f, h * 0.56f, w * 0.86f, h * 0.48f, w * 0.865f, h * 0.40f)
    cubicTo(w * 0.87f, h * 0.30f, w * 0.865f, h * 0.18f, w * 0.85f, h * 0.10f)
    cubicTo(w * 0.83f, h * 0.035f, w * 0.70f, h * 0.025f, w * 0.5f, h * 0.025f)
    close()
}

private fun trapezoid(bottomLeft: Float, bottomRight: Float, bottomY: Float, topLeft: Float, topRight: Float, topY: Float): Path =
    Path().apply {
        moveTo(bottomLeft, bottomY)
        lineTo(bottomRight, bottomY)
        lineTo(topRight, topY)
        lineTo(topLeft, topY)
        close()
    }

/** 아이오닉 느낌의 픽셀 램프: 작은 네모 네 개 */
private fun DrawScope.pixels(x: Float, y: Float, w: Float, color: Color) {
    val s = w * 0.026f
    val gap = w * 0.012f
    repeat(4) { i ->
        drawRect(color, Offset(x + i * (s + gap), y), Size(s, s))
    }
}

@Composable
private fun BoxScope.ZoneHit(zone: CarZone, selected: Set<CarZone>, onClick: (CarZone) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .semantics { contentDescription = "차 ${zone.label}" }
            .toggleable(value = zone in selected, role = Role.Checkbox) { onClick(zone) },
    )
}

private fun zoneCenter(zone: CarZone, w: Float, h: Float): Offset = when (zone) {
    CarZone.FRONT -> Offset(w * 0.5f, h * 0.13f)
    CarZone.REAR -> Offset(w * 0.5f, h * 0.88f)
    CarZone.LEFT -> Offset(w * 0.15f, h * 0.53f)
    CarZone.RIGHT -> Offset(w * 0.85f, h * 0.53f)
    CarZone.INTERIOR -> Offset(w * 0.5f, h * 0.53f)
}

/** 차 그림 가로:세로 비율 (1:1.6). Modifier.aspectRatio(CarTopViewAspect) 로 씁니다. */
const val CarTopViewAspect = 1f / 1.6f
