package com.wordlight.game.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wordlight.game.R
import com.wordlight.game.ui.common.CircleIconButton
import com.wordlight.game.ui.theme.WordlightColors

@Composable
fun GameTopBar(
    levelId: Int,
    foundCount: Int,
    totalWords: Int,
    bonusCount: Int,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        CircleIconButton(
            icon = Icons.Rounded.Pause,
            contentDescription = stringResource(R.string.cd_pause),
            onClick = onPause,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = stringResource(R.string.game_level, levelId),
                style = MaterialTheme.typography.headlineMedium,
                color = WordlightColors.Paper,
            )
            Text(
                text = stringResource(R.string.game_words_progress, foundCount, totalWords),
                style = MaterialTheme.typography.bodyMedium,
                color = WordlightColors.Muted,
            )
        }
        StarCounter(count = bonusCount, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

/** Bonus counter; "ticks" (pops) whenever a bonus word is found. */
@Composable
fun StarCounter(count: Int, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count > previous) {
            pop.snapTo(1.35f)
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
        previous = count
    }
    val description = stringResource(R.string.cd_bonus_stars)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(56.dp)
            .background(WordlightColors.Slate, RoundedCornerShape(percent = 50))
            .padding(horizontal = 18.dp)
            .semantics { contentDescription = "$description: $count" },
    ) {
        Icon(Icons.Rounded.StarOutline, contentDescription = null, tint = WordlightColors.Sky, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = WordlightColors.Sky,
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
    }
}

@Composable
fun GameBottomBar(
    gridWords: List<String>,
    foundWords: List<String>,
    shuffleEnabled: Boolean,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        CircleIconButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = stringResource(R.string.cd_shuffle),
            onClick = onShuffle,
            enabled = shuffleEnabled,
            size = 60.dp,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        WordDots(gridWords, foundWords, Modifier.align(Alignment.Center))
    }
}

/** One dot per grid word; lights up in lantern when that word is found. */
@Composable
private fun WordDots(gridWords: List<String>, foundWords: List<String>, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        gridWords.forEach { word ->
            val found = word in foundWords
            val color by animateColorAsState(
                if (found) WordlightColors.Lantern else WordlightColors.Outline,
                label = "dotColor",
            )
            val scale by animateFloatAsState(
                if (found) 1.25f else 1f,
                spring(dampingRatio = 0.4f),
                label = "dotScale",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(color, CircleShape),
            )
        }
    }
}
