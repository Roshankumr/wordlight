package com.wordlight.game.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wordlight.game.R

/** Fredoka 600: letters, tiles, titles, buttons. */
val Fredoka = FontFamily(Font(R.font.fredoka_semibold, FontWeight.SemiBold))

/** Inter 400–600: labels, status, captions. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private fun fredoka(size: Int, line: Int = size + 4) = TextStyle(
    fontFamily = Fredoka,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = line.sp,
)

private fun inter(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size + 6).sp,
)

val WordlightTypography = Typography(
    displayLarge = fredoka(64, 68),   // "light"
    displayMedium = fredoka(52, 58),  // "Complete!"
    displaySmall = fredoka(44, 50),   // "Paused"
    headlineLarge = fredoka(34, 40),  // screen titles
    headlineMedium = fredoka(30, 36), // "Level 4" in game
    titleLarge = fredoka(24, 30),     // primary buttons, word bubble
    titleMedium = fredoka(20, 26),    // section headers, secondary buttons
    bodyLarge = inter(16),
    bodyMedium = inter(15),
    labelLarge = inter(15, FontWeight.SemiBold),
    labelMedium = inter(13, FontWeight.Medium),
)

object WordlightTextStyles {
    val WheelLetter = fredoka(38, 44)
    val LogoTile = fredoka(40, 44)
    val LevelNumber = fredoka(28, 32)
}
