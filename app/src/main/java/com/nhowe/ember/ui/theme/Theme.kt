package com.nhowe.ember.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.lerp
import com.nhowe.ember.domain.model.AccentTheme
import com.nhowe.ember.domain.model.ThemeMode

/** Semantic colors that Material's scheme has no slot for. */
data class EmberColors(
    val perfect: Color,
    val hit: Color,
    val miss: Color,
    val frozen: Color,
    val xp: Color,
    val rest: Color,
    val ringTrack: Color,
)

val LocalEmberColors = staticCompositionLocalOf {
    EmberColors(Gold, Mint, Rose, Ice, Violet, NightTextMuted, NightSurfaceHighest)
}

private val DarkScheme = darkColorScheme(
    primary = EmberOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A2314),
    onPrimaryContainer = Color(0xFFFFD9CB),
    secondary = Gold,
    onSecondary = Color(0xFF3B2A00),
    secondaryContainer = Color(0xFF3F3312),
    onSecondaryContainer = Color(0xFFFFE8B0),
    tertiary = Violet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF32245C),
    onTertiaryContainer = Color(0xFFE6DBFF),
    background = Night,
    onBackground = NightText,
    surface = Night,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightTextMuted,
    surfaceContainerLowest = Color(0xFF0A0B10),
    surfaceContainerLow = Color(0xFF121420),
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    outline = NightOutline,
    outlineVariant = Color(0xFF2B2F40),
    error = Rose,
    onError = Color.White,
)

private val LightScheme = lightColorScheme(
    primary = EmberOrangeDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDACB),
    onPrimaryContainer = Color(0xFF3A1200),
    secondary = Color(0xFF9A6A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6A8),
    onSecondaryContainer = Color(0xFF2A1F00),
    tertiary = Color(0xFF6D3FD9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9DDFF),
    onTertiaryContainer = Color(0xFF23005C),
    background = Day,
    onBackground = DayText,
    surface = Day,
    onSurface = DayText,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = DayTextMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2EC),
    surfaceContainer = DaySurface,
    surfaceContainerHigh = DaySurfaceHigh,
    surfaceContainerHighest = DaySurfaceHighest,
    outline = DayOutline,
    outlineVariant = Color(0xFFE6DFD5),
    error = Color(0xFFD1264A),
    onError = Color.White,
)

private fun accented(base: androidx.compose.material3.ColorScheme, accent: AccentTheme, dark: Boolean): androidx.compose.material3.ColorScheme {
    if (accent == AccentTheme.EMBER) return base
    val p = Color(accent.primary); val s = Color(accent.secondary); val t = Color(accent.tertiary)
    val bg = if (dark) Night else Day
    val fg = if (dark) Color.White else Color(0xFF1B1A22)
    fun container(c: Color) = lerp(c, bg, if (dark) 0.72f else 0.78f)
    fun onContainer(c: Color) = lerp(c, fg, 0.75f)
    val primary = if (dark) p else lerp(p, Color.Black, 0.18f)
    return base.copy(
        primary = primary, onPrimary = Color.White, primaryContainer = container(p), onPrimaryContainer = onContainer(p),
        secondary = if (dark) s else lerp(s, Color.Black, 0.3f), onSecondary = if (dark) Color(0xFF1B1A22) else Color.White, secondaryContainer = container(s), onSecondaryContainer = onContainer(s),
        tertiary = t, onTertiary = Color.White, tertiaryContainer = container(t), onTertiaryContainer = onContainer(t),
    )
}

@Composable
fun EmberTheme(
    mode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    accent: AccentTheme = AccentTheme.EMBER,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> accented(DarkScheme, accent, true)
        else -> accented(LightScheme, accent, false)
    }
    val emberColors = if (dark) {
        EmberColors(perfect = Gold, hit = Mint, miss = Rose, frozen = Ice, xp = Violet, rest = NightTextMuted, ringTrack = NightSurfaceHighest)
    } else {
        EmberColors(perfect = Color(0xFFE0A100), hit = Color(0xFF15A56F), miss = Color(0xFFD1264A), frozen = Color(0xFF0E8BC7), xp = Color(0xFF6D3FD9), rest = DayTextMuted, ringTrack = DaySurfaceHighest)
    }
    CompositionLocalProvider(LocalEmberColors provides emberColors) {
        MaterialTheme(colorScheme = scheme, typography = EmberTypography, shapes = EmberShapes, content = content)
    }
}

val MaterialTheme.ember: EmberColors
    @Composable get() = LocalEmberColors.current
