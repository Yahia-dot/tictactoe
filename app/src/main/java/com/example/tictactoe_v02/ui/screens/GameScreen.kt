package com.example.tictactoe_v02.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.tictactoe_v02.data.model.Game
import com.example.tictactoe_v02.data.model.GameState
import com.example.tictactoe_v02.ui.navigation.NavRoutes
import com.example.tictactoe_v02.viewmodel.GameViewModel
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow

private const val TAG = "GameScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(navController: NavController, viewModel: GameViewModel, gameId: String?) {
    val context = LocalContext.current
    val players by viewModel.players.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    val game = gameId?.let { games[it] }
    val localPlayerId = viewModel.localPlayerId.value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tic-Tac-Toe") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (game == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading game...")
                }
            }
            LaunchedEffect(Unit) {
                navController.navigate(NavRoutes.LOBBY) {
                    popUpTo(NavRoutes.LOBBY) { inclusive = false }
                }
            }
        } else {
            GameContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                game = game,
                players = players,
                localPlayerId = localPlayerId,
                isMyTurn = viewModel.isMyTurn(game),
                onCellClick = { cellIndex ->
                    if (viewModel.isMyTurn(game) &&
                        game.gameBoard[cellIndex] == 0 &&
                        (game.gameState == GameState.PLAYER1_TURN || game.gameState == GameState.PLAYER2_TURN)) {
                        viewModel.makeMove(gameId, cellIndex) { }
                    }
                },
                onBackToLobby = {
                    navController.navigate(NavRoutes.LOBBY)
                }
            )
        }
    }
}


@Composable
fun GameContent(
    modifier: Modifier = Modifier,
    game: Game,
    players: Map<String, com.example.tictactoe_v02.data.model.Player>,
    localPlayerId: String?,
    isMyTurn: Boolean,
    onCellClick: (Int) -> Unit,
    onBackToLobby: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player info card (same as in LobbyScreen)
        PlayerInfoCard(playerName = localPlayerId?.let { players[it]?.name } ?: "Unknown")

        Spacer(modifier = Modifier.height(16.dp))

        // Game status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    game.gameState == GameState.PLAYER1_WON -> MaterialTheme.colorScheme.primaryContainer
                    game.gameState == GameState.PLAYER2_WON -> MaterialTheme.colorScheme.secondaryContainer
                    game.gameState == GameState.DRAW -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val winnerId = when (game.gameState) {
                    GameState.PLAYER1_WON -> game.player1Id
                    GameState.PLAYER2_WON -> game.player2Id
                    else -> null
                }

                when {
                    winnerId != null -> {
                        if (localPlayerId == winnerId) {
                            Text(
                                "You Won!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "You Lost, ${players[winnerId]?.name ?: "Opponent"} won",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    game.gameState == GameState.DRAW -> {
                        Text(
                            "Game Ended in Draw!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            if (isMyTurn) "Your Turn!" else "Waiting for opponent...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isMyTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Player indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerIndicator(
                name = players[game.player1Id]?.name ?: "Player 1",
                symbol = "X",
                isActive = game.gameState == GameState.PLAYER1_TURN,
                isWinner = game.gameState == GameState.PLAYER1_WON
            )
            PlayerIndicator(
                name = players[game.player2Id]?.name ?: "Player 2",
                symbol = "O",
                isActive = game.gameState == GameState.PLAYER2_TURN,
                isWinner = game.gameState == GameState.PLAYER2_WON
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game board
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            GameBoard(
                board = game.gameBoard,
                enabled = isMyTurn && (game.gameState == GameState.PLAYER1_TURN || game.gameState == GameState.PLAYER2_TURN),
                onCellClick = onCellClick
            )
        }

        if (game.gameState == GameState.PLAYER1_WON || game.gameState == GameState.PLAYER2_WON || game.gameState == GameState.DRAW) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onBackToLobby,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    "Back to Lobby",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PlayerIndicator(
    name: String,
    symbol: String,
    isActive: Boolean,
    isWinner: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isWinner -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    isWinner -> MaterialTheme.colorScheme.onPrimary
                    isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GameBoard(board: List<Int>, enabled: Boolean, onCellClick: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        for (i in 0..2) {
            Row(modifier = Modifier.weight(1f)) {
                for (j in 0..2) {
                    val index = i * 3 + j
                    GameCell(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp),
                        value = board[index],
                        onClick = { onCellClick(index) },
                        enabled = enabled && board[index] == 0
                    )
                }
            }
        }
    }
}

@Composable
fun GameCell(modifier: Modifier = Modifier, value: Int, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            1 -> Icon(
                Icons.Default.Close,
                contentDescription = "X",
                tint = Color(0xFFE53935), // bright red
                modifier = Modifier.size(48.dp)
            )
            2 -> Icon(
                Icons.Default.RadioButtonUnchecked,
                contentDescription = "O",
                tint = Color(0xFF43A047), // bright green
                modifier = Modifier.size(48.dp)
            )
        }
    }
}