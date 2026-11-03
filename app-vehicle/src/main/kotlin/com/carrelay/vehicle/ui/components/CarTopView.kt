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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
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
 * 그림: 부드러운 바닥 그림자 → 펜더 밖으로 살짝 보이는 바퀴 → 유선형 차체(가운데가 밝은 금속 느낌, 보닛 반사광)
 * → 한 장으로 이어진 유리(앞유리·파노라마 지붕·뒷유리, 하늘 반사) → 차체 곡선을 따라 이어진 얇은 앞·뒤 라이트.
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
        // 바닥 그림자: 차체 모양을 조금 내려 흐리게 (흐림은 Android 12 이상, 그 아래는 옅은 그림자만)
        Canvas(Modifier.fillMaxSize().blur(14.dp, BlurredEdgeTreatment.Unbounded)) {
            val w = size.width
            val h = size.height
            scale(1.03f, 1.01f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                translate(top = h * 0.014f) { drawPath(bodyPath(w, h), Color.Black.copy(alpha = paint.shadowAlpha * 2.4f)) }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val body = bodyPath(w, h)
            val canopy = canopyPath(w, h)

            // 바퀴: 펜더 밖으로 살짝만 보이게 (차체가 안쪽을 덮습니다)
            listOf(0.215f, 0.765f).forEach { cy ->
                val top = h * (cy - 0.068f)
                val len = h * 0.136f
                drawRoundRect(paint.tire, Offset(w * 0.152f, top), Size(w * 0.09f, len), CornerRadius(w * 0.035f))
                drawRoundRect(paint.tire, Offset(w * 0.758f, top), Size(w * 0.09f, len), CornerRadius(w * 0.035f))
            }

            // 사이드미러(카메라형): 앞유리 아래 끝에 붙은 작은 검은 날개
            drawPath(mirrorPath(w, h, left = true), paint.glassTop)
            drawPath(mirrorPath(w, h, left = false), paint.glassTop)

            // 차체: 가장자리는 어둡고 가운데로 갈수록 밝은 금속 느낌
            drawPath(
                body,
                Brush.horizontalGradient(
                    0f to paint.bodyEdge, 0.22f to paint.bodyMid, 0.5f to paint.bodyHighlight, 0.78f to paint.bodyMid, 1f to paint.bodyEdge,
                    startX = w * 0.17f, endX = w * 0.83f,
                ),
            )
            clipPath(body) {
                // 보닛·트렁크 가운데 반사광
                val sheen = Brush.horizontalGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = paint.reflection), 1f to Color.Transparent,
                    startX = w * 0.40f, endX = w * 0.60f,
                )
                drawRect(sheen, Offset(w * 0.40f, h * 0.05f), Size(w * 0.20f, h * 0.24f))
                drawRect(sheen, Offset(w * 0.40f, h * 0.81f), Size(w * 0.20f, h * 0.12f))
                // 위(앞)에서 비추는 빛: 앞쪽이 아주 조금 더 밝게
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = paint.reflection * 0.35f), Color.Transparent), startY = 0f, endY = h * 0.35f))
            }
            // 보닛 주름선 두 줄
            drawLine(paint.crease, Offset(w * 0.375f, h * 0.075f), Offset(w * 0.345f, h * 0.29f), strokeWidth = w * 0.006f, cap = StrokeCap.Round)
            drawLine(paint.crease, Offset(w * 0.625f, h * 0.075f), Offset(w * 0.655f, h * 0.29f), strokeWidth = w * 0.006f, cap = StrokeCap.Round)
            // 문 틈 (옆쪽에 짧게)
            listOf(0.475f, 0.625f).forEach { y ->
                drawLine(paint.crease, Offset(w * 0.200f, h * y), Offset(w * 0.262f, h * (y - 0.004f)), strokeWidth = w * 0.005f)
                drawLine(paint.crease, Offset(w * 0.800f, h * y), Offset(w * 0.738f, h * (y - 0.004f)), strokeWidth = w * 0.005f)
            }
            drawPath(body, paint.outline, style = Stroke(width = w * 0.008f))

            // 유리 한 장: 앞유리 → 파노라마 지붕 → 뒷유리
            drawPath(canopy, paint.glassBottom.copy(alpha = 0.45f), style = Stroke(width = w * 0.014f, join = StrokeJoin.Round)) // 옆 유리 가장자리
            drawPath(
                canopy,
                Brush.verticalGradient(
                    0f to paint.glassHigh, 0.22f to paint.glassTop, 0.75f to paint.glassBottom, 1f to paint.glassTop,
                    startY = h * 0.30f, endY = h * 0.80f,
                ),
            )
            clipPath(canopy) {
                // 하늘이 비친 사선 반사
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent, 0.45f to Color.White.copy(alpha = paint.reflection * 0.9f), 0.62f to Color.Transparent,
                        start = Offset(w * 0.20f, h * 0.30f), end = Offset(w * 0.75f, h * 0.62f),
                    ),
                )
            }
            // 앞유리·지붕·뒷유리 사이의 얇은 경계
            drawPath(arc(w, h, 0.276f, 0.432f, 0.422f), paint.pillar, style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))
            drawPath(arc(w, h, 0.292f, 0.690f, 0.700f), paint.pillar, style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))

            // 앞·뒤 라이트: 차체 곡선을 따라 끝까지 이어진 얇은 빛 (번짐 + 심지)
            val head = arc(w, h, 0.245f, 0.078f, 0.050f)
            val tail = arc(w, h, 0.240f, 0.932f, 0.956f)
            drawPath(head, paint.headGlow, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
            drawPath(head, paint.headlight, style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))
            drawPath(tail, paint.tailGlow, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
            drawPath(tail, paint.taillight, style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))

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
    val glassHigh: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val pillar: Color,
    val tire: Color,
    val headlight: Color,
    val headGlow: Color,
    val taillight: Color,
    val tailGlow: Color,
    val shadowAlpha: Float,
    val reflection: Float,
) {
    companion object {
        val Light = CarPaint(
            bodyEdge = Color(0xFFA9B6BE), bodyMid = Color(0xFFD6DEE3), bodyHighlight = Color(0xFFF9FBFC),
            outline = Color(0xCC8A98A0), crease = Color(0x2E6E7E87),
            glassHigh = Color(0xFF6C8190), glassTop = Color(0xFF34454F), glassBottom = Color(0xFF151D23), pillar = Color(0xFF52626C),
            tire = Color(0xFF20272C),
            headlight = Color(0xFFF2FBFF), headGlow = Color(0x5579D9FF),
            taillight = Color(0xFFFF3B47), tailGlow = Color(0x55FF3B47),
            shadowAlpha = 0.10f, reflection = 0.34f,
        )
        val Dark = CarPaint(
            bodyEdge = Color(0xFF263035), bodyMid = Color(0xFF414E55), bodyHighlight = Color(0xFF6D7D86),
            outline = Color(0xCC7C8D95), crease = Color(0x33A9B8C0),
            glassHigh = Color(0xFF3A4C57), glassTop = Color(0xFF18232A), glassBottom = Color(0xFF0A0F12), pillar = Color(0xFF2E3B42),
            tire = Color(0xFF07090B),
            headlight = Color(0xFFE6FAFF), headGlow = Color(0x6670D7FF),
            taillight = Color(0xFFFF4B55), tailGlow = Color(0x66FF4B55),
            shadowAlpha = 0.30f, reflection = 0.20f,
        )
    }
}

