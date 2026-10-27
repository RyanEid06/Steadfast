package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SteadfastGold,
    onPrimary = SteadfastNavyDark,
    primaryContainer = SteadfastGoldContainer,
    onPrimaryContainer = SteadfastGoldLight,
    secondary = SteadfastGoldLight,
    onSecondary = SteadfastNavyDark,
    secondaryContainer = SteadfastNavySurfaceElevated,
    onSecondaryContainer = SteadfastCream,
    tertiary = SteadfastMutedText,
    onTertiary = SteadfastNavyDark,
    background = SteadfastNavyDark,
    onBackground = SteadfastCream,
    surface = SteadfastNavySurface,
    onSurface = SteadfastCream,
    surfaceVariant = SteadfastNavySurfaceVariant,
    onSurfaceVariant = SteadfastMutedText,
    outline = SteadfastNavyBorder,
    outlineVariant = SteadfastSubtleText,
    error = SteadfastStrugglingDark,
    onError = SteadfastCream,
    errorContainer = SteadfastStrugglingContainerDark,
    onErrorContainer = SteadfastGoldLight
)

private val LightColorScheme = lightColorScheme(
    primary = SteadfastGoldLightMode,
    onPrimary = SteadfastCreamLight,
    primaryContainer = SteadfastGoldContainerLight,
    onPrimaryContainer = SteadfastGoldLightMode,
    secondary = SteadfastGoldLightMode,
    onSecondary = SteadfastWhiteSurface,
    secondaryContainer = SteadfastLightSurfaceElevated,
    onSecondaryContainer = SteadfastInk,
    tertiary = SteadfastInkMuted,
    onTertiary = SteadfastWhiteSurface,
    background = SteadfastCreamLight,
    onBackground = SteadfastInk,
    surface = SteadfastWhiteSurface,
    onSurface = SteadfastInk,
    surfaceVariant = SteadfastLightSurfaceVariant,
    onSurfaceVariant = SteadfastInkMuted,
    outline = SteadfastLightBorder,
    outlineVariant = SteadfastInkSubtle,
    error = SteadfastStrugglingLight,
    onError = SteadfastWhiteSurface,
    errorContainer = SteadfastStrugglingContainerLight,
    onErrorContainer = SteadfastStrugglingLight
)

enum class ThemeSetting {
    SYSTEM,
    DARK,
    LIGHT
}

@Composable
fun SteadfastTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        ThemeSetting.DARK -> true
        ThemeSetting.LIGHT -> false
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
