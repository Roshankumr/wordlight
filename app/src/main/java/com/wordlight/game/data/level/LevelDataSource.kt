package com.wordlight.game.data.level

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Where raw level JSON comes from. Swappable (assets today, remote config/Room tomorrow). */
interface LevelDataSource {
    fun readLevelsJson(): String
}

class AssetLevelDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LevelDataSource {

    override fun readLevelsJson(): String =
        context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }

    private companion object {
        const val FILE_NAME = "levels.json"
    }
}
