package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    CONSOLE_DARK,
    CONSOLE_LIGHT,
    SYSTEM
}

private val ConsoleDarkColorScheme = darkColorScheme(
    primary = RecRed,
    onPrimary = Color.White,
    secondary = AmberAccent,
    onSecondary = GroundDark,
    background = GroundDark,
    onBackground = TextCream,
    surface = PanelDark,
    onSurface = TextCream,
    surfaceVariant = Panel2Dark,
    onSurfaceVariant = TextMutedDark,
    outline = LineDark
)

private val ConsoleLightColorScheme = lightColorScheme(
    primary = RecRed,
    onPrimary = Color.White,
    secondary = AmberAccent,
    onSecondary = GroundDark,
    background = GroundLight,
    onBackground = TextDark,
    surface = PanelLight,
    onSurface = TextDark,
    surfaceVariant = Panel2Light,
    onSurfaceVariant = TextMutedLight,
    outline = LineLight
)

@Composable
fun SoundboardTheme(
    themeMode: ThemeMode = ThemeMode.CONSOLE_DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.CONSOLE_DARK -> true
        ThemeMode.CONSOLE_LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) ConsoleDarkColorScheme else ConsoleLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

