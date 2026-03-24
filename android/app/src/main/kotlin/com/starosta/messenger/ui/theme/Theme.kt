package com.starosta.messenger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkBackground,
    primaryContainer = MessageBubbleSent,
    onPrimaryContainer = TextPrimary,
    secondary = TealSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = TealDark,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ErrorRed,
    onError = DarkBackground,
    scrim = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = TealDark,
    onPrimary = LightBackground,
    primaryContainer = CyanLight,
    onPrimaryContainer = TealDark,
    secondary = TealSecondary,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = DarkBackground,
    surface = LightSurface,
    onSurface = DarkBackground,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextTertiary,
    outline = LightBorder,
    error = ErrorRed,
)

val LocalDarkTheme = compositionLocalOf { true }

@Composable
fun StarostaMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
