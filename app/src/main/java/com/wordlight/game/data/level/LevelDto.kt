package com.wordlight.game.data.level

import kotlinx.serialization.Serializable

/** Raw shape of assets/levels.json. Kept separate from the domain model on purpose. */
@Serializable
data class LevelFileDto(
    val version: Int,
    val levels: List<LevelDto>,
)

@Serializable
data class LevelDto(
    val id: Int,
    val pack: String,
    val letters: String,
    val rows: Int,
    val cols: Int,
    val words: List<PlacementDto>,
    val bonus: List<String> = emptyList(),
)

@Serializable
data class PlacementDto(
    val word: String,
    val row: Int,
    val col: Int,
    /** "H" (across) or "V" (down). */
    val dir: String,
)
