package com.wordlight.game.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.wordlight.game.data.progress.safeData
import com.wordlight.game.domain.model.GameSettings
import com.wordlight.game.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<GameSettings> = dataStore.safeData()
        .map { prefs ->
            GameSettings(
                soundEnabled = prefs[KEY_SOUND] ?: true,
                hapticsEnabled = prefs[KEY_HAPTICS] ?: true,
            )
        }
        .distinctUntilChanged()

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND] = enabled }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    private companion object {
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
    }
}
