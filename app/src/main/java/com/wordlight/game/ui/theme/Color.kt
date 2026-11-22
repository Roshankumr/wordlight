package com.wordlight.game.ui.theme

import androidx.compose.ui.graphics.Color

/** Palette from the Wordlight design spec. */
object WordlightColors {
    val Night = Color(0xFF101B2E)   // background
    val Slate = Color(0xFF1D2C45)   // empty tiles, cards
    val Wheel = Color(0xFF17243A)   // wheel disc, rows
    val Paper = Color(0xFFF6EEDF)   // text, solved tiles
    val Lantern = Color(0xFFF2A541) // selection, primary
    val Sky = Color(0xFF7CC6FE)     // bonus words
    val Coral = Color(0xFFFF7A6B)   // invalid word

    // Supporting tones derived from the mocks
    val Muted = Color(0xFF8FA3C2)   // captions / secondary text
    val Outline = Color(0xFF2A3B59) // outlined buttons, locked tiles, dots
    val Neutral = Color(0xFF3A4A66) // "already found" bubble
}
