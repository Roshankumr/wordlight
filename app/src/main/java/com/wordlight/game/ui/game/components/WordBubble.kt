package com.wordlight.game.ui.game.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordlight.game.R
import com.wordlight.game.ui.game.FeedbackKind
import com.wordlight.game.ui.game.WordFeedback
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.Motion
import com.wordlight.game.ui.theme.WordlightColors

private sealed interface BubbleModel {
    val key: String

    data class Building(val word: String) : BubbleModel {
        override val key = "building" // same key => letters update in place, no re-transition
    }

    data class Result(val feedback: WordFeedback) : BubbleModel {
        override val key = "result-${feedback.id}"
    }
}

/**
 * Live word bubble between the grid and the wheel.
 *  Building  – lantern pill, updates per letter while dragging
 *  Grid word – lantern pill lifts away
 *  Bonus     – sky pill, "Bonus word +1"
 *  Already   – neutral pill, "Already found"
 *  Invalid   – coral pill shakes ±8dp for 380ms, "Not in this puzzle"
 */
@Composable
fun WordBubble(
    currentWord: String,
    feedback: WordFeedback?,
    modifier: Modifier = Modifier,
) {
    val model: BubbleModel? = when {
        currentWord.isNotEmpty() -> BubbleModel.Building(currentWord)
        feedback != null -> BubbleModel.Result(feedback)
        else -> null
    }
    AnimatedContent(
        targetState = model,
        contentKey = { it?.key },
        transitionSpec = {
            (fadeIn(tween(140)) + scaleIn(tween(160), initialScale = 0.85f)) togetherWith fadeOut(tween(120))
        },
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        label = "wordBubble",
    ) { target ->
        when (target) {
            null -> Spacer(Modifier.fillMaxSize())
            is BubbleModel.Building -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WordPill(target.word, WordlightColors.Lantern, WordlightColors.Night)
            }
            is BubbleModel.Result -> ResultBubble(target.feedback)
        }
    }
}

@Composable
private fun ResultBubble(feedback: WordFeedback) {
    val reducedMotion = LocalReducedMotion.current
    val shake = remember(feedback.id) { Animatable(0f) }
    val lift = remember(feedback.id) { Animatable(0f) }

    LaunchedEffect(feedback.id) {
        if (reducedMotion) return@LaunchedEffect
        when (feedback.kind) {
            FeedbackKind.INVALID -> shake.animateTo(1f, tween(Motion.SHAKE_MS, easing = LinearEasing))
            FeedbackKind.GRID_WORD -> lift.animateTo(1f, tween(650, delayMillis = 150, easing = FastOutSlowInEasing))
            else -> Unit
        }
    }

    val style = feedback.kind.style()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationX = Motion.shake(shake.value) * Motion.ShakeAmplitude.toPx()
                translationY = -lift.value * 28.dp.toPx()
                alpha = 1f - 0.9f * lift.value
            },
        ) {
            WordPill(feedback.word, style.container, style.content)
            style.caption?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (feedback.kind == FeedbackKind.ALREADY_FOUND) WordlightColors.Muted else style.container,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private data class BubbleStyle(val container: Color, val content: Color, @StringRes val caption: Int?)

private fun FeedbackKind.style() = when (this) {
    FeedbackKind.GRID_WORD -> BubbleStyle(WordlightColors.Lantern, WordlightColors.Night, null)
    FeedbackKind.BONUS_WORD -> BubbleStyle(WordlightColors.Sky, WordlightColors.Night, R.string.feedback_bonus)
    FeedbackKind.ALREADY_FOUND -> BubbleStyle(WordlightColors.Neutral, WordlightColors.Paper, R.string.feedback_already)
    FeedbackKind.INVALID -> BubbleStyle(WordlightColors.Coral, WordlightColors.Night, R.string.feedback_invalid)
}

@Composable
fun WordPill(word: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(container, RoundedCornerShape(percent = 50))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 3.sp),
            color = content,
        )
    }
}
