package com.wordlight.game.ui.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wordlight.game.R
import com.wordlight.game.ui.common.PrimaryButton
import com.wordlight.game.ui.common.rememberPressScale
import com.wordlight.game.ui.theme.WordlightColors

/** Scrim + bottom sheet. Tapping the scrim or pressing Back resumes. */
@Composable
fun BoxScope.PauseOverlay(
    visible: Boolean,
    levelId: Int,
    foundCount: Int,
    totalWords: Int,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onResume: () -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(220)),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(WordlightColors.Night.copy(alpha = 0.78f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onResume,
                ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320)) { it } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .background(WordlightColors.Slate, RoundedCornerShape(36.dp))
                .pointerInput(Unit) { detectTapGestures { } } // don't let taps fall through to the scrim
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.displaySmall, color = WordlightColors.Paper)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.pause_subtitle, levelId, foundCount, totalWords),
                style = MaterialTheme.typography.bodyMedium,
                color = WordlightColors.Muted,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = stringResource(R.string.pause_resume), onClick = onResume, leadingIcon = Icons.Rounded.PlayArrow)
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(WordlightColors.Wheel),
            ) {
                SettingRow(stringResource(R.string.pause_sound), soundEnabled, onSoundChange)
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = WordlightColors.Outline)
                SettingRow(stringResource(R.string.pause_haptics), hapticsEnabled, onHapticsChange)
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PauseAction(Icons.Rounded.Replay, stringResource(R.string.pause_restart), onRestart, Modifier.weight(1f))
                PauseAction(Icons.Outlined.GridView, stringResource(R.string.pause_levels), onLevels, Modifier.weight(1f))
                PauseAction(Icons.Outlined.Home, stringResource(R.string.pause_home), onHome, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = WordlightColors.Paper, modifier = Modifier.weight(1f))
        LanternCheckbox(checked)
    }
}

@Composable
private fun LanternCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .then(
                if (checked) Modifier.background(WordlightColors.Lantern, shape)
                else Modifier.border(2.dp, WordlightColors.Outline, shape),
            ),
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = WordlightColors.Night, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PauseAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp))
            .background(WordlightColors.Wheel)
            .clickable(interaction, ripple(color = WordlightColors.Paper), role = Role.Button, onClick = onClick)
            .padding(vertical = 18.dp),
    ) {
        Icon(icon, contentDescription = null, tint = WordlightColors.Paper, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = WordlightColors.Paper)
    }
}
