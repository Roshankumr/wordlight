package com.wordlight.game.ui.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordlight.game.R
import com.wordlight.game.ui.common.PrimaryButton
import com.wordlight.game.ui.game.GameUiState
import com.wordlight.game.ui.game.grid.CrosswordGrid
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.WordlightColors

@Composable
fun LevelCompleteOverlay(
    state: GameUiState,
    onNextLevel: (Int) -> Unit,
    onLevelSelect: () -> Unit,
    onStay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val titleScale = remember { Animatable(if (reducedMotion) 1f else 0.6f) }
    LaunchedEffect(Unit) { titleScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }

    Box(
        modifier
            .fillMaxSize()
            .background(WordlightColors.Night),
    ) {
        Sparkles(Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.complete_level, state.levelId),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp),
                color = WordlightColors.Muted,
            )
            Text(
                text = stringResource(R.string.complete_title),
                style = MaterialTheme.typography.displayMedium,
                color = WordlightColors.Lantern,
                modifier = Modifier.graphicsLayer { scaleX = titleScale.value; scaleY = titleScale.value },
            )
            Spacer(Modifier.height(16.dp))
            CrosswordGrid(
                rows = state.rows,
                cols = state.cols,
                cells = state.cells,
                entrance = true,
                showEmptyTiles = false,
                maxTileSize = 52.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.85f),
            )
            Spacer(Modifier.height(16.dp))
            BonusCard(found = state.bonusFound, total = state.bonusWords.size)
            Spacer(Modifier.height(16.dp))
            AutoAdvanceCountdown(secondsLeft = state.autoAdvanceIn, onStay = onStay)
            Spacer(Modifier.height(8.dp))

            val nextId = state.nextLevelId
            Column(Modifier.widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (nextId != null) {
                    PrimaryButton(
                        text = stringResource(R.string.complete_next),
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = { onNextLevel(nextId) },
                    )
                } else {
                    PrimaryButton(text = stringResource(R.string.complete_back_to_levels), onClick = onLevelSelect)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.complete_level_select),
                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline),
                    color = WordlightColors.Lantern,
                    modifier = Modifier
                        .clickable(onClick = onLevelSelect)
                        .padding(12.dp),
                )
            }
        }
    }
}

/** "Next level in 4s · Stay". Keeps its height when hidden so the buttons don't jump. */
@Composable
private fun AutoAdvanceCountdown(secondsLeft: Int?, onStay: () -> Unit) {
    // Remember the last non-null value so the text doesn't change while it fades out.
    // (Plain holder, not state: recomposition is already driven by `secondsLeft`.)
    val lastSeconds = remember { IntArray(1) }
    if (secondsLeft != null) lastSeconds[0] = secondsLeft
    Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = secondsLeft != null, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.complete_auto_advance, lastSeconds[0]),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WordlightColors.Muted,
                )
                Text(
                    text = stringResource(R.string.complete_stay),
                    style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
                    color = WordlightColors.Paper,
                    modifier = Modifier
                        .clickable(onClick = onStay)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BonusCard(found: List<String>, total: Int) {
    Column(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .background(WordlightColors.Slate, RoundedCornerShape(28.dp))
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.complete_bonus_title),
                style = MaterialTheme.typography.titleMedium,
                color = WordlightColors.Paper,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.complete_bonus_progress, found.size, total),
                style = MaterialTheme.typography.bodyMedium,
                color = WordlightColors.Muted,
            )
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            found.forEach { WordPill(it, WordlightColors.Sky, WordlightColors.Night) }
            val left = total - found.size
            if (left > 0) DashedChip(stringResource(R.string.complete_bonus_left, left))
        }
    }
}

@Composable
private fun DashedChip(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    color = WordlightColors.Outline,
                    cornerRadius = CornerRadius(size.height / 2f),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = WordlightColors.Muted)
    }
}

/** Twinkling 4-point stars around the title. */
@Composable
private fun Sparkles(modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "sparkles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    val t = if (reducedMotion) 0.5f else phase
    Canvas(modifier) {
        val unit = 1.dp.toPx()
        sparkle(Offset(size.width * 0.15f, size.height * 0.13f), 26 * unit * (0.8f + 0.2f * t), WordlightColors.Lantern)
        sparkle(Offset(size.width * 0.85f, size.height * 0.10f), 20 * unit * (1f - 0.2f * t), WordlightColors.Sky)
        sparkle(Offset(size.width * 0.92f, size.height * 0.31f), 26 * unit * (0.85f + 0.15f * (1 - t)), WordlightColors.Lantern)
        sparkle(Offset(size.width * 0.10f, size.height * 0.33f), 18 * unit * (0.8f + 0.2f * t), WordlightColors.Sky)
        drawCircle(WordlightColors.Paper.copy(alpha = 0.4f + 0.6f * t), 5 * unit, Offset(size.width * 0.77f, size.height * 0.19f))
        drawCircle(WordlightColors.Paper.copy(alpha = 1f - 0.6f * t), 5 * unit, Offset(size.width * 0.23f, size.height * 0.225f))
    }
}

private fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val inner = radius * 0.22f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + inner, center.y - inner, center.x + radius, center.y)
        quadraticTo(center.x + inner, center.y + inner, center.x, center.y + radius)
        quadraticTo(center.x - inner, center.y + inner, center.x - radius, center.y)
        quadraticTo(center.x - inner, center.y - inner, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}
