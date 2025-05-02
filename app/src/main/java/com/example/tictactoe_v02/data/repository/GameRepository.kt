package com.example.tictactoe_v02.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.example.tictactoe_v02.data.model.Game
import com.example.tictactoe_v02.data.model.GameState
import com.example.tictactoe_v02.data.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class GameRepository {
    private val TAG = "GameRepository"
    private val db = Firebase.firestore

    private val _players = MutableStateFlow<Map<String, Player>>(emptyMap())
    val players: StateFlow<Map<String, Player>> = _players.asStateFlow()

    private val _games = MutableStateFlow<Map<String, Game>>(emptyMap())
    val games: StateFlow<Map<String, Game>> = _games.asStateFlow()

    fun initializeListeners() {
        db.collection(PLAYERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for player updates", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val updatedMap = snapshot.documents.associate { doc ->
                        val player = doc.toObject(Player::class.java) ?: Player()
                        doc.id to player.copy(id = doc.id)
                    }
                    _players.value = updatedMap
                }
            }

        db.collection(GAMES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for game updates", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val updatedMap = snapshot.documents.associate { doc ->
                        val game = doc.toObject(Game::class.java) ?: Game()
                        doc.id to game.copy(id = doc.id)
                    }
                    _games.value = updatedMap
                }
            }
    }

    // NEW: Check if player exists by name, or create new one
    suspend fun createOrFetchPlayer(name: String): String {
        try {
            val existingPlayer = db.collection(PLAYERS_COLLECTION)
                .whereEqualTo("name", name)
                .get()
                .await()
                .documents
                .firstOrNull()

            return if (existingPlayer != null) {
                existingPlayer.id
            } else {
                val newPlayer = hashMapOf("name" to name)
                val docRef = db.collection(PLAYERS_COLLECTION).add(newPlayer).await()
                docRef.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating or fetching player", e)
            throw e
        }
    }

    suspend fun createGameInvitation(player1Id: String, player2Id: String): String {
        try {
            val newGame = hashMapOf(
                "gameBoard" to List(9) { 0 },
                "gameState" to GameState.INVITE.name,
                "player1Id" to player1Id,
                "player2Id" to player2Id
            )

            val docRef = db.collection(GAMES_COLLECTION).add(newGame).await()
            return docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating game invitation", e)
            throw e
        }
    }

    suspend fun acceptGameInvitation(gameId: String) {
        try {
            db.collection(GAMES_COLLECTION).document(gameId)
                .update("gameState", GameState.PLAYER1_TURN.name)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting game invitation", e)
            throw e
        }
    }

    suspend fun declineGameInvitation(gameId: String) {
        try {
            db.collection(GAMES_COLLECTION).document(gameId)
                .update("gameState", GameState.DECLINED.name)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error declining game invitation", e)
            throw e
        }
    }


    suspend fun makeMove(gameId: String, cellIndex: Int, playerId: String) {
        try {
            val gameDoc = db.collection(GAMES_COLLECTION).document(gameId).get().await()
            val game = gameDoc.toObject(Game::class.java) ?: return

            val isPlayer1 = game.player1Id == playerId
            val isPlayer2 = game.player2Id == playerId
            val isPlayer1Turn = game.gameState == GameState.PLAYER1_TURN
            val isPlayer2Turn = game.gameState == GameState.PLAYER2_TURN

            if ((isPlayer1 && isPlayer1Turn) || (isPlayer2 && isPlayer2Turn)) {
                val board = game.gameBoard.toMutableList()
                if (board[cellIndex] != 0) return

                val playerMark = if (isPlayer1) 1 else 2
                board[cellIndex] = playerMark

                val newState = checkGameResult(board, playerMark)

                db.collection(GAMES_COLLECTION).document(gameId)
                    .update(
                        "gameBoard", board,
                        "gameState", newState.name
                    )
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making move", e)
            throw e
        }
    }

    private fun checkGameResult(board: List<Int>, playerMark: Int): GameState {
        for (i in 0..2) {
            if (board[i * 3] == playerMark && board[i * 3 + 1] == playerMark && board[i * 3 + 2] == playerMark) {
                return if (playerMark == 1) GameState.PLAYER1_WON else GameState.PLAYER2_WON
            }
        }

        for (i in 0..2) {
            if (board[i] == playerMark && board[i + 3] == playerMark && board[i + 6] == playerMark) {
                return if (playerMark == 1) GameState.PLAYER1_WON else GameState.PLAYER2_WON
            }
        }

        if (board[0] == playerMark && board[4] == playerMark && board[8] == playerMark) {
            return if (playerMark == 1) GameState.PLAYER1_WON else GameState.PLAYER2_WON
        }
        if (board[2] == playerMark && board[4] == playerMark && board[6] == playerMark) {
            return if (playerMark == 1) GameState.PLAYER1_WON else GameState.PLAYER2_WON
        }

        if (!board.contains(0)) {
            return GameState.DRAW
        }

        return if (playerMark == 1) GameState.PLAYER2_TURN else GameState.PLAYER1_TURN
    }

    companion object {
        const val PLAYERS_COLLECTION = "players"
        const val GAMES_COLLECTION = "games"
    }
}
