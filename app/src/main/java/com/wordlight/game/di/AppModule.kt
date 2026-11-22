package com.wordlight.game.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.wordlight.game.core.audio.SoundPlayer
import com.wordlight.game.core.audio.SoundPoolPlayer
import com.wordlight.game.data.level.AssetLevelDataSource
import com.wordlight.game.data.level.LevelDataSource
import com.wordlight.game.data.level.LevelRepositoryImpl
import com.wordlight.game.data.progress.DataStoreProgressRepository
import com.wordlight.game.data.settings.DataStoreSettingsRepository
import com.wordlight.game.domain.repository.LevelRepository
import com.wordlight.game.domain.repository.ProgressRepository
import com.wordlight.game.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("wordlight_prefs") },
        )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindLevelDataSource(impl: AssetLevelDataSource): LevelDataSource

    @Binds
    abstract fun bindLevelRepository(impl: LevelRepositoryImpl): LevelRepository

    @Binds
    abstract fun bindProgressRepository(impl: DataStoreProgressRepository): ProgressRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindSoundPlayer(impl: SoundPoolPlayer): SoundPlayer
}
