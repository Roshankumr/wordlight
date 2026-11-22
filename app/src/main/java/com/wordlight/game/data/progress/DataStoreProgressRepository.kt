package com.wordlight.game.data.progress

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.wordlight.game.domain.model.GameProgress
import com.wordlight.game.domain.model.LevelSnapshot
import com.wordlight.game.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreProgressRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ProgressRepository {

    override val progress: Flow<GameProgress> = dataStore.safeData()
        .map { prefs ->
            GameProgress(prefs[KEY_COMPLETED].orEmpty().mapNotNullTo(HashSet()) { it.toIntOrNull() })
        }
        .distinctUntilChanged()

    override suspend fun markCompleted(levelId: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_COMPLETED] = prefs[KEY_COMPLETED].orEmpty() + levelId.toString()
            // Replaying a solved level starts fresh.
            prefs.remove(foundKey(levelId))
            prefs.remove(bonusKey(levelId))
        }
    }

    override suspend fun getSnapshot(levelId: Int): LevelSnapshot {
        val prefs = dataStore.safeData().first()
        return LevelSnapshot(
            foundWords = prefs[foundKey(levelId)].orEmpty(),
            bonusWords = prefs[bonusKey(levelId)].orEmpty(),
        )
    }

    override suspend fun saveSnapshot(levelId: Int, snapshot: LevelSnapshot) {
        dataStore.edit { prefs ->
            prefs[foundKey(levelId)] = snapshot.foundWords
            prefs[bonusKey(levelId)] = snapshot.bonusWords
        }
    }

    override suspend fun clearSnapshot(levelId: Int) {
        dataStore.edit { prefs ->
            prefs.remove(foundKey(levelId))
            prefs.remove(bonusKey(levelId))
        }
    }

    private companion object {
        val KEY_COMPLETED = stringSetPreferencesKey("completed_levels")
        fun foundKey(levelId: Int) = stringSetPreferencesKey("level_${levelId}_found")
        fun bonusKey(levelId: Int) = stringSetPreferencesKey("level_${levelId}_bonus")
    }
}

/** A corrupted/unreadable prefs file shouldn't crash the game; fall back to defaults. */
internal fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
