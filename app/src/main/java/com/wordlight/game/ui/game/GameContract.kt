package com.wordlight.game.ui.game

import androidx.compose.runtime.Immutable
import com.wordlight.game.domain.model.Cell
import com.wordlight.game.ui.common.HapticType

@Immutable
data class GridCellUi(
    val row: Int,
    val col: Int,
    val letter: Char,
    val revealed: Boolean,
)

enum class FeedbackKind { GRID_WORD, BONUS_WORD, ALREADY_FOUND, INVALID }

/** Result bubble shown after a swipe. [id] makes repeated identical results re-animate. */
@Immutable
data class WordFeedback(val id: Long, val word: String, val kind: FeedbackKind)

/** Cells of the word just found, in letter order, so the grid can stagger the pop-in. */
@Immutable
data class RevealEvent(val id: Long, val cells: List<Cell>)

enum class GameOverlay { NONE, PAUSED, COMPLETE }

/** Single immutable UI state for the game screen (MVI "model"). */
@Immutable
data class GameUiState(
    val levelId: Int = 0,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val letters: List<Char> = emptyList(),
    /** wheelOrder[slot] = letter index; changes on shuffle. */
    val wheelOrder: List<Int> = emptyList(),
    /** Letter indices currently connected by the finger. */
    val path: List<Int> = emptyList(),
    val rows: Int = 0,
    val cols: Int = 0,
    val cells: List<GridCellUi> = emptyList(),
    val gridWords: List<String> = emptyList(),
    val foundWords: List<String> = emptyList(),
    val bonusWords: List<String> = emptyList(),
    val bonusFound: List<String> = emptyList(),
    val feedback: WordFeedback? = null,
    val reveal: RevealEvent? = null,
    val overlay: GameOverlay = GameOverlay.NONE,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val nextLevelId: Int? = null,
    /** Seconds until the Complete screen auto-advances to the next level; null = not counting. */
    val autoAdvanceIn: Int? = null,
) {
    val currentWord: String get() = path.joinToString("") { letters[it].toString() }
    val isSolved: Boolean get() = gridWords.isNotEmpty() && foundWords.size == gridWords.size
    val isInteractive: Boolean get() = !isLoading && overlay == GameOverlay.NONE && !isSolved
}

/** Everything the user can do on the game screen (MVI "intents"). */
sealed interface GameIntent {
    data class LetterEntered(val index: Int) : GameIntent
    data object DragReleased : GameIntent
    data object DragCancelled : GameIntent
    data object Shuffle : GameIntent
    data object Pause : GameIntent
    data object Resume : GameIntent
    /** App went to the background (not a rotation): pause play and stop any auto-advance. */
    data object AppBackgrounded : GameIntent
    /** "Stay" on the Complete screen: don't auto-advance. */
    data object CancelAutoAdvance : GameIntent
    data object Restart : GameIntent
    data class SetSound(val enabled: Boolean) : GameIntent
    data class SetHaptics(val enabled: Boolean) : GameIntent
}

/** One-shot side effects handled by the UI layer. */
sealed interface GameEffect {
    data class Haptic(val type: HapticType) : GameEffect
    data class NavigateToLevel(val levelId: Int) : GameEffect
}
