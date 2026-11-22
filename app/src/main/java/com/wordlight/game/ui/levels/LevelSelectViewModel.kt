package com.wordlight.game.ui.levels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordlight.game.domain.model.GameProgress
import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.repository.LevelRepository
import com.wordlight.game.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class LevelStatus { COMPLETED, CURRENT, UNLOCKED, LOCKED }

@Immutable
data class LevelTileUi(val id: Int, val status: LevelStatus)

@Immutable
data class PackUi(
    val title: String,
    val firstId: Int,
    val lastId: Int,
    val minLetters: Int,
    val maxLetters: Int,
    val levels: List<LevelTileUi>,
)

@Immutable
data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val currentLevel: Int = 1,
    val packs: List<PackUi> = emptyList(),
)

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<LevelSelectUiState> =
        combine(flow { emit(levelRepository.getLevels()) }, progressRepository.progress, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LevelSelectUiState())

    private fun toUiState(levels: List<Level>, progress: GameProgress): LevelSelectUiState {
        val ids = levels.map { it.id }
        val current = progress.currentLevel(ids)

        fun statusOf(id: Int) = when {
            progress.isCompleted(id) -> LevelStatus.COMPLETED
            id == current -> LevelStatus.CURRENT
            progress.isUnlocked(id, ids) -> LevelStatus.UNLOCKED
            else -> LevelStatus.LOCKED
        }

        // Packs come from the data ("pack" field), in the order they first appear.
        val packs = levels.groupBy { it.pack }.map { (title, packLevels) ->
            PackUi(
                title = title,
                firstId = packLevels.first().id,
                lastId = packLevels.last().id,
                minLetters = packLevels.minOf { it.letters.size },
                maxLetters = packLevels.maxOf { it.letters.size },
                levels = packLevels.map { LevelTileUi(it.id, statusOf(it.id)) },
            )
        }
        return LevelSelectUiState(
            isLoading = false,
            completedCount = ids.count(progress::isCompleted),
            totalCount = ids.size,
            currentLevel = current,
            packs = packs,
        )
    }
}
