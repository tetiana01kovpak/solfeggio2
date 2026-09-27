package io.github.tetiana01kovpak.solfeggio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.tetiana01kovpak.solfeggio.TONE_COLORS

val Cream = Color(0xFFFAF4E6)

@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
)

// Same cream-and-forest palette as the web page.
private val Light = Palette(
    dark = false,
    bg = Color(0xFFF3EAD8),
    surface = Color(0xFFFAF4E6),
    ink = Color(0xFF23402B),
    muted = Color(0xFF5D7563),
    line = Color(0x3823402B),
    accent = Color(0xFF3F6B4A),
)

private val Dark = Palette(
    dark = true,
    bg = Color(0xFF101A14),
    surface = Color(0xFF18261D),
    ink = Color(0xFFEDE5D2),
    muted = Color(0xFF9FB2A4),
    line = Color(0x29EDE5D2),
    accent = Color(0xFF6E9C7A),
)

val LocalPalette = staticCompositionLocalOf { Light }

fun toneColor(index: Int) = Color(TONE_COLORS[index])

/** A tone colour bright enough to draw lines with on the current background. */
fun Palette.lineTone(tone: Color) = if (dark) lerp(tone, Cream, 0.35f) else tone

@Composable
fun SolfeggioTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) Dark else Light
    val scheme = if (palette.dark) {
        darkColorScheme(primary = palette.accent, background = palette.bg, surface = palette.surface, onSurface = palette.ink)
    } else {
        lightColorScheme(primary = palette.accent, background = palette.bg, surface = palette.surface, onSurface = palette.ink)
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
