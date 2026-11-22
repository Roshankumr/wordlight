package com.wordlight.game.data.level

import com.wordlight.game.di.IoDispatcher
import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.repository.LevelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelRepositoryImpl @Inject constructor(
    private val dataSource: LevelDataSource,
    private val parser: LevelParser,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LevelRepository {

    private val mutex = Mutex()

    @Volatile
    private var cache: List<Level>? = null

    override suspend fun getLevels(): List<Level> =
        cache ?: mutex.withLock {
            cache ?: withContext(ioDispatcher) {
                parser.parse(dataSource.readLevelsJson())
            }.also { cache = it }
        }

    override suspend fun getLevel(id: Int): Level? = getLevels().firstOrNull { it.id == id }
}
