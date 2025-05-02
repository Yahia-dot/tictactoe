package com.example.tictactoe_v02.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.tictactoe_v02.data.model.Game
import com.example.tictactoe_v02.data.model.GameState
import com.example.tictactoe_v02.ui.navigation.NavRoutes
import com.example.tictactoe_v02.viewmodel.GameViewModel

private const val TAG = "LobbyScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(navController: NavController, viewModel: GameViewModel) {
    val context = LocalContext.current
    val players by viewModel.players.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    val currentPlayerId = viewModel.localPlayerId.value
    var shouldCheckForActiveGames by remember { mutableStateOf(true) }

    LaunchedEffect(games) {
        if (currentPlayerId != null) {
            // Navigate if an active game exists (accepted)
            val activeGame = games.entries.find { (_, game) ->
                (game.player1Id == currentPlayerId || game.player2Id == currentPlayerId) &&
                        (game.gameState == GameState.PLAYER1_TURN || game.gameState == GameState.PLAYER2_TURN)
            }
            if (activeGame != null) {
                navController.navigate(NavRoutes.gameRoute(activeGame.key))
            }

            // Notify if a challenge was declined
            val declinedGame = games.entries.find { (_, game) ->
                game.player1Id == currentPlayerId &&
                        game.gameState == GameState.DECLINED &&
                        game.id == viewModel.lastSentChallengeId.value  // only match the latest
            }
            if (declinedGame != null) {
                Toast.makeText(
                    context,
                    "Your challenge to ${players[declinedGame.value.player2Id]?.name ?: "opponent"} was declined.",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.lastSentChallengeId.value = null  // clear after notifying
            }
        }
    }

    val playerName = viewModel.getCurrentPlayerName()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tic-Tac-Toe", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Clear the player session and navigate back
                        viewModel.localPlayerId.value = null
                        navController.navigate(NavRoutes.PLAYER_CREATION) {
                            popUpTo(NavRoutes.PLAYER_CREATION) { inclusive = true }
                        }
                    }) {
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
        if (players.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading players...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Player info card
                PlayerInfoCard(playerName = playerName)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Available Players",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(players.entries.filter { it.key != currentPlayerId }.toList()) { (playerId, player) ->
                        val pendingInvite = games.entries.find { (_, game) ->
                            (game.player1Id == currentPlayerId && game.player2Id == playerId ||
                                    game.player1Id == playerId && game.player2Id == currentPlayerId) &&
                                    game.gameState == GameState.INVITE
                        }

                        PlayerListItem(
                            player = player,
                            pendingInvite = pendingInvite?.value,
                            currentPlayerId = currentPlayerId,
                            onChallenge = {
                                viewModel.invitePlayer(playerId) { success, gameId ->
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "Challenge sent to ${player.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onAccept = { gameId ->
                                viewModel.acceptInvitation(gameId) { success ->
                                    if (success) {
                                        navController.navigate(NavRoutes.gameRoute(gameId))
                                    }
                                }
                            },
                            onDecline = { gameId ->
                                viewModel.declineInvitation(gameId) { success ->
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "You declined the challenge.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerListItem(
    player: com.example.tictactoe_v02.data.model.Player,
    pendingInvite: Game?,
    currentPlayerId: String?,
    onChallenge: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (pendingInvite != null) {
                if (pendingInvite.player1Id == currentPlayerId) {
                    Badge(
                        modifier = Modifier.padding(end = 8.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text("Pending")
                    }
                } else {
                    Row {
                        Button(
                            onClick = { onAccept(pendingInvite.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Accept")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { onDecline(pendingInvite.id) },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Decline")
                        }
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onChallenge,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Challenge")
                }
            }
        }
    }
}

@Composable
fun PlayerInfoCard(playerName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Player",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "You are",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    playerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
