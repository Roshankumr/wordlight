package com.wordlight.game.domain.engine

/**
 * Reducer for the swipe path (list of wheel letter indices).
 *
 * Rules:
 *  - first letter starts the path
 *  - re-entering the current (last) letter is a no-op
 *  - re-entering the previous letter backtracks one step
 *  - a letter already used elsewhere in the path is ignored (each tile used once)
 *  - otherwise the letter is appended
 */
object SelectionPath {

    fun enter(path: List<Int>, index: Int): List<Int> = when {
        path.isEmpty() -> listOf(index)
        path.last() == index -> path
        path.size >= 2 && path[path.size - 2] == index -> path.dropLast(1)
        index in path -> path
        else -> path + index
    }
}
