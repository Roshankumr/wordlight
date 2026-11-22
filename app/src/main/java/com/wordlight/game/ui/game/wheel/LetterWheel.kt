package com.wordlight.game.ui.game.wheel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.Motion
import com.wordlight.game.ui.theme.WordlightColors
import com.wordlight.game.ui.theme.WordlightTextStyles
import kotlin.math.roundToInt

private object WheelDefaults {
    val LetterSize = 64.dp
    val HitRadius = 34.dp   // spec: 34dp around each letter centre
    val SampleStep = 8.dp   // spec: sample every 8dp between move events
    val LineWidth = 10.dp
}

/**
 * Circular letter wheel with continuous swipe selection.
 *
 * State split:
 *  - [path] (which letters are connected) is owned by the ViewModel -> survives rotation.
 *  - the live finger position is purely visual, so it's local state read only in the draw
 *    phase; moving the finger redraws the line without recomposing the screen.
 *
 * Gesture rules (from the spec):
 *  1. Track one pointer id; other fingers are ignored until it lifts.
 *  2. 34dp hit radius per letter.
 *  3. Segment between move events is sampled every 8dp, so fast swipes never skip letters.
 *  4. Re-entering the previous letter backtracks (handled by SelectionPath in the VM).
 *  5. Finger up -> [onRelease] (VM submits if 2+ letters); any cancellation -> [onCancel].
 */
@Composable
fun LetterWheel(
    letters: List<Char>,
    wheelOrder: List<Int>,
    path: List<Int>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLetterEnter: (Int) -> Unit = {},
    onRelease: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { WheelDefaults.HitRadius.toPx() }
    val sampleStepPx = with(density) { WheelDefaults.SampleStep.toPx() }
    val lineWidthPx = with(density) { WheelDefaults.LineWidth.toPx() }
    val reducedMotion = LocalReducedMotion.current

    var wheelSize by remember { mutableStateOf(IntSize.Zero) }
    var fingerPosition by remember { mutableStateOf<Offset?>(null) }

    // Each letter animates around the ring to its new slot when shuffled.
    val angles: List<Float> = letters.indices.map { letterIndex ->
        val slot = wheelOrder.indexOf(letterIndex).coerceAtLeast(0)
        val angle by animateFloatAsState(
            targetValue = WheelGeometry.slotAngle(slot, letters.size),
            animationSpec = if (reducedMotion) snap() else tween(420, easing = FastOutSlowInEasing),
            label = "letterAngle$letterIndex",
        )
        angle
    }
    val centers = WheelGeometry.letterCenters(wheelSize.toSize(), angles)

    val latestCenters by rememberUpdatedState(centers)
    val latestOnEnter by rememberUpdatedState(onLetterEnter)
    val latestOnRelease by rememberUpdatedState(onRelease)
    val latestOnCancel by rememberUpdatedState(onCancel)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { wheelSize = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    var lastPosition = down.position
                    var lastHit: Int? = null
                    var released = false

                    fun probe(point: Offset) {
                        val hit = WheelGeometry.hitTest(latestCenters, point, hitRadiusPx)
                        if (hit != lastHit) {
                            lastHit = hit
                            if (hit != null) latestOnEnter(hit)
                        }
                    }

                    try {
                        fingerPosition = down.position
                        probe(down.position)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            // Rule 1: only our pointer matters; extra fingers are ignored.
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                            if (!change.pressed) {
                                released = true
                                change.consume()
                                break
                            }
                            WheelGeometry.sampleSegment(lastPosition, change.position, sampleStepPx) { probe(it) }
                            lastPosition = change.position
                            fingerPosition = change.position
                            change.consume()
                        }
                    } finally {
                        // Runs on normal release AND when the coroutine is cancelled
                        // (rotation, composable leaving, `enabled` flipping to false).
                        fingerPosition = null
                        if (released) latestOnRelease() else latestOnCancel()
                    }
                }
            }
            .drawBehind {
                drawCircle(color = WordlightColors.Wheel, radius = size.minDimension / 2f)
                if (path.isEmpty() || centers.isEmpty()) return@drawBehind
                for (i in 0 until path.lastIndex) {
                    drawLine(
                        color = WordlightColors.Lantern,
                        start = centers[path[i]],
                        end = centers[path[i + 1]],
                        strokeWidth = lineWidthPx,
                        cap = StrokeCap.Round,
                    )
                }
                // Reading fingerPosition here (draw phase) => only a redraw per move, no recomposition.
                fingerPosition?.let { finger ->
                    drawLine(
                        color = WordlightColors.Lantern,
                        start = centers[path.last()],
                        end = finger,
                        strokeWidth = lineWidthPx,
                        cap = StrokeCap.Round,
                    )
                }
            },
    ) {
        if (centers.size == letters.size) {
            letters.forEachIndexed { index, letter ->
                WheelLetter(
                    letter = letter,
                    selected = index in path,
                    center = centers[index],
                )
            }
        }
    }
}

@Composable
private fun WheelLetter(letter: Char, selected: Boolean, center: Offset) {
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (selected && !reducedMotion) Motion.LETTER_SELECT_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1400f), // ~120ms settle
        label = "letterScale",
    )
    val background by animateColorAsState(
        targetValue = if (selected) WordlightColors.Lantern else Color.Transparent,
        animationSpec = tween(120),
        label = "letterBg",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) WordlightColors.Night else WordlightColors.Paper,
        animationSpec = tween(120),
        label = "letterFg",
    )
    Box(
        modifier = Modifier
            .offset {
                val half = WheelDefaults.LetterSize.roundToPx() / 2
                IntOffset(center.x.roundToInt() - half, center.y.roundToInt() - half)
            }
            .size(WheelDefaults.LetterSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = letter.toString(), style = WordlightTextStyles.WheelLetter, color = foreground)
    }
}
