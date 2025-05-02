package com.example.tictactoe_v02

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tictactoe_v02.ui.theme.TicTacToeV02Theme
import com.example.tictactoe_v02.ui.navigation.TicTacToeApp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicTacToeV02Theme {
                TicTacToeApp()
            }
        }
    }
}