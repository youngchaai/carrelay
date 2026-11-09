package com.carrelay.vehicle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.carrelay.vehicle.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 브랜드: 앱 아이콘(청록 바탕에 흰·민트 화살표 두 개가 이어 달림)과 영문 워드마크 CARRELAY.
 * 워드마크 글꼴은 시연 영상 로고와 같은 Montserrat SemiBold(SIL OFL, third_party/montserrat).
 */
object Brand {
    val Teal = Color(0xFF006B60)
    val Mint = Color(0xFF7FD6C2)
    /** 시작 화면 바탕: 아주 어두운 청록 (위가 조금 밝음) */
    val NightTop = Color(0xFF0B2226)
    val Night = Color(0xFF061416)
    const val NAME = "CARRELAY"
    const val TAGLINE = "HAND OVER  ·  DRIVE ON"
}

val Montserrat = FontFamily(Font(R.font.montserrat_semibold, FontWeight.SemiBold))

/** 넓은 자간의 대문자 워드마크 */
fun wordmarkStyle(size: TextUnit, spacing: TextUnit = 0.24.em) = TextStyle(
    fontFamily = Montserrat,
    fontWeight = FontWeight.SemiBold,
    fontSize = size,
    letterSpacing = spacing,
)

/** 앱 아이콘과 같은 표시: 청록 둥근 네모 + 위(흰)·아래(민트) 화살표 */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Brand.Teal)) {
        // 아이콘 벡터(108 단위)에서 화살표가 있는 30..78 x 32..82 부분을 네모 안쪽 64%에 맞춥니다.
        val box = this.size.minDimension
        val scale = box * 0.64f / 50f
        val ox = (box - 48f * scale) / 2f - 30f * scale
        val oy = (box - 50f * scale) / 2f - 32f * scale
        fun p(x: Float, y: Float) = Offset(ox + x * scale, oy + y * scale)
        fun arrow(points: List<Offset>) = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(arrow(listOf(p(30f, 40f), p(62f, 40f), p(62f, 32f), p(78f, 46f), p(62f, 60f), p(62f, 52f), p(30f, 52f))), Color.White)
        drawPath(arrow(listOf(p(78f, 62f), p(46f, 62f), p(46f, 54f), p(30f, 68f), p(46f, 82f), p(46f, 74f), p(78f, 74f))), Brand.Mint)
    }
}

/** 위쪽 메뉴 왼쪽의 로고: 표시 + CARRELAY. 좁은 화면이면 표시만 */
@Composable
fun BrandLockup(compact: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "카릴레이" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(40.dp)
        if (!compact) {
            Text(Brand.NAME, style = wordmarkStyle(26.sp, 0.14.em), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * 시작 화면 (앱을 처음 켤 때 한 번): 어두운 청록 바탕에 흐린 CARRELAY 가 보이고,
 * 빛줄기가 왼쪽에서 오른쪽으로 지나가며 글자를 밝힌 뒤 한 줄 소개가 떠오릅니다. 약 2.5초, 누르면 바로 넘어갑니다.
 * [ready] 가 false 인 동안(저장된 기록을 읽는 중)은 마지막 장면에서 기다립니다.
 */
@Composable
fun LaunchSplash(ready: Boolean, onDone: () -> Unit, onLeaving: () -> Unit = {}) {
    val readyNow by rememberUpdatedState(ready)
    val done by rememberUpdatedState(onDone)
    val leavingNow by rememberUpdatedState(onLeaving)
    val ghost = remember { Animatable(0f) }
    val sweep = remember { Animatable(-0.12f) }
    val streak = remember { Animatable(0f) }
    val trail = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var leaving by remember { mutableStateOf(false) }
    fun leave() {
        if (leaving) return
        leaving = true
        // 사라지기 시작할 때 뒤의 화면을 그려, 첫 화면의 등장 효과가 시작 화면에 가려지지 않게 합니다.
        leavingNow()
        scope.launch {
            fade.animateTo(0f, tween(380))
            done()
        }
    }
    LaunchedEffect(Unit) {
        launch { ghost.animateTo(1f, tween(400)) }
        delay(250)
        launch { streak.animateTo(1f, tween(150)) }
        sweep.animateTo(1.12f, tween(900, easing = FastOutSlowInEasing))
        launch { streak.animateTo(0f, tween(300)) }
        launch { trail.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        tagline.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(650)
        while (!readyNow) delay(50)
        leave()
    }

    Box(
        Modifier
            .fillMaxSize()
            .alpha(fade.value)
            .background(Brush.verticalGradient(listOf(Brand.NightTop, Brand.Night)))
            .drawBehind {
                // 가운데 아주 옅은 청록 빛 (번진 색 얼룩처럼 보이지 않게 약하게)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Brand.Teal.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.46f),
                        radius = size.minDimension * 0.62f,
                    ),
                    radius = size.minDimension * 0.62f,
                    center = Offset(size.width / 2, size.height * 0.46f),
                )
            }
            .clipToBounds()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { leave() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val style = wordmarkStyle(88.sp)
            Box(
                Modifier.drawWithContent {
                    drawContent()
                    val x = sweep.value * size.width
                    val y = size.height / 2
                    // 빛줄기: 지나온 쪽으로 꼬리가 길게, 앞쪽은 밝은 점
                    if (streak.value > 0f) {
                        val tail = 260.dp.toPx()
                        drawLine(
                            Brush.horizontalGradient(listOf(Color.Transparent, Brand.Mint.copy(alpha = 0.5f * streak.value)), startX = x - tail, endX = x),
                            Offset(x - tail, y), Offset(x, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                        )
                        drawCircle(
                            Brush.radialGradient(listOf(Color.White.copy(alpha = streak.value), Brand.Mint.copy(alpha = 0.35f * streak.value), Color.Transparent), center = Offset(x, y), radius = 20.dp.toPx()),
                            radius = 20.dp.toPx(), center = Offset(x, y),
                        )
                    }
                    // 지나간 뒤 오른쪽으로 남는 가는 선
                    if (trail.value > 0f) {
                        val start = size.width + 18.dp.toPx()
                        drawLine(
                            Brush.horizontalGradient(listOf(Brand.Mint.copy(alpha = 0.55f * (1f - tagline.value * 0.6f)), Color.Transparent), startX = start, endX = start + 220.dp.toPx() * trail.value),
                            Offset(start, y), Offset(start + 220.dp.toPx() * trail.value, y), strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            ) {
                // 흐린 글자 위에 밝은 글자를 빛줄기 위치까지만 보여 줍니다.
                Text(Brand.NAME, style = style, color = Color.White.copy(alpha = 0.10f * ghost.value))
                Text(
                    Brand.NAME,
                    style = style,
                    color = Color.White,
                    modifier = Modifier.drawWithContent {
                        clipRect(right = (sweep.value * size.width).coerceAtLeast(0f)) { this@drawWithContent.drawContent() }
                    },
                )
            }
            Text(
                Brand.TAGLINE,
                // 자간을 너무 벌린 대문자 문구는 AI 화면 같은 느낌이라 로고보다 조금만 넓게
                style = wordmarkStyle(22.sp, 0.16.em),
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .alpha(tagline.value)
                    .offset(y = (18 + 10 * (1f - tagline.value)).dp),
            )
        }
    }
}
