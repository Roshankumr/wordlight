package com.wordlight.game.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wordlight.game.core.audio.Sfx
import com.wordlight.game.core.audio.SoundPlayer
import com.wordlight.game.domain.engine.GuessResult
import com.wordlight.game.domain.engine.SelectionPath
import com.wordlight.game.domain.engine.WordJudge
import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.model.LevelSnapshot
import com.wordlight.game.domain.repository.LevelRepository
import com.wordlight.game.domain.repository.ProgressRepository
import com.wordlight.game.domain.repository.SettingsRepository
import com.wordlight.game.ui.common.HapticType
import com.wordlight.game.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * State holder for one level.
 *
 * State survival:
 *  - rotation: the ViewModel itself survives.
 *  - process death: found words, bonus words, wheel order and pause flag are mirrored
 *    into [SavedStateHandle].
 *  - leaving the level / killing the app: found words are also persisted to DataStore,
 *    so coming back later resumes the level ("progress saved").
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val wordJudge: WordJudge,
    private val soundPlayer: SoundPlayer,
) : ViewModel() {

    private val levelId: Int = savedStateHandle.toRoute<Route.Game>().levelId

    private val _uiState = MutableStateFlow(GameUiState(levelId = levelId))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = Channel<GameEffect>(Channel.BUFFERED)
    val effects: Flow<GameEffect> = _effects.receiveAsFlow()

    private var level: Level? = null
    private var feedbackJob: Job? = null
    private var completionJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var nextEventId = 0L

    init {
        observeSettings()
        loadLevel()
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.LetterEntered -> onLetterEntered(intent.index)
            GameIntent.DragReleased -> onDragReleased()
            GameIntent.DragCancelled -> _uiState.update { it.copy(path = emptyList()) }
            GameIntent.Shuffle -> shuffle()
            GameIntent.Pause -> pause()
            GameIntent.Resume -> resume()
            GameIntent.AppBackgrounded -> {
                pause()
                cancelAutoAdvance()
            }
            GameIntent.CancelAutoAdvance -> cancelAutoAdvance()
            GameIntent.Restart -> restart()
            is GameIntent.SetSound -> viewModelScope.launch { settingsRepository.setSoundEnabled(intent.enabled) }
            is GameIntent.SetHaptics -> viewModelScope.launch { settingsRepository.setHapticsEnabled(intent.enabled) }
        }
    }

    // region Loading

    private fun observeSettings() {
        settingsRepository.settings
            .onEach { s -> _uiState.update { it.copy(soundEnabled = s.soundEnabled, hapticsEnabled = s.hapticsEnabled) } }
            .launchIn(viewModelScope)
    }

    private fun loadLevel() = viewModelScope.launch {
        val loaded = levelRepository.getLevel(levelId)
        if (loaded == null) {
            _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            return@launch
        }
        level = loaded

        val snapshot = restoreSnapshot()
        val found = loaded.gridWords.filter { it in snapshot.foundWords }
        val bonus = loaded.bonusWords.filter { it in snapshot.bonusWords }
        val order = savedStateHandle.get<IntArray>(KEY_ORDER)?.toList()
            ?.takeIf { it.sorted() == loaded.letters.indices.toList() }
            ?: loaded.letters.indices.toList()
        val solved = found.size == loaded.placements.size
        val nextId = levelRepository.getLevels().map { it.id }.filter { it > levelId }.minOrNull()

        if (solved) {
            // e.g. process died during the 900ms "grid full" hold — make sure it's recorded.
            withContext(NonCancellable) { progressRepository.markCompleted(levelId) }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                letters = loaded.letters,
                wheelOrder = order,
                rows = loaded.rows,
                cols = loaded.cols,
                cells = buildCells(loaded, found),
                gridWords = loaded.gridWords,
                foundWords = found,
                bonusWords = loaded.bonusWords,
                bonusFound = bonus,
                nextLevelId = nextId,
                overlay = when {
                    solved -> GameOverlay.COMPLETE
                    savedStateHandle.get<Boolean>(KEY_PAUSED) == true -> GameOverlay.PAUSED
                    else -> GameOverlay.NONE
                },
            )
        }
    }

    private suspend fun restoreSnapshot(): LevelSnapshot {
        val found = savedStateHandle.get<ArrayList<String>>(KEY_FOUND)
        val bonus = savedStateHandle.get<ArrayList<String>>(KEY_BONUS)
        return if (found != null && bonus != null) {
            LevelSnapshot(found.toSet(), bonus.toSet())
        } else {
            progressRepository.getSnapshot(levelId)
        }
    }

    // endregion

    // region Swipe

    private fun onLetterEntered(index: Int) {
        val state = _uiState.value
        if (!state.isInteractive || index !in state.letters.indices) return

        val newPath = SelectionPath.enter(state.path, index)
        if (newPath == state.path) return

        if (newPath.size > state.path.size) {
            // Each extra letter plays slightly higher — a small, satisfying build-up.
            play(Sfx.SELECT, pitch = 1f + 0.08f * (newPath.size - 1))
            haptic(HapticType.TICK)
        }
        if (state.path.isEmpty()) feedbackJob?.cancel()
        _uiState.update { it.copy(path = newPath, feedback = null) }
    }

    private fun onDragReleased() {
        val state = _uiState.value
        val currentLevel = level ?: return
        if (state.path.isEmpty()) return

        _uiState.update { it.copy(path = emptyList()) }
        if (state.path.size < MIN_SUBMIT_LENGTH || !state.isInteractive) return

        val guess = state.currentWord
        when (val result = wordJudge.judge(guess, currentLevel, state.foundWords, state.bonusFound)) {
            is GuessResult.GridWord -> onGridWord(currentLevel, result, state)
            is GuessResult.BonusWord -> onBonusWord(result.word, state)
            is GuessResult.AlreadyFound -> {
                showFeedback(result.word, FeedbackKind.ALREADY_FOUND)
                haptic(HapticType.TICK)
            }
            is GuessResult.Invalid -> {
                showFeedback(result.word, FeedbackKind.INVALID)
                play(Sfx.INVALID)
                haptic(HapticType.REJECT)
            }
        }
    }

    private fun onGridWord(currentLevel: Level, result: GuessResult.GridWord, state: GameUiState) {
        val found = state.foundWords + result.placement.word
        val solved = found.size == currentLevel.placements.size
        _uiState.update {
            it.copy(
                foundWords = found,
                cells = buildCells(currentLevel, found),
                reveal = RevealEvent(nextId(), result.placement.cells),
            )
        }
        showFeedback(result.placement.word, FeedbackKind.GRID_WORD)
        play(Sfx.WORD)
        haptic(HapticType.CONFIRM)
        persist(found, state.bonusFound, solved)
        if (solved) scheduleCompletion()
    }

    private fun onBonusWord(word: String, state: GameUiState) {
        val bonus = state.bonusFound + word
        _uiState.update { it.copy(bonusFound = bonus) }
        showFeedback(word, FeedbackKind.BONUS_WORD)
        play(Sfx.BONUS)
        haptic(HapticType.CONFIRM)
        persist(state.foundWords, bonus, solved = false)
    }

    private fun scheduleCompletion() {
        completionJob?.cancel()
        completionJob = viewModelScope.launch {
            delay(GRID_FULL_HOLD_MS) // let the last tiles pop before the overlay
            play(Sfx.COMPLETE)
            haptic(HapticType.SUCCESS)
            _uiState.update { it.copy(overlay = GameOverlay.COMPLETE) }
            startAutoAdvance()
        }
    }

    /**
     * Spec: "automatically transition to the next level when the grid is filled".
     * The Complete screen (from the UI design) is shown first, then counts down and moves on
     * by itself. The player can tap Next to go now, or Stay to cancel.
     */
    private fun startAutoAdvance() {
        val nextId = _uiState.value.nextLevelId ?: return
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            for (seconds in AUTO_ADVANCE_SECONDS downTo 1) {
                _uiState.update { it.copy(autoAdvanceIn = seconds) }
                delay(1_000)
            }
            _uiState.update { it.copy(autoAdvanceIn = null) }
            _effects.send(GameEffect.NavigateToLevel(nextId))
        }
    }

    private fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
        _uiState.update { it.copy(autoAdvanceIn = null) }
    }

    private fun showFeedback(word: String, kind: FeedbackKind) {
        val feedback = WordFeedback(nextId(), word, kind)
        _uiState.update { it.copy(feedback = feedback) }
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_VISIBLE_MS)
            _uiState.update { if (it.feedback?.id == feedback.id) it.copy(feedback = null) else it }
        }
    }

    // endregion

    // region Controls

    private fun shuffle() {
        val state = _uiState.value
        if (!state.isInteractive || state.path.isNotEmpty() || state.wheelOrder.size < 2) return
        var next = state.wheelOrder.shuffled()
        while (next == state.wheelOrder) next = state.wheelOrder.shuffled()
        savedStateHandle[KEY_ORDER] = next.toIntArray()
        _uiState.update { it.copy(wheelOrder = next) }
        haptic(HapticType.TICK)
    }

    private fun pause() {
        val state = _uiState.value
        if (!state.isInteractive) return
        savedStateHandle[KEY_PAUSED] = true
        _uiState.update { it.copy(overlay = GameOverlay.PAUSED, path = emptyList()) }
    }

    private fun resume() {
        if (_uiState.value.overlay != GameOverlay.PAUSED) return
        savedStateHandle[KEY_PAUSED] = false
        _uiState.update { it.copy(overlay = GameOverlay.NONE) }
    }

    private fun restart() {
        val currentLevel = level ?: return
        completionJob?.cancel()
        feedbackJob?.cancel()
        savedStateHandle[KEY_FOUND] = arrayListOf<String>()
        savedStateHandle[KEY_BONUS] = arrayListOf<String>()
        savedStateHandle[KEY_PAUSED] = false
        _uiState.update {
            it.copy(
                foundWords = emptyList(),
                bonusFound = emptyList(),
                cells = buildCells(currentLevel, emptyList()),
                path = emptyList(),
                reveal = null,
                feedback = null,
                overlay = GameOverlay.NONE,
            )
        }
        viewModelScope.launch { withContext(NonCancellable) { progressRepository.clearSnapshot(levelId) } }
    }

    // endregion

    // region Helpers

    private fun persist(found: List<String>, bonus: List<String>, solved: Boolean) {
        savedStateHandle[KEY_FOUND] = ArrayList(found)
        savedStateHandle[KEY_BONUS] = ArrayList(bonus)
        viewModelScope.launch {
            // NonCancellable: leaving the screen right after a word must not lose the write.
            withContext(NonCancellable) {
                if (solved) {
                    progressRepository.markCompleted(levelId)
                } else {
                    progressRepository.saveSnapshot(levelId, LevelSnapshot(found.toSet(), bonus.toSet()))
                }
            }
        }
    }

    private fun buildCells(level: Level, found: Collection<String>): List<GridCellUi> {
        val revealed = level.placements.filter { it.word in found }.flatMapTo(HashSet()) { it.cells }
        return level.solution.map { (cell, letter) ->
            GridCellUi(cell.row, cell.col, letter, revealed = cell in revealed)
        }
    }

    private fun play(sfx: Sfx, pitch: Float = 1f) {
        if (_uiState.value.soundEnabled) soundPlayer.play(sfx, pitch)
    }

    private fun haptic(type: HapticType) {
        if (_uiState.value.hapticsEnabled) _effects.trySend(GameEffect.Haptic(type))
    }

    private fun nextId(): Long = ++nextEventId

    // endregion

    private companion object {
        const val KEY_FOUND = "found_words"
        const val KEY_BONUS = "bonus_words"
        const val KEY_ORDER = "wheel_order"
        const val KEY_PAUSED = "paused"

        const val MIN_SUBMIT_LENGTH = 2
        const val FEEDBACK_VISIBLE_MS = 900L
        const val GRID_FULL_HOLD_MS = 900L
        const val AUTO_ADVANCE_SECONDS = 5
    }
}
