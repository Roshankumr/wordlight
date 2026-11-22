package com.wordlight.game.domain.repository

import com.wordlight.game.domain.model.GameProgress
import com.wordlight.game.domain.model.GameSettings
import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.model.LevelSnapshot
import kotlinx.coroutines.flow.Flow

interface LevelRepository {
    /** All levels ordered by id. Loaded once and cached. */
    suspend fun getLevels(): List<Level>
    suspend fun getLevel(id: Int): Level?
}

interface ProgressRepository {
    val progress: Flow<GameProgress>
    suspend fun markCompleted(levelId: Int)
    suspend fun getSnapshot(levelId: Int): LevelSnapshot
    suspend fun saveSnapshot(levelId: Int, snapshot: LevelSnapshot)
    suspend fun clearSnapshot(levelId: Int)
}

interface SettingsRepository {
    val settings: Flow<GameSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
}
