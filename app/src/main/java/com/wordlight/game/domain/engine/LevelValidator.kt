package com.wordlight.game.domain.engine

import com.wordlight.game.domain.model.Cell
import com.wordlight.game.domain.model.Level

/**
 * Checks that a level is actually solvable: every word can be spelled from the wheel,
 * fits inside the grid, and crossing words agree on shared letters.
 * Runs when levels are parsed (fail fast) and in unit tests over the shipped JSON.
 */
object LevelValidator {

    fun validate(level: Level): List<String> {
        val errors = mutableListOf<String>()
        val tag = "Level ${level.id}"
        val wheel = level.letters.groupingBy { it }.eachCount()

        fun spellable(word: String) =
            word.groupingBy { it }.eachCount().all { (ch, n) -> (wheel[ch] ?: 0) >= n }

        if (level.letters.size !in 3..5) errors += "$tag: wheel must have 3-5 letters"
        if (level.placements.isEmpty()) errors += "$tag: no grid words"
        if (level.gridWords.size != level.gridWords.toSet().size) errors += "$tag: duplicate grid words"

        val grid = mutableMapOf<Cell, Char>()
        level.placements.forEach { placement ->
            if (!spellable(placement.word)) errors += "$tag: '${placement.word}' can't be spelled from the wheel"
            placement.cells.forEachIndexed { i, cell ->
                if (cell.row !in 0 until level.rows || cell.col !in 0 until level.cols) {
                    errors += "$tag: '${placement.word}' is outside the ${level.rows}x${level.cols} grid"
                }
                val existing = grid.put(cell, placement.word[i])
                if (existing != null && existing != placement.word[i]) {
                    errors += "$tag: '${placement.word}' conflicts at (${cell.row},${cell.col})"
                }
            }
        }

        level.bonusWords.forEach { bonus ->
            if (!spellable(bonus)) errors += "$tag: bonus '$bonus' can't be spelled from the wheel"
            if (bonus in level.gridWords) errors += "$tag: bonus '$bonus' is also a grid word"
        }
        return errors
    }
}
