package com.wordlight.game.domain.model

/** Long-lived, cross-level progress. */
data class GameProgress(val completed: Set<Int> = emptySet()) {

    fun isCompleted(levelId: Int): Boolean = levelId in completed

    /** A level is playable if it's the first one, already solved, or its predecessor is solved. */
    fun isUnlocked(levelId: Int, orderedIds: List<Int>): Boolean {
        val index = orderedIds.indexOf(levelId)
        if (index < 0) return false
        return index == 0 || levelId in completed || orderedIds[index - 1] in completed
    }

    /** First unsolved level, or the last level if everything is done. */
    fun currentLevel(orderedIds: List<Int>): Int =
        orderedIds.firstOrNull { it !in completed } ?: orderedIds.lastOrNull() ?: 1
}

/** In-progress state of a single level, persisted so "progress saved" is true. */
data class LevelSnapshot(
    val foundWords: Set<String> = emptySet(),
    val bonusWords: Set<String> = emptySet(),
)

data class GameSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)
