package com.carrelay.vehicle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carrelay.vehicle.R

/*
 * 카릴레이 디자인 시스템 (Pleos Connect 2560x1440 @160dpi 기준, 1dp = 1px)
 * - 낮: 밝은 테마 (Pleos 시스템 화면과 톤을 맞춤) / 밤: 어두운 테마. isSystemInDarkTheme() 로 전환.
 * - 의미 색: 경고(주의) / 사람 메모 / 정상 / 안내 / 위험. 색만으로 구분하지 않고 아이콘·라벨을 함께 씁니다.
 * - 글자: 본문 28sp 이상, 주행 핵심 수치 64sp. (GPT 디자인 검토 + Claude 검토를 합친 기준)
 */

/** 글자·아이콘 색과 배경 색 한 쌍 */
@Immutable
data class StatusTone(val content: Color, val container: Color)

@Immutable
data class StatusColors(
    val warning: StatusTone,
    val memo: StatusTone,
    val ok: StatusTone,
    val info: StatusTone,
    val danger: StatusTone,
    /** 운전자 아바타 색 (상태 의미 없음) */
    val avatars: List<StatusTone>,
)

private val LightStatus = StatusColors(
    warning = StatusTone(Color(0xFF704900), Color(0xFFFFF0CC)),
    memo = StatusTone(Color(0xFF503C91), Color(0xFFEEE7FF)),
    ok = StatusTone(Color(0xFF17613D), Color(0xFFDFF3E7)),
    info = StatusTone(Color(0xFF205D85), Color(0xFFE0F0FC)),
    danger = StatusTone(Color(0xFFA12328), Color(0xFFFDE7E7)),
    avatars = listOf(
        StatusTone(Color(0xFF004D44), Color(0xFFD7F2EB)),
        StatusTone(Color(0xFF1F4E79), Color(0xFFDCEBFA)),
        StatusTone(Color(0xFF7A2944), Color(0xFFF9E1E6)),
        StatusTone(Color(0xFF5B4421), Color(0xFFF3E8D6)),
    ),
)

private val DarkStatus = StatusColors(
    warning = StatusTone(Color(0xFFFFD58A), Color(0xFF493500)),
    memo = StatusTone(Color(0xFFD4C5FF), Color(0xFF352954)),
    ok = StatusTone(Color(0xFF9CD9B5), Color(0xFF173D2B)),
    info = StatusTone(Color(0xFFACD6F5), Color(0xFF19394F)),
    danger = StatusTone(Color(0xFFFFB4AB), Color(0xFF522329)),
    avatars = listOf(
        StatusTone(Color(0xFFD7F2EB), Color(0xFF164F48)),
        StatusTone(Color(0xFFD6E8F8), Color(0xFF1E3A55)),
        StatusTone(Color(0xFFF6D6DE), Color(0xFF4A2330)),
        StatusTone(Color(0xFFF0E2C8), Color(0xFF3F3322)),
    ),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7F2EB),
    onPrimaryContainer = Color(0xFF004D44),
    secondary = Color(0xFF465B64),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EEF0),
    onSecondaryContainer = Color(0xFF17262C),
    tertiary = Color(0xFF704900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF0CC),
    onTertiaryContainer = Color(0xFF3D2800),
    error = Color(0xFFA12328),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF5C0F12),
    background = Color(0xFFF5F7F8),
    onBackground = Color(0xFF17262C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17262C),
    surfaceVariant = Color(0xFFE8EEF0),
    onSurfaceVariant = Color(0xFF465B64),
    surfaceContainer = Color(0xFFEEF2F3),
    surfaceContainerHigh = Color(0xFFE8EEF0),
    outline = Color(0xFF687D86),
    outlineVariant = Color(0xFFD3DDE0),
    inverseSurface = Color(0xFF17262C),
    inverseOnSurface = Color(0xFFEDF3F5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5C5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF164F48),
    onPrimaryContainer = Color(0xFFD7F2EB),
    secondary = Color(0xFFB7C8CF),
    onSecondary = Color(0xFF17262C),
    secondaryContainer = Color(0xFF243136),
    onSecondaryContainer = Color(0xFFEDF3F5),
    tertiary = Color(0xFFFFD58A),
    onTertiary = Color(0xFF3D2800),
    tertiaryContainer = Color(0xFF493500),
    onTertiaryContainer = Color(0xFFFFF0CC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF522329),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101619),
    onBackground = Color(0xFFEDF3F5),
    surface = Color(0xFF192226),
    onSurface = Color(0xFFEDF3F5),
    surfaceVariant = Color(0xFF243136),
    onSurfaceVariant = Color(0xFFB7C8CF),
    surfaceContainer = Color(0xFF1E282C),
    surfaceContainerHigh = Color(0xFF243136),
    outline = Color(0xFF7C929B),
    outlineVariant = Color(0xFF34454B),
    inverseSurface = Color(0xFFEDF3F5),
    inverseOnSurface = Color(0xFF17262C),
)

/** Pretendard (SIL OFL 1.1, third_party/pretendard). 한글·숫자 폭이 고르고 작은 크기에서도 또렷합니다. */
private val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, numbers: Boolean = false) =
    TextStyle(
        fontFamily = Pretendard,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        // 큰 수치는 숫자 폭을 고정(tnum)해 값이 바뀔 때 글자가 흔들리지 않게 합니다.
        fontFeatureSettings = if (numbers) "tnum" else null,
        letterSpacing = if (size >= 44) (-0.5).sp else 0.sp,
        // 한국어를 글자 단위가 아니라 어절(띄어쓰기) 단위로 줄바꿈합니다("주세\n요" 방지, Android 13+).
        lineBreak = KoreanLineBreak,
    )

private val KoreanLineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase,
)

/** 160dpi 차량 화면용 글자 크기 (sp = px) */
private val CarTypography = Typography(
    displayLarge = style(76, 88, FontWeight.Bold, numbers = true),
    displayMedium = style(64, 76, FontWeight.Bold, numbers = true), // 주행 핵심 수치, 안부 질문
    displaySmall = style(52, 64, FontWeight.Bold, numbers = true),
    headlineLarge = style(44, 56, FontWeight.Bold),
    headlineMedium = style(38, 50, FontWeight.Bold), // 화면 제목
    headlineSmall = style(34, 44, FontWeight.Bold),
    titleLarge = style(30, 40, FontWeight.Bold), // 카드 제목
    titleMedium = style(28, 38, FontWeight.SemiBold),
    titleSmall = style(26, 34, FontWeight.SemiBold),
    bodyLarge = style(28, 40), // 본문 기본
    bodyMedium = style(24, 34), // 보조 설명·시간
    bodySmall = style(22, 30),
    labelLarge = style(28, 36, FontWeight.SemiBold), // 버튼
    labelMedium = style(24, 30, FontWeight.SemiBold), // 배지·작은 라벨
    labelSmall = style(22, 28, FontWeight.Medium),
)

private val CarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** 간격·크기 기준 */
object Dimens {
    val page = 32.dp
    val gap = 24.dp
    val gapS = 16.dp
    val gapXs = 8.dp
    val touch = 88.dp
    val button = 104.dp
    val drivingButton = 152.dp
    val cardRadius = 24.dp
    val buttonRadius = 20.dp
    val cardPadding = 28.dp
}

private val LocalStatusColors = staticCompositionLocalOf { LightStatus }

object CarRelayTheme {
    val status: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}

@Composable
fun CarRelayTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CarTypography,
            shapes = CarShapes,
            content = content,
        )
    }
}