/** 왼쪽 반의 점(가로·세로 비율)을 좌우로 뒤집어 붙여 대칭 모양을 만듭니다. */
private class Half(val w: Float, val h: Float) {
    val path = Path()
    fun move(x: Float, y: Float) = path.moveTo(w * x, h * y)
    fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        path.cubicTo(w * x1, h * y1, w * x2, h * y2, w * x3, h * y3)
}

/**
 * 차체 윤곽: 좁은 코 → 앞 펜더가 살짝 부풀고 → 문 쪽이 조금 잘록하고 → 뒤 펜더가 다시 부푼 뒤 → 둥근 꼬리.
 * 실제 차처럼 가로:세로가 약 1:2.3 입니다.
 */
private fun bodyPath(w: Float, h: Float): Path {
    // 왼쪽 반: (앞 가운데) → … → (뒤 가운데). 각 줄은 cubic(제어점1, 제어점2, 끝점)
    val left = listOf(
        floatArrayOf(0.36f, 0.030f, 0.245f, 0.044f, 0.207f, 0.100f), // 코 → 앞 모서리
        floatArrayOf(0.188f, 0.140f, 0.175f, 0.190f, 0.176f, 0.245f), // 앞 펜더
        floatArrayOf(0.178f, 0.330f, 0.196f, 0.420f, 0.196f, 0.500f), // 앞문 쪽으로 잘록하게
        floatArrayOf(0.196f, 0.585f, 0.170f, 0.665f, 0.171f, 0.745f), // 뒤 펜더
        floatArrayOf(0.172f, 0.830f, 0.180f, 0.905f, 0.218f, 0.942f), // 뒤 모서리
        floatArrayOf(0.275f, 0.966f, 0.400f, 0.972f, 0.500f, 0.972f), // 꼬리
    )
    val p = Half(w, h)
    p.move(0.5f, 0.030f)
    left.forEach { c -> p.cubic(c[0], c[1], c[2], c[3], c[4], c[5]) }
    // 오른쪽 반: 같은 점을 거꾸로, x 를 뒤집어서
    val pts = mutableListOf(floatArrayOf(0.5f, 0.030f))
    left.forEach { pts += floatArrayOf(it[4], it[5]) }
    for (i in left.indices.reversed()) {
        val c = left[i]
        val start = pts[i]
        p.cubic(1f - c[2], c[3], 1f - c[0], c[1], 1f - start[0], start[1])
    }
    p.path.close()
    return p.path
}

