package com.wordlight.game.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Motion tokens from the spec ("values in dp / ms, map 1:1 to Compose"). */
object Motion {
    const val SCREEN_MS = 280
    val ScreenOffset = 24.dp

    const val LETTER_SELECT_SCALE = 1.16f

    const val TILE_REVEAL_MS = 440
    const val TILE_STAGGER_MS = 70L

    const val SHAKE_MS = 380
    val ShakeAmplitude = 8.dp

    /** Overshoot used for tile pop-ins (scale .3 -> 1). */
    val Overshoot = Easing { t ->
        val s = 1.7f
        val x = t - 1f
        x * x * ((s + 1f) * x + s) + 1f
    }

    /** Damped horizontal shake: progress 0..1 -> offset factor in -1..1. */
    fun shake(progress: Float): Float =
        (sin(progress * PI * 6).toFloat()) * (1f - progress)
}

/** True when the user disabled animations system-wide; we then use fades only. */
val LocalReducedMotion = staticCompositionLocalOf { false }
