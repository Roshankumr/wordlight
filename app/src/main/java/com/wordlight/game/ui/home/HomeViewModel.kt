package com.wordlight.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordlight.game.domain.repository.LevelRepository
import com.wordlight.game.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentLevel: Int = 1,
    val allComplete: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = progressRepository.progress
        .map { progress ->
            val ids = levelRepository.getLevels().map { it.id }
            HomeUiState(
                isLoading = false,
                currentLevel = progress.currentLevel(ids),
                allComplete = ids.isNotEmpty() && ids.all(progress::isCompleted),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
