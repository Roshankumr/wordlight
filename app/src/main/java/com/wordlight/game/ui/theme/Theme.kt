package com.wordlight.game.ui.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private val WordlightColorScheme = darkColorScheme(
    primary = WordlightColors.Lantern,
    onPrimary = WordlightColors.Night,
    secondary = WordlightColors.Sky,
    onSecondary = WordlightColors.Night,
    error = WordlightColors.Coral,
    background = WordlightColors.Night,
    onBackground = WordlightColors.Paper,
    surface = WordlightColors.Slate,
    onSurface = WordlightColors.Paper,
    surfaceVariant = WordlightColors.Wheel,
    onSurfaceVariant = WordlightColors.Muted,
    outline = WordlightColors.Outline,
)

@Composable
fun WordlightTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = WordlightColorScheme,
            typography = WordlightTypography,
            content = content,
        )
    }
}
