package com.wordlight.game.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe destinations (Navigation Compose 2.8+). Args are restored automatically via SavedStateHandle. */
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object LevelSelect : Route

    @Serializable
    data class Game(val levelId: Int) : Route
}
