package com.wordlight.game.ui.game.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.wordlight.game.domain.model.Cell
import com.wordlight.game.ui.game.GridCellUi
import com.wordlight.game.ui.game.RevealEvent
import com.wordlight.game.ui.theme.Fredoka
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.Motion
import com.wordlight.game.ui.theme.WordlightColors
import kotlinx.coroutines.delay

/**
 * Crossword grid that scales its tiles to fit the available space.
 *
 * @param reveal cells of the word just found; their tiles pop in staggered 70ms per letter.
 * @param entrance when true every tile starts hidden and pops in diagonally (completion screen).
 */
@Composable
fun CrosswordGrid(
    rows: Int,
    cols: Int,
    cells: List<GridCellUi>,
    modifier: Modifier = Modifier,
    reveal: RevealEvent? = null,
    entrance: Boolean = false,
    maxTileSize: Dp = 64.dp,
    showEmptyTiles: Boolean = true,
) {
    if (rows <= 0 || cols <= 0) return
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val gap = 6.dp
        val tile = min(
            (maxWidth - gap * (cols - 1)) / cols,
            (maxHeight - gap * (rows - 1)) / rows,
        ).coerceIn(12.dp, maxTileSize)
        val step = tile + gap

        Box(Modifier.size(width = step * cols - gap, height = step * rows - gap)) {
            cells.forEach { cell ->
                key(cell.row, cell.col) {
                    val revealIndex = reveal?.cells?.indexOf(Cell(cell.row, cell.col)) ?: -1
                    val delayMs = when {
                        entrance -> (cell.row + cell.col) * 60L
                        revealIndex >= 0 -> revealIndex * Motion.TILE_STAGGER_MS
                        else -> 0L
                    }
                    GridTile(
                        letter = cell.letter,
                        revealed = cell.revealed,
                        size = tile,
                        delayMs = delayMs,
                        startHidden = entrance,
                        showEmpty = showEmptyTiles,
                        modifier = Modifier.offset(x = step * cell.col, y = step * cell.row),
                    )
                }
            }
        }
    }
}

@Composable
private fun GridTile(
    letter: Char,
    revealed: Boolean,
    size: Dp,
    delayMs: Long,
    startHidden: Boolean,
    showEmpty: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    // 0 = hidden, 1 = fully shown. Already-revealed tiles start at 1 so rotation never re-animates them.
    val progress = remember { Animatable(if (revealed && !startHidden) 1f else 0f) }

    LaunchedEffect(revealed) {
        if (revealed && progress.value < 1f) {
            delay(delayMs)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = if (reducedMotion) tween(200) else tween(Motion.TILE_REVEAL_MS, easing = Motion.Overshoot),
            )
        } else if (!revealed) {
            progress.snapTo(0f) // restart
        }
    }

    val shape = RoundedCornerShape(size * 0.18f)
    val fontSize = with(LocalDensity.current) { (size * 0.52f).toSp() }

    Box(modifier.size(size)) {
        if (showEmpty) Box(Modifier.size(size).background(WordlightColors.Slate, shape))
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val p = progress.value
                    alpha = p.coerceIn(0f, 1f)
                    // Spec: scale .3 -> 1 with overshoot. Reduced motion: fade only.
                    val s = if (reducedMotion) 1f else 0.3f + 0.7f * p
                    scaleX = s
                    scaleY = s
                }
                .background(WordlightColors.Paper, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.toString(),
                color = WordlightColors.Night,
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = fontSize),
            )
        }
    }
}
