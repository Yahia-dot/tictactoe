package com.example.tictactoe_v02.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tictactoe_v02.ui.screens.GameScreen
import com.example.tictactoe_v02.ui.screens.LobbyScreen
import com.example.tictactoe_v02.ui.screens.PlayerCreationScreen
import com.example.tictactoe_v02.ui.theme.TicTacToeV02Theme
import com.example.tictactoe_v02.viewmodel.GameViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Navigation routes for the app
 */
object NavRoutes {
    const val PLAYER_CREATION = "player_creation"
    const val LOBBY = "lobby"
    const val GAME = "game/{gameId}"

    // Helper function to navigate to game screen with gameId
    fun gameRoute(gameId: String) = "game/$gameId"
}

/**
 * Main app composable that sets up navigation and view model
 */
@Composable
fun TicTacToeApp() {
    val navController = rememberNavController()
    val viewModel = remember { GameViewModel() }

    // Add system UI controller for status bar theming
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    // Wrap in a custom theme with modern colors
    TicTacToeV02Theme {
        NavHost(
            navController = navController,
            startDestination = NavRoutes.PLAYER_CREATION,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            composable(NavRoutes.PLAYER_CREATION) {
                PlayerCreationScreen(navController, viewModel)
            }

            composable(NavRoutes.LOBBY) {
                LobbyScreen(navController, viewModel)
            }

            composable(
                route = NavRoutes.GAME,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId")
                GameScreen(navController, viewModel, gameId)
            }
        }
    }
}