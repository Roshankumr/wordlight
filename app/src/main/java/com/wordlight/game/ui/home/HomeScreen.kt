package com.wordlight.game.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordlight.game.R
import com.wordlight.game.ui.common.PaperTile
import com.wordlight.game.ui.common.PrimaryButton
import com.wordlight.game.ui.common.SecondaryButton
import com.wordlight.game.ui.game.wheel.LetterWheel
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.WordlightColors
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(
    onPlay: (Int) -> Unit,
    onChooseLevel: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPlay = { onPlay(state.currentLevel) },
        onChooseLevel = onChooseLevel,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlay: () -> Unit,
    onChooseLevel: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(WordlightColors.Night)
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        val landscape = maxWidth > maxHeight
        val playSubtitle = if (state.allComplete) {
            stringResource(R.string.home_all_complete)
        } else {
            stringResource(R.string.home_level, state.currentLevel)
        }

        if (landscape) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Brand() }
                Column(Modifier.weight(1f).widthIn(max = 420.dp)) {
                    Actions(playSubtitle, !state.isLoading, onPlay, onChooseLevel)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(0.4f))
                Brand()
                Spacer(Modifier.weight(0.35f))
                DemoWheel(Modifier.size(220.dp))
                Spacer(Modifier.weight(1f))
                Actions(playSubtitle, !state.isLoading, onPlay, onChooseLevel)
            }
        }
    }
}

@Composable
private fun Brand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            "WORD".forEachIndexed { index, letter -> DroppingTile(letter, index) }
        }
        Text(
            text = stringResource(R.string.home_logo_light),
            style = MaterialTheme.typography.displayLarge,
            color = WordlightColors.Lantern,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = WordlightColors.Muted,
        )
    }
}

/** Logo tiles drop in one after another on first composition. */
@Composable
private fun DroppingTile(letter: Char, index: Int) {
    val reducedMotion = LocalReducedMotion.current
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    PaperTile(
        letter = letter,
        size = 64.dp,
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * -24.dp.toPx()
        },
    )
}

/** Non-interactive wheel that "plays" T -> E -> A on a loop, teaching the gesture. */
@Composable
private fun DemoWheel(modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    var path by remember { mutableStateOf<List<Int>>(if (reducedMotion) listOf(0, 1, 2) else emptyList()) }
    if (!reducedMotion) {
        LaunchedEffect(Unit) {
            while (true) {
                path = emptyList(); delay(700)
                path = listOf(0); delay(380)
                path = listOf(0, 1); delay(380)
                path = listOf(0, 1, 2); delay(1600)
            }
        }
    }
    Box(modifier) {
        LetterWheel(
            letters = DEMO_LETTERS,
            wheelOrder = DEMO_LETTERS.indices.toList(),
            path = path,
            enabled = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val DEMO_LETTERS = "TEAR".toList()

@Composable
private fun Actions(playSubtitle: String, enabled: Boolean, onPlay: () -> Unit, onChooseLevel: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        PrimaryButton(
            text = stringResource(R.string.home_play),
            subtitle = playSubtitle,
            enabled = enabled,
            onClick = onPlay,
        )
        Spacer(Modifier.height(16.dp))
        SecondaryButton(text = stringResource(R.string.home_choose_level), onClick = onChooseLevel)
    }
}
