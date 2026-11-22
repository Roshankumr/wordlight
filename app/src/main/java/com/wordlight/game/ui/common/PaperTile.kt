package com.wordlight.game.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.wordlight.game.ui.theme.Fredoka
import com.wordlight.game.ui.theme.WordlightColors
import androidx.compose.ui.text.font.FontWeight

/** Cream letter tile used by the logo, the solved grid and the completion screen. */
@Composable
fun PaperTile(
    letter: Char,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val fontSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .background(WordlightColors.Paper, RoundedCornerShape(size * 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            color = WordlightColors.Night,
            style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = fontSize),
        )
    }
}
