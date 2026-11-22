package com.wordlight.game.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wordlight.game.ui.game.GameRoute
import com.wordlight.game.ui.home.HomeRoute
import com.wordlight.game.ui.levels.LevelSelectRoute
import com.wordlight.game.ui.theme.LocalReducedMotion
import com.wordlight.game.ui.theme.Motion
import com.wordlight.game.ui.theme.WordlightColors

@Composable
fun WordlightNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val reducedMotion = LocalReducedMotion.current
    val offsetPx = with(LocalDensity.current) { Motion.ScreenOffset.roundToPx() }
    val fade = tween<Float>(Motion.SCREEN_MS)
    val slide = tween<androidx.compose.ui.unit.IntOffset>(Motion.SCREEN_MS)

    // Spec: "Screen enter / exit — slide 24dp + fade, 280ms". Reduced motion: fades only.
    fun enter(fromPx: Int): EnterTransition =
        fadeIn(fade) + if (reducedMotion) EnterTransition.None else slideInHorizontally(slide) { fromPx }

    fun exit(toPx: Int): ExitTransition =
        fadeOut(fade) + if (reducedMotion) ExitTransition.None else slideOutHorizontally(slide) { toPx }

    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier
            .fillMaxSize()
            .background(WordlightColors.Night),
        enterTransition = { enter(offsetPx) },
        exitTransition = { exit(-offsetPx) },
        popEnterTransition = { enter(-offsetPx) },
        popExitTransition = { exit(offsetPx) },
    ) {
        composable<Route.Home> {
            HomeRoute(
                onPlay = { levelId -> navController.navigateSafely(Route.Game(levelId)) },
                onChooseLevel = {
                    navController.navigateSafely(Route.LevelSelect) { launchSingleTop = true }
                },
            )
        }

        composable<Route.LevelSelect> {
            LevelSelectRoute(
                onBack = navController::popBackStackSafely,
                onLevelClick = { levelId -> navController.navigateSafely(Route.Game(levelId)) },
            )
        }

        composable<Route.Game> {
            GameRoute(
                onHome = navController::backToHome,
                onLevels = navController::backToLevelSelect,
                onExit = navController::popBackStackSafely,
                onNextLevel = { nextId ->
                    // Replace the finished level instead of stacking games on top of each other.
                    navController.navigateSafely(Route.Game(nextId)) {
                        popUpTo<Route.Game> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
