package com.carrelay.vehicle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.carrelay.core.model.CarZone
import com.carrelay.vehicle.ui.theme.StatusTone

/**
 * 위에서 내려다본 차 (앞이 위쪽). 사고·파손 위치를 점으로 보여 주고,
 * [onZoneClick] 을 주면 앞·뒤·왼쪽·오른쪽·실내를 눌러 고를 수 있습니다.
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
    val body = colors.surfaceContainer
    val outline = colors.outline
    val glass = colors.onSurfaceVariant.copy(alpha = 0.28f)
    val roof = colors.surface
    val tire = colors.onSurface.copy(alpha = 0.75f)
    val light = colors.primary.copy(alpha = 0.55f)
    val pick = selectedTone ?: StatusTone(colors.primary, colors.primaryContainer)

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = w * 0.018f)

            // 바퀴
            listOf(0.17f to 0.31f, 0.69f to 0.83f).forEach { (top, bottom) ->
                drawRoundRect(tire, Offset(w * 0.05f, h * top), Size(w * 0.11f, h * (bottom - top)), CornerRadius(w * 0.04f))
                drawRoundRect(tire, Offset(w * 0.84f, h * top), Size(w * 0.11f, h * (bottom - top)), CornerRadius(w * 0.04f))
            }
            // 사이드미러
            drawOval(outline, Offset(w * 0.04f, h * 0.285f), Size(w * 0.12f, h * 0.04f))
            drawOval(outline, Offset(w * 0.84f, h * 0.285f), Size(w * 0.12f, h * 0.04f))
            // 차체
            val bodyTopLeft = Offset(w * 0.12f, h * 0.035f)
            val bodySize = Size(w * 0.76f, h * 0.93f)
            val radius = CornerRadius(w * 0.24f, w * 0.24f)
            drawRoundRect(body, bodyTopLeft, bodySize, radius)
            drawRoundRect(outline, bodyTopLeft, bodySize, radius, style = stroke)
            // 지붕
            drawRoundRect(roof, Offset(w * 0.25f, h * 0.31f), Size(w * 0.5f, h * 0.45f), CornerRadius(w * 0.08f))
            // 앞유리·뒷유리
            drawTrapezoid(glass, w * 0.24f, w * 0.76f, h * 0.29f, w * 0.30f, w * 0.70f, h * 0.20f)
            drawTrapezoid(glass, w * 0.27f, w * 0.73f, h * 0.79f, w * 0.32f, w * 0.68f, h * 0.86f)
            // 전조등
            drawRoundRect(light, Offset(w * 0.22f, h * 0.05f), Size(w * 0.16f, h * 0.025f), CornerRadius(w * 0.02f))
            drawRoundRect(light, Offset(w * 0.62f, h * 0.05f), Size(w * 0.16f, h * 0.025f), CornerRadius(w * 0.02f))

            // 고른 위치 / 표시
            CarZone.entries.forEach { zone ->
                val c = zoneCenter(zone, w, h)
                val r = w * 0.085f
                if (zone in selected) {
                    drawCircle(pick.container, r * 1.25f, c)
                    drawCircle(pick.content, r * 1.25f, c, style = Stroke(w * 0.02f))
                }
                marks[zone]?.let { tone ->
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

@Composable
private fun BoxScope.ZoneHit(zone: CarZone, selected: Set<CarZone>, onClick: (CarZone) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .semantics { contentDescription = "차 ${zone.label}" }
            .toggleable(value = zone in selected, role = Role.Checkbox) { onClick(zone) },
    )
}

private fun zoneCenter(zone: CarZone, w: Float, h: Float): Offset = when (zone) {
    CarZone.FRONT -> Offset(w * 0.5f, h * 0.12f)
    CarZone.REAR -> Offset(w * 0.5f, h * 0.92f)
    CarZone.LEFT -> Offset(w * 0.15f, h * 0.53f)
    CarZone.RIGHT -> Offset(w * 0.85f, h * 0.53f)
    CarZone.INTERIOR -> Offset(w * 0.5f, h * 0.53f)
}

private fun DrawScope.drawTrapezoid(
    color: Color,
    bottomLeft: Float,
    bottomRight: Float,
    bottomY: Float,
    topLeft: Float,
    topRight: Float,
    topY: Float,
) {
    val path = Path().apply {
        moveTo(bottomLeft, bottomY)
        lineTo(bottomRight, bottomY)
        lineTo(topRight, topY)
        lineTo(topLeft, topY)
        close()
    }
    drawPath(path, color)
}

/** 차 그림 가로:세로 비율 (1:1.6). Modifier.aspectRatio(CarTopViewAspect) 로 씁니다. */
const val CarTopViewAspect = 1f / 1.6f
