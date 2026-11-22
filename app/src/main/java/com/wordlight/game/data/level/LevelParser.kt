package com.wordlight.game.data.level

import com.wordlight.game.domain.engine.LevelValidator
import com.wordlight.game.domain.model.Cell
import com.wordlight.game.domain.model.Direction
import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.model.WordPlacement
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** JSON -> validated domain levels. Pure (no Android), so it's unit-tested against the real asset. */
class LevelParser @Inject constructor(private val json: Json) {

    fun parse(raw: String): List<Level> {
        val file = json.decodeFromString<LevelFileDto>(raw)
        val levels = file.levels.map { it.toDomain() }.sortedBy { it.id }

        val errors = buildList {
            levels.groupBy { it.id }.filterValues { it.size > 1 }.keys
                .forEach { add("Duplicate level id $it") }
            levels.forEach { addAll(LevelValidator.validate(it)) }
        }
        check(errors.isEmpty()) { "Invalid level data:\n" + errors.joinToString("\n") }
        return levels
    }

    private fun LevelDto.toDomain() = Level(
        id = id,
        pack = pack,
        letters = letters.uppercase().toList(),
        rows = rows,
        cols = cols,
        placements = words.map { it.toDomain() },
        bonusWords = bonus.map { it.uppercase() },
    )

    private fun PlacementDto.toDomain() = WordPlacement(
        word = word.uppercase(),
        start = Cell(row, col),
        direction = when (dir.uppercase()) {
            "H", "ACROSS" -> Direction.ACROSS
            "V", "DOWN" -> Direction.DOWN
            else -> error("Unknown direction '$dir' for '$word'")
        },
    )
}