/** 유리 한 장(앞유리 + 파노라마 지붕 + 뒷유리) */
private fun canopyPath(w: Float, h: Float): Path {
    val p = Half(w, h)
    p.move(0.5f, 0.302f)
    p.cubic(0.385f, 0.302f, 0.295f, 0.310f, 0.262f, 0.336f) // 앞유리 아래 곡선 → 왼쪽 A필러
    p.cubic(0.268f, 0.460f, 0.283f, 0.620f, 0.306f, 0.768f) // 왼쪽 옆선
    p.cubic(0.335f, 0.796f, 0.420f, 0.804f, 0.500f, 0.804f) // 뒷유리 아래 곡선
    p.cubic(0.580f, 0.804f, 0.665f, 0.796f, 0.694f, 0.768f)
    p.cubic(0.717f, 0.620f, 0.732f, 0.460f, 0.738f, 0.336f)
    p.cubic(0.705f, 0.310f, 0.615f, 0.302f, 0.500f, 0.302f)
    p.path.close()
    return p.path
}

/** 가운데가 볼록한 가로 곡선: 양 끝 x(왼쪽 기준), 양 끝 y, 가운데 y */
private fun arc(w: Float, h: Float, xLeft: Float, yEnds: Float, yMid: Float): Path = Path().apply {
    moveTo(w * xLeft, h * yEnds)
    quadraticTo(w * 0.5f, h * (2 * yMid - yEnds), w * (1f - xLeft), h * yEnds)
}

/** 앞유리 아래 끝에 붙은 사이드미러 */
private fun mirrorPath(w: Float, h: Float, left: Boolean): Path = Path().apply {
    fun x(v: Float) = w * (if (left) v else 1f - v)
    moveTo(x(0.205f), h * 0.330f)
    cubicTo(x(0.170f), h * 0.326f, x(0.130f), h * 0.330f, x(0.122f), h * 0.343f)
    cubicTo(x(0.120f), h * 0.355f, x(0.160f), h * 0.360f, x(0.205f), h * 0.358f)
    close()
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
    CarZone.LEFT -> Offset(w * 0.19f, h * 0.53f)
    CarZone.RIGHT -> Offset(w * 0.81f, h * 0.53f)
    CarZone.INTERIOR -> Offset(w * 0.5f, h * 0.53f)
}

/** 차 그림 가로:세로 비율 (1:1.6). Modifier.aspectRatio(CarTopViewAspect) 로 씁니다. */
const val CarTopViewAspect = 1f / 1.6f
