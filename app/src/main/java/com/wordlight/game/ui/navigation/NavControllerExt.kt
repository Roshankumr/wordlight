package com.wordlight.game.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Only navigate when the current destination is RESUMED. While a transition is running the
 * current entry is not RESUMED, so double taps / rapid taps can never push duplicate screens.
 */
private val NavController.isCurrentResumed: Boolean
    get() = currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

fun <T : Any> NavController.navigateSafely(route: T, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (isCurrentResumed) navigate(route, builder)
}

fun NavController.popBackStackSafely() {
    if (isCurrentResumed) popBackStack()
}

/** Pause/Complete -> "Home": unwind to the existing Home entry (never a second Home). */
fun NavController.backToHome() {
    if (isCurrentResumed) popBackStack<Route.Home>(inclusive = false)
}

/** Pause/Complete -> "Levels": reuse Level Select if it's already on the stack, else put it above Home. */
fun NavController.backToLevelSelect() {
    if (!isCurrentResumed) return
    val popped = popBackStack<Route.LevelSelect>(inclusive = false)
    if (!popped) {
        navigate(Route.LevelSelect) {
            popUpTo<Route.Home> { inclusive = false }
            launchSingleTop = true
        }
    }
}
