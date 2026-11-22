package com.wordlight.game.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.wordlight.game.R
import com.wordlight.game.ui.common.SecondaryButton
import com.wordlight.game.ui.common.findActivity
import com.wordlight.game.ui.common.performHaptic
import com.wordlight.game.ui.game.components.GameBottomBar
import com.wordlight.game.ui.game.components.GameTopBar
import com.wordlight.game.ui.game.components.LevelCompleteOverlay
import com.wordlight.game.ui.game.components.PauseOverlay
import com.wordlight.game.ui.game.components.WordBubble
import com.wordlight.game.ui.game.grid.CrosswordGrid
import com.wordlight.game.ui.game.wheel.LetterWheel
import com.wordlight.game.ui.theme.WordlightColors

/** Stateful entry point: wires the ViewModel, lifecycle and back handling to the stateless screen. */
@Composable
fun GameRoute(
    onHome: () -> Unit,
    onLevels: () -> Unit,
    onExit: () -> Unit,
    onNextLevel: (Int) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current.findActivity()

    // One-shot effects, only collected while visible.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is GameEffect.Haptic -> view.performHaptic(effect.type)
                    is GameEffect.NavigateToLevel -> onNextLevel(effect.levelId)
                }
            }
        }
    }

    // Incoming call / notification shade / multi-window: drop the half-drawn word, don't submit it.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onIntent(GameIntent.DragCancelled)
    }
    // App backgrounded (but NOT a rotation): come back to the pause sheet, and never
    // auto-advance while the player isn't looking.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.onIntent(GameIntent.AppBackgrounded)
    }

    // System back: Game -> Pause, Pause -> Resume, Complete -> leave.
    BackHandler {
        when (state.overlay) {
            GameOverlay.NONE -> viewModel.onIntent(GameIntent.Pause)
            GameOverlay.PAUSED -> viewModel.onIntent(GameIntent.Resume)
            GameOverlay.COMPLETE -> onExit()
        }
    }

    GameScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onHome = onHome,
        onLevels = onLevels,
        onExit = onExit,
        onNextLevel = onNextLevel,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onHome: () -> Unit,
    onLevels: () -> Unit,
    onExit: () -> Unit,
    onNextLevel: (Int) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(WordlightColors.Night),
    ) {
        when {
            state.loadFailed -> LoadError(onExit)
            state.isLoading -> Unit // levels are cached in memory; this is a single frame at most
            else -> {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    if (maxWidth > maxHeight) LandscapeGame(state, onIntent) else PortraitGame(state, onIntent)
                }

                PauseOverlay(
                    visible = state.overlay == GameOverlay.PAUSED,
                    levelId = state.levelId,
                    foundCount = state.foundWords.size,
                    totalWords = state.gridWords.size,
                    soundEnabled = state.soundEnabled,
                    hapticsEnabled = state.hapticsEnabled,
                    onResume = { onIntent(GameIntent.Resume) },
                    onSoundChange = { onIntent(GameIntent.SetSound(it)) },
                    onHapticsChange = { onIntent(GameIntent.SetHaptics(it)) },
                    onRestart = { onIntent(GameIntent.Restart) },
                    onLevels = onLevels,
                    onHome = onHome,
                )

                AnimatedVisibility(
                    visible = state.overlay == GameOverlay.COMPLETE,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(200)),
                ) {
                    LevelCompleteOverlay(
                        state = state,
                        onNextLevel = onNextLevel,
                        onLevelSelect = onLevels,
                        onStay = { onIntent(GameIntent.CancelAutoAdvance) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitGame(state: GameUiState, onIntent: (GameIntent) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        GameTopBar(
            levelId = state.levelId,
            foundCount = state.foundWords.size,
            totalWords = state.gridWords.size,
            bonusCount = state.bonusFound.size,
            onPause = { onIntent(GameIntent.Pause) },
        )
        CrosswordGrid(
            rows = state.rows,
            cols = state.cols,
            cells = state.cells,
            reveal = state.reveal,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
        WordBubble(currentWord = state.currentWord, feedback = state.feedback)
        GameWheel(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .widthIn(max = 380.dp),
        )
        Spacer(Modifier.height(8.dp))
        GameBottomBar(
            gridWords = state.gridWords,
            foundWords = state.foundWords,
            shuffleEnabled = state.isInteractive,
            onShuffle = { onIntent(GameIntent.Shuffle) },
        )
    }
}

@Composable
private fun LandscapeGame(state: GameUiState, onIntent: (GameIntent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            GameTopBar(
                levelId = state.levelId,
                foundCount = state.foundWords.size,
                totalWords = state.gridWords.size,
                bonusCount = state.bonusFound.size,
                onPause = { onIntent(GameIntent.Pause) },
            )
            CrosswordGrid(
                rows = state.rows,
                cols = state.cols,
                cells = state.cells,
                reveal = state.reveal,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            WordBubble(currentWord = state.currentWord, feedback = state.feedback)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                GameWheel(state = state, onIntent = onIntent, modifier = Modifier.fillMaxHeight())
            }
            GameBottomBar(
                gridWords = state.gridWords,
                foundWords = state.foundWords,
                shuffleEnabled = state.isInteractive,
                onShuffle = { onIntent(GameIntent.Shuffle) },
            )
        }
    }
}

@Composable
private fun GameWheel(state: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    LetterWheel(
        letters = state.letters,
        wheelOrder = state.wheelOrder,
        path = state.path,
        enabled = state.isInteractive,
        onLetterEnter = { onIntent(GameIntent.LetterEntered(it)) },
        onRelease = { onIntent(GameIntent.DragReleased) },
        onCancel = { onIntent(GameIntent.DragCancelled) },
        modifier = modifier,
    )
}

@Composable
private fun LoadError(onExit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(stringResource(R.string.game_load_error), style = MaterialTheme.typography.titleMedium, color = WordlightColors.Paper)
        Spacer(Modifier.height(24.dp))
        SecondaryButton(text = stringResource(R.string.game_go_back), onClick = onExit)
    }
}
