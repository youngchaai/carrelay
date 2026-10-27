package com.carrelay.vehicle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/** 차량 화면용 어두운 테마: 야간 눈부심을 줄이고 큰 글자를 씁니다. */
private val CarColors = darkColorScheme(
    primary = Color(0xFF7FD6C2),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0F5257),
    onPrimaryContainer = Color(0xFFCFF5EC),
    secondary = Color(0xFFB7C8D6),
    onSecondary = Color(0xFF213240),
    secondaryContainer = Color(0xFF2B3A45),
    onSecondaryContainer = Color(0xFFD3E4F2),
    tertiary = Color(0xFFFFC870),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDFA8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1418),
    onBackground = Color(0xFFE3E8EC),
    surface = Color(0xFF161D22),
    onSurface = Color(0xFFE3E8EC),
    surfaceVariant = Color(0xFF222B31),
    onSurfaceVariant = Color(0xFFB9C4CB),
    outline = Color(0xFF6B777E),
)

private val CarTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontSize = 44.sp, lineHeight = 52.sp),
        headlineLarge = t.headlineLarge.copy(fontSize = 38.sp, lineHeight = 46.sp),
        headlineMedium = t.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp),
        titleLarge = t.titleLarge.copy(fontSize = 26.sp, lineHeight = 34.sp),
        titleMedium = t.titleMedium.copy(fontSize = 22.sp, lineHeight = 30.sp),
        bodyLarge = t.bodyLarge.copy(fontSize = 21.sp, lineHeight = 30.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        labelLarge = t.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
    )
}

@Composable
fun CarRelayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CarColors, typography = CarTypography, content = content)
}
