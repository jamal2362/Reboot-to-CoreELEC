package com.jamal2367.coreelec.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The red of the CoreELEC logo. */
val CoreElecRed = Color(0xFFE30613)

val StatusGreen = Color(0xFF6DD58C)

// Always dark: the app is made for TV boxes. Tones are derived from the CoreELEC red.
private val ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFB9C3FF),
    onTertiary = Color(0xFF222C61),
    tertiaryContainer = Color(0xFF394379),
    onTertiaryContainer = Color(0xFFDEE0FF),
    error = Color(0xFFFFB95C),
    onError = Color(0xFF462A00),
    errorContainer = Color(0xFF643F00),
    onErrorContainer = Color(0xFFFFDDB7),
    background = Color(0xFF161820),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF161820),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF464B65),
    onSurfaceVariant = Color(0xFFC5C7D6),
    surfaceContainerLowest = Color(0xFF101218),
    surfaceContainerLow = Color(0xFF1B1D27),
    surfaceContainer = Color(0xFF20222F),
    surfaceContainerHigh = Color(0xFF2A2D3C),
    surfaceContainerHighest = Color(0xFF343850),
    outline = Color(0xFF8F92A6),
    outlineVariant = Color(0xFF464B65),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = ColorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
