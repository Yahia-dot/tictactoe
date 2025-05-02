package com.example.tictactoe_v02.data.model

/**
 * Represents the state of a TicTacToe game
 */
data class Game(
    val id: String = "",
    val gameBoard: List<Int> = List(9) { 0 }, // 0: empty, 1: player1's move, 2: player2's move
    val gameState: GameState = GameState.INVITE,
    val player1Id: String = "",
    val player2Id: String = ""
)

/**
 * Possible states of a TicTacToe game
 */
enum class GameState {
    INVITE,         // Waiting for player2 to accept invitation
    PLAYER1_TURN,   // Player1's turn to move
    PLAYER2_TURN,   // Player2's turn to move
    PLAYER1_WON,    // Player1 has won the game
    PLAYER2_WON,    // Player2 has won the game
    DRAW,           // The game ended in a draw
    DECLINED        // Player2 declined the invitation
}