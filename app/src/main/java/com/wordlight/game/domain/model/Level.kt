package com.wordlight.game.domain.model

enum class Direction { ACROSS, DOWN }

data class Cell(val row: Int, val col: Int)

/** One answer word on the crossword grid. */
data class WordPlacement(
    val word: String,
    val start: Cell,
    val direction: Direction,
) {
    /** Grid cells this word occupies, in letter order. */
    val cells: List<Cell> = List(word.length) { i ->
        when (direction) {
            Direction.ACROSS -> Cell(start.row, start.col + i)
            Direction.DOWN -> Cell(start.row + i, start.col)
        }
    }
}

data class Level(
    val id: Int,
    val pack: String,
    val letters: List<Char>,
    val rows: Int,
    val cols: Int,
    val placements: List<WordPlacement>,
    val bonusWords: List<String>,
) {
    val gridWords: List<String> = placements.map { it.word }

    /** Every occupied cell mapped to its solution letter. */
    val solution: Map<Cell, Char> = buildMap {
        placements.forEach { placement ->
            placement.cells.forEachIndexed { i, cell -> put(cell, placement.word[i]) }
        }
    }

    fun placementOf(word: String): WordPlacement? = placements.firstOrNull { it.word == word }
}
