package com.nijatva.appstore.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------- Theme state model ---------- */

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
    BLACK("Black"),
}

/**
 * Accent colours. Every accent carries its own "on" colour so that a filled chip or button
 * keeps a WCAG AA contrast ratio (>= 4.5:1) for normal-size text:
 *  - Green  #0B7A52 on white  = 5.36:1
 *  - Saffron #EE6E00 on #111  = 6.17:1  (white would only give 3.06:1, so text is near-black)
 *  - Indigo #1C1A7E on white  = 14.01:1
 */
enum class Accent(val label: String) {
    GREEN("Green"),
    SAFFRON("Saffron"),
    INDIGO("Indigo");

    /** The same three accents the signature library uses, so the suite reads as one. */
    val primary: Color
        get() = when (this) {
            GREEN -> Color(0xFF1E7A46)
            SAFFRON -> Color(0xFFD98431)
            INDIGO -> Color(0xFF3F45A8)
        }

    /**
     * Label colour on a filled chip, picked by relative luminance with the library's
     * 0.179 cut-off: the point where black and white have equal contrast.
     */
    val onPrimary: Color
        get() = if (relativeLuminance(primary) > 0.179) Color(0xFF101820) else Color(0xFFFFFFFF)

    val container: Color
        get() = when (this) {
            GREEN -> Color(0xFFD3EFE3)
            SAFFRON -> Color(0xFFFFE2CC)
            INDIGO -> Color(0xFFDEDDFF)
        }

    val onContainer: Color
        get() = when (this) {
            GREEN -> Color(0xFF06291C)
            SAFFRON -> Color(0xFF3A1A00)
            INDIGO -> Color(0xFF0D0C40)
        }
}


/** WCAG relative luminance of a colour, in the same form the signature library uses. */
fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.GREEN,
)

/* ---------- Colour scheme builder (pure, unit-testable) ---------- */

fun isDarkMode(mode: ThemeMode, systemInDark: Boolean): Boolean = when (mode) {
    ThemeMode.DARK, ThemeMode.BLACK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemInDark
}

fun colorSchemeFor(mode: ThemeMode, accent: Accent, systemInDark: Boolean): ColorScheme {
    val dark = isDarkMode(mode, systemInDark)
    return if (dark) {
        val background = if (mode == ThemeMode.BLACK) Color(0xFF000000) else Color(0xFF121212)
        val surface = if (mode == ThemeMode.BLACK) Color(0xFF000000) else Color(0xFF171717)
        darkColorScheme(
            primary = accent.primary,
            onPrimary = accent.onPrimary,
            primaryContainer = accent.primary,
            onPrimaryContainer = accent.onPrimary,
            secondary = accent.primary,
            onSecondary = accent.onPrimary,
            background = background,
            onBackground = Color(0xFFE8E8E8),
            surface = surface,
            onSurface = Color(0xFFE8E8E8),
            surfaceVariant = if (mode == ThemeMode.BLACK) Color(0xFF141414) else Color(0xFF232323),
            onSurfaceVariant = Color(0xFFBDBDBD),
            outline = Color(0xFF3A3A3A),
            outlineVariant = Color(0xFF2A2A2A),
        )
    } else {
        lightColorScheme(
            primary = accent.primary,
            onPrimary = accent.onPrimary,
            primaryContainer = accent.container,
            onPrimaryContainer = accent.onContainer,
            secondary = accent.primary,
            onSecondary = accent.onPrimary,
            background = Color(0xFFF5F6F8),
            onBackground = Color(0xFF14181C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF14181C),
            surfaceVariant = Color(0xFFEDF0F3),
            onSurfaceVariant = Color(0xFF4A545E),
            outline = Color(0xFFC9D0D6),
            outlineVariant = Color(0xFFE1E6EA),
        )
    }
}

/* ---------- Design tokens ---------- */

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

val NijatvaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val NijatvaTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun NijatvaTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit,
) {
    val scheme = colorSchemeFor(
        mode = themeState.themeMode,
        accent = themeState.accent,
        systemInDark = isSystemInDarkTheme(),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = NijatvaTypography,
        shapes = NijatvaShapes,
        content = content,
    )
}
