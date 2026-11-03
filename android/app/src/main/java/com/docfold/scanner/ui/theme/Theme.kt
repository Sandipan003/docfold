package com.docfold.scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.docfold.scanner.R

// ── Type ────────────────────────────────────────────────────────────────────────────────────────────
// Archivo carries everything: its condensed black cut is the display voice (titles, page numerals),
// its normal width is the UI voice. Martian Mono is reserved for data — dates, sizes, counts.

val ArchivoCondensed = FontFamily(
    Font(R.font.archivo_condensed_black, FontWeight.Black),
    Font(R.font.archivo_condensed_bold, FontWeight.Bold),
)
val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
)
val MartianMono = FontFamily(
    Font(R.font.martian_mono_regular, FontWeight.Normal),
    Font(R.font.martian_mono_semibold, FontWeight.SemiBold),
)

private fun display(size: Int, line: Float = 0.92f, weight: FontWeight = FontWeight.Black) = TextStyle(
    fontFamily = ArchivoCondensed, fontWeight = weight, fontSize = size.sp, lineHeight = (size * line).sp, letterSpacing = (-0.01).em,
)
private fun ui(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = Archivo, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em,
)

private val DocfoldTypography = Typography(
    displayLarge = display(64, 0.88f),
    displayMedium = display(48, 0.9f),
    displaySmall = display(38),
    headlineLarge = display(32, 1f),
    headlineMedium = display(26, 1.05f, FontWeight.Bold),
    headlineSmall = display(22, 1.1f, FontWeight.Bold),
    titleLarge = ui(20, 26, FontWeight.SemiBold),
    titleMedium = ui(16, 22, FontWeight.SemiBold),
    titleSmall = ui(14, 20, FontWeight.Medium),
    bodyLarge = ui(16, 24),
    bodyMedium = ui(14, 20),
    bodySmall = ui(12, 16),
    labelLarge = ui(15, 20, FontWeight.SemiBold, 0.01f),
    labelMedium = ui(12, 16, FontWeight.SemiBold, 0.02f),
    labelSmall = ui(11, 14, FontWeight.Medium, 0.03f),
)

/** Data styles: postal dates, sizes and counts. */
object Mono {
    val small = TextStyle(fontFamily = MartianMono, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.04.em)
    val medium = TextStyle(fontFamily = MartianMono, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.02.em)
    val strong = medium.copy(fontWeight = FontWeight.SemiBold)
}

// ── Colour ──────────────────────────────────────────────────────────────────────────────────────────

val AirmailRed = Color(0xFFD7262E)
val AirmailBlue = Color(0xFF1B3F8F)

private val Light = lightColorScheme(
    primary = AirmailRed, onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE3E2), onPrimaryContainer = Color(0xFF6E0E12),
    secondary = AirmailBlue, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E8F7), onSecondaryContainer = Color(0xFF0E2459),
    tertiary = Color(0xFF12825A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9F1E6), onTertiaryContainer = Color(0xFF053D29),
    background = Color(0xFFFBFBF8), onBackground = Color(0xFF14161A),
    surface = Color(0xFFFBFBF8), onSurface = Color(0xFF14161A),
    surfaceVariant = Color(0xFFEDEDE7), onSurfaceVariant = Color(0xFF52565F),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F6F2),
    surfaceContainer = Color(0xFFF1F1EC), surfaceContainerHigh = Color(0xFFEBEBE5), surfaceContainerHighest = Color(0xFFE4E4DE),
    outline = Color(0xFF8E9199), outlineVariant = Color(0xFFDADAD3),
    error = Color(0xFFB3261E), onError = Color.White,
    inverseSurface = Color(0xFF23262C), inverseOnSurface = Color(0xFFF2F2EE), inversePrimary = Color(0xFFFFB3AE),
    scrim = Color.Black,
)

// Night: a sorting office after hours — blue-black ground, the same airmail red for action.
private val Dark = darkColorScheme(
    // A brighter airmail red so red text keeps 4.5:1 on the night ground; ink-dark labels on red buttons.
    primary = Color(0xFFFF5A5F), onPrimary = Color(0xFF14161A),
    primaryContainer = Color(0xFF5A1216), onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFF9DB6FF), onSecondary = Color(0xFF0B1D47),
    secondaryContainer = Color(0xFF243A72), onSecondaryContainer = Color(0xFFDCE5FF),
    tertiary = Color(0xFF59CFA0), onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF0B4A33), onTertiaryContainer = Color(0xFFC9F2DF),
    background = Color(0xFF10131A), onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF10131A), onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF262B37), onSurfaceVariant = Color(0xFFADB2BE),
    surfaceContainerLowest = Color(0xFF0B0E14), surfaceContainerLow = Color(0xFF151922),
    surfaceContainer = Color(0xFF1A1F29), surfaceContainerHigh = Color(0xFF212733), surfaceContainerHighest = Color(0xFF2A303D),
    outline = Color(0xFF7D8494), outlineVariant = Color(0xFF353C4B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    inverseSurface = Color(0xFFECEDEF), inverseOnSurface = Color(0xFF23262C), inversePrimary = AirmailRed,
    scrim = Color.Black,
)

/** The six stamp inks. Each document owns one and wears it wherever it appears. */
@Immutable
data class Inks(val all: List<Color>) {
    operator fun get(i: Int) = all[((i % all.size) + all.size) % all.size]
}

private val LightInks = Inks(listOf(Color(0xFFE2452B), Color(0xFF2F4FD8), Color(0xFF12825A), Color(0xFFB87500), Color(0xFF7A3FB0), Color(0xFFD03C7A)))
private val DarkInks = Inks(listOf(Color(0xFFFF7A5C), Color(0xFF8EA2FF), Color(0xFF4CC79A), Color(0xFFF2B53A), Color(0xFFC49BF0), Color(0xFFFF86B8)))

val LocalInks = staticCompositionLocalOf { LightInks }
val LocalDark = staticCompositionLocalOf { false }

private val DocfoldShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemeMode { System, Light, Dark }

@Composable
fun DocfoldTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.System -> isSystemInDarkTheme(); ThemeMode.Light -> false; ThemeMode.Dark -> true }
    CompositionLocalProvider(LocalInks provides if (dark) DarkInks else LightInks, LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = DocfoldTypography, shapes = DocfoldShapes, content = content)
    }
}
