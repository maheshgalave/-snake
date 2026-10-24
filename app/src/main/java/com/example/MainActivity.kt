package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.snake.*
import com.example.ui.theme.TaskFlowTheme

class MainActivity : ComponentActivity() {

    private val snakeViewModel: SnakeGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaskFlowTheme(darkTheme = true) {
                CyberSnakeApp(viewModel = snakeViewModel)
            }
        }
    }
}

@Composable
fun CyberSnakeApp(viewModel: SnakeGameViewModel) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val highScores by viewModel.highScores.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()

    BackHandler(enabled = gameState.screenState != ScreenState.TITLE) {
        when (gameState.screenState) {
            ScreenState.PLAYING -> viewModel.pauseGame()
            ScreenState.PAUSED, ScreenState.GAME_OVER -> viewModel.setScreenState(ScreenState.TITLE)
            ScreenState.LEADERBOARD, ScreenState.SKIN_SELECT -> viewModel.setScreenState(ScreenState.TITLE)
            ScreenState.TITLE -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(gameState.theme.bgDark)
    ) {
        when (gameState.screenState) {
            ScreenState.TITLE -> {
                TitleScreen(
                    gameState = gameState,
                    onStartGame = { viewModel.setScreenState(ScreenState.PLAYING) },
                    onSelectMode = viewModel::selectGameMode,
                    onOpenLeaderboard = { viewModel.setScreenState(ScreenState.LEADERBOARD) },
                    onOpenSkins = { viewModel.setScreenState(ScreenState.SKIN_SELECT) },
                    onToggleDpad = viewModel::toggleDpad,
                    onToggleSound = viewModel::toggleSound,
                    onToggleHaptic = viewModel::toggleHaptic
                )
            }

            ScreenState.PLAYING, ScreenState.PAUSED, ScreenState.GAME_OVER -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(gameState.theme.bgDark))
                ) {
                    // Full-screen Canvas Playfield (85%+ Unobstructed)
                    SnakeCanvas(
                        gameState = gameState,
                        onDirectionChange = viewModel::queueDirection,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Low-occlusion perimeter status cluster and virtual D-Pad
                    GameHud(
                        gameState = gameState,
                        onDirectionClick = viewModel::queueDirection,
                        onPauseClick = viewModel::pauseGame
                    )

                    // Pause Modal Overlay
                    if (gameState.screenState == ScreenState.PAUSED) {
                        PauseDialog(
                            onResume = viewModel::resumeGame,
                            onRestart = viewModel::startNewGame,
                            onTitleScreen = { viewModel.setScreenState(ScreenState.TITLE) }
                        )
                    }

                    // Game Over Modal Overlay
                    if (gameState.screenState == ScreenState.GAME_OVER) {
                        GameOverDialog(
                            gameState = gameState,
                            onPlayAgain = viewModel::startNewGame,
                            onTitleScreen = { viewModel.setScreenState(ScreenState.TITLE) }
                        )
                    }
                }
            }

            ScreenState.LEADERBOARD -> {
                LeaderboardScreen(
                    highScores = highScores,
                    playerStats = playerStats,
                    onBack = { viewModel.setScreenState(ScreenState.TITLE) }
                )
            }

            ScreenState.SKIN_SELECT -> {
                SkinSelectScreen(
                    currentSkin = gameState.skin,
                    currentTheme = gameState.theme,
                    onSelectSkin = viewModel::selectSkin,
                    onSelectTheme = viewModel::selectTheme,
                    onBack = { viewModel.setScreenState(ScreenState.TITLE) }
                )
            }
        }
    }
}
