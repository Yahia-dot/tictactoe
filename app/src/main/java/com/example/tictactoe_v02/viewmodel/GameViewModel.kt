package com.example.tictactoe_v02.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoe_v02.data.model.Game
import com.example.tictactoe_v02.data.model.GameState
import com.example.tictactoe_v02.data.model.Player
import com.example.tictactoe_v02.data.repository.GameRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository = GameRepository()) : ViewModel() {

    val localPlayerId = mutableStateOf<String?>(null)
    val lastSentChallengeId = mutableStateOf<String?>(null)
    val players: StateFlow<Map<String, Player>> = repository.players
    val games: StateFlow<Map<String, Game>> = repository.games

    init {
        repository.initializeListeners()
    }

    // Check if player exists or create one
    fun createOrFetchPlayer(name: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val playerId = repository.createOrFetchPlayer(name)
                localPlayerId.value = playerId
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // Send a challenge, stay in lobby (do not navigate yet)
    fun invitePlayer(player2Id: String, onComplete: (Boolean, String?) -> Unit) {
        val currentPlayerId = localPlayerId.value ?: return

        viewModelScope.launch {
            try {
                val gameId = repository.createGameInvitation(currentPlayerId, player2Id)
                lastSentChallengeId.value = gameId  // track it here
                onComplete(true, gameId)
            } catch (e: Exception) {
                onComplete(false, null)
            }
        }
    }

    fun acceptInvitation(gameId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.acceptGameInvitation(gameId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun declineInvitation(gameId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.declineGameInvitation(gameId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun makeMove(gameId: String, cellIndex: Int, onComplete: (Boolean) -> Unit) {
        val currentPlayerId = localPlayerId.value ?: return

        viewModelScope.launch {
            try {
                repository.makeMove(gameId, cellIndex, currentPlayerId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun isMyTurn(game: Game): Boolean {
        val currentPlayerId = localPlayerId.value ?: return false

        return (game.gameState == GameState.PLAYER1_TURN && game.player1Id == currentPlayerId) ||
                (game.gameState == GameState.PLAYER2_TURN && game.player2Id == currentPlayerId)
    }

    fun getCurrentPlayerName(): String {
        val currentPlayerId = localPlayerId.value ?: return "Unknown"
        return players.value[currentPlayerId]?.name ?: "Unknown"
    }

    // Get list of pending games where I am the challenger
    fun getPendingChallenges(): List<Game> {
        val currentPlayerId = localPlayerId.value ?: return emptyList()
        return games.value.values.filter {
            it.player1Id == currentPlayerId && it.gameState == GameState.INVITE
        }
    }

    // Get list of declined games where I am the challenger
    fun getDeclinedChallenges(): List<Game> {
        val currentPlayerId = localPlayerId.value ?: return emptyList()
        return games.value.values.filter {
            it.player1Id == currentPlayerId && it.gameState == GameState.DECLINED
        }
    }

    // Get list of accepted/active games where I am involved
    fun getActiveGames(): List<Game> {
        val currentPlayerId = localPlayerId.value ?: return emptyList()
        return games.value.values.filter {
            (it.player1Id == currentPlayerId || it.player2Id == currentPlayerId) &&
                    (it.gameState == GameState.PLAYER1_TURN || it.gameState == GameState.PLAYER2_TURN)
        }
    }
}
