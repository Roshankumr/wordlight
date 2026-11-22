package com.wordlight.game.ui.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordlight.game.R
import com.wordlight.game.ui.common.CircleIconButton
import com.wordlight.game.ui.common.HapticType
import com.wordlight.game.ui.common.PrimaryButton
import com.wordlight.game.ui.common.performHaptic
import com.wordlight.game.ui.common.rememberPressScale
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.Motion
import com.wordlight.game.ui.theme.WordlightColors
import com.wordlight.game.ui.theme.WordlightTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS = 5

@Composable
fun LevelSelectRoute(
    onBack: () -> Unit,
    onLevelClick: (Int) -> Unit,
    viewModel: LevelSelectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LevelSelectScreen(state = state, onBack = onBack, onLevelClick = onLevelClick)
}

@Composable
fun LevelSelectScreen(
    state: LevelSelectUiState,
    onBack: () -> Unit,
    onLevelClick: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(WordlightColors.Night)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.levels_title), style = MaterialTheme.typography.headlineLarge, color = WordlightColors.Paper)
                Text(
                    stringResource(R.string.levels_progress, state.completedCount, state.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WordlightColors.Muted,
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            var tileIndex = 0
            state.packs.forEach { pack ->
                Spacer(Modifier.height(28.dp))
                PackHeader(pack)
                Spacer(Modifier.height(14.dp))
                pack.levels.chunked(COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { tile ->
                            LevelTile(
                                tile = tile,
                                appearIndex = tileIndex++,
                                onClick = { onLevelClick(tile.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (!state.isLoading) {
            PrimaryButton(
                text = stringResource(R.string.levels_continue, state.currentLevel),
                onClick = { onLevelClick(state.currentLevel) },
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PackHeader(pack: PackUi) {
    val letters = if (pack.minLetters == pack.maxLetters) {
        stringResource(R.string.levels_letters_single, pack.minLetters)
    } else {
        stringResource(R.string.levels_letters_range, pack.minLetters, pack.maxLetters)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(pack.title, style = MaterialTheme.typography.titleLarge, color = WordlightColors.Paper, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.levels_pack_info, pack.firstId, pack.lastId, letters),
            style = MaterialTheme.typography.bodyMedium,
            color = WordlightColors.Muted,
        )
    }
}

@Composable
private fun LevelTile(
    tile: LevelTileUi,
    appearIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val locked = tile.status == LevelStatus.LOCKED

    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(appearIndex * 22L)
        appear.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    val shake = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interaction, pressedScale = 0.92f)
    val shape = RoundedCornerShape(20.dp)

    val lockedDescription = stringResource(R.string.cd_locked)
    val completedDescription = stringResource(R.string.cd_completed)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                alpha = appear.value
                val s = (0.85f + 0.15f * appear.value) * pressScale
                scaleX = s
                scaleY = s
                translationX = Motion.shake(shake.value) * Motion.ShakeAmplitude.toPx()
            }
            .semantics {
                contentDescription = when (tile.status) {
                    LevelStatus.LOCKED -> "${tile.id}, $lockedDescription"
                    LevelStatus.COMPLETED -> "${tile.id}, $completedDescription"
                    else -> tile.id.toString()
                }
            }
            .clickable(interactionSource = interaction, indication = null) {
                if (locked) {
                    // Locked tiles answer with a "no" shake instead of silently ignoring the tap.
                    view.performHaptic(HapticType.REJECT)
                    if (!reducedMotion) {
                        scope.launch {
                            shake.snapTo(0f)
                            shake.animateTo(1f, tween(Motion.SHAKE_MS, easing = LinearEasing))
                        }
                    }
                } else {
                    onClick()
                }
            },
    ) {
        when (tile.status) {
            LevelStatus.COMPLETED -> {
                Box(Modifier.fillMaxSize().background(WordlightColors.Paper, shape), contentAlignment = Alignment.Center) {
                    Text(tile.id.toString(), style = WordlightTextStyles.LevelNumber, color = WordlightColors.Night)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(24.dp)
                        .background(WordlightColors.Lantern, CircleShape),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = WordlightColors.Night, modifier = Modifier.size(16.dp))
                }
            }

            LevelStatus.CURRENT -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, WordlightColors.Lantern, shape)
                        .padding(5.dp)
                        .background(WordlightColors.Lantern, RoundedCornerShape(15.dp)),
                ) {
                    Text(tile.id.toString(), style = WordlightTextStyles.LevelNumber, color = WordlightColors.Night)
                }
            }

            LevelStatus.UNLOCKED -> {
                Box(Modifier.fillMaxSize().background(WordlightColors.Slate, shape), contentAlignment = Alignment.Center) {
                    Text(tile.id.toString(), style = WordlightTextStyles.LevelNumber, color = WordlightColors.Paper)
                }
            }

            LevelStatus.LOCKED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, WordlightColors.Outline, shape),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = WordlightColors.Muted, modifier = Modifier.size(18.dp))
                    Text(tile.id.toString(), style = MaterialTheme.typography.titleMedium, color = WordlightColors.Muted)
                }
            }
        }
    }
}
