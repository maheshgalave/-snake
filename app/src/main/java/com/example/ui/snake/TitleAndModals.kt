package com.example.ui.snake

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TitleScreen(
    gameState: SnakeGameState,
    onStartGame: () -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSkins: () -> Unit,
    onToggleDpad: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val headColor = Color(gameState.skin.headColor)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(gameState.theme.bgDark))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Controls Bar (Sound, Haptic, DPad toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High Score Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BEST: ${gameState.highScore}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onToggleDpad) {
                        Icon(
                            imageVector = if (gameState.showDpad) Icons.Default.Gamepad else Icons.Outlined.Gamepad,
                            contentDescription = "Toggle D-Pad",
                            tint = if (gameState.showDpad) headColor else Color.White.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onToggleSound) {
                        Icon(
                            imageVector = if (gameState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = if (gameState.soundEnabled) headColor else Color.White.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onToggleHaptic) {
                        Icon(
                            imageVector = if (gameState.hapticEnabled) Icons.Default.Vibration else Icons.Outlined.Vibration,
                            contentDescription = "Toggle Haptics",
                            tint = if (gameState.hapticEnabled) headColor else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Center Lockup & Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glowing Cyber Emblem
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(headColor.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                        .border(2.dp, headColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = headColor,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "CYBER SNAKE",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = "60 FPS · NEON JUICE · RETRO ARCADE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = headColor
                )
            }

            // Game Mode Selection Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SELECT GAME MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GameMode.values()) { mode ->
                        val isSelected = gameState.mode == mode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) headColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) headColor else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectMode(mode) }
                                .testTag("mode_select_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) headColor else Color.White
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Start CTA & Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(pulseScale)
                        .testTag("start_game_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = headColor)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START GAME",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color.Black
                    )
                }

                // Sub-buttons: Skins & Leaderboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSkins,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("title_skins_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Skins (${gameState.skin.title})", color = Color.White, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenLeaderboard,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("title_leaderboard_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Leaderboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("High Scores", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Control hint line
                Text(
                    text = "Swipe to Move · Screen Taps to Steer · Pause with II",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GameOverDialog(
    gameState: SnakeGameState,
    onPlayAgain: () -> Unit,
    onTitleScreen: () -> Unit
) {
    val headColor = Color(gameState.skin.headColor)

    AlertDialog(
        onDismissRequest = {},
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFF3366)
                    )
                )
                if (gameState.isNewHighScore) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "★ NEW HIGH SCORE! ★",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Score card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatRow("Final Score", String.format("%04d", gameState.score), isHighlight = true)
                        StatRow("Apples Collected", gameState.applesEaten.toString())
                        StatRow("Max Snake Length", gameState.snake.size.toString())
                        StatRow("Time Survived", "${gameState.gameDurationSec}s")
                        StatRow("Game Mode", gameState.mode.title)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_over_play_again_button"),
                colors = ButtonDefaults.buttonColors(containerColor = headColor)
            ) {
                Text(
                    text = "PLAY AGAIN",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onTitleScreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Main Menu")
            }
        }
    )
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onTitleScreen: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onResume,
        title = {
            Text(
                text = "GAME PAUSED",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Catch your breath! Resume when you are ready to continue your run.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resume Game")
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onRestart) {
                    Text("Restart")
                }
                TextButton(onClick = onTitleScreen) {
                    Text("Quit to Menu")
                }
            }
        }
    )
}

@Composable
fun LeaderboardScreen(
    highScores: List<HighScore>,
    playerStats: PlayerStats?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "HALL OF FAME",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }

        // Lifetime Player Stats
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PLAYER CAREER STATS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox("Games", (playerStats?.gamesPlayed ?: 0).toString())
                    StatBox("Total Apples", (playerStats?.totalApplesEaten ?: 0).toString())
                    StatBox("Max Length", (playerStats?.longestSnake ?: 3).toString())
                    StatBox("Play Time", "${(playerStats?.totalPlayTimeSec ?: 0) / 60}m")
                }
            }
        }

        Text(
            text = "TOP HIGH SCORES",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (highScores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No runs logged yet! Play a round to claim the crown.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(highScores.take(15)) { record ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(record.skinUsed.headColor))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${record.mode.title} · ${record.skinUsed.title}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${record.applesEaten} apples · Length: ${record.snakeLength} · ${sdf.format(Date(record.timestamp))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = String.format("%04d", record.score),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(record.skinUsed.headColor)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkinSelectScreen(
    currentSkin: SnakeSkin,
    currentTheme: GameTheme,
    onSelectSkin: (SnakeSkin) -> Unit,
    onSelectTheme: (GameTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SKINS & THEMES",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }

        Text("SNAKE SKINS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(SnakeSkin.values()) { skin ->
                val isSelected = skin == currentSkin
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color(skin.headColor).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Color(skin.headColor) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSkin(skin) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(skin.headColor))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(skin.title, fontWeight = FontWeight.Bold)
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Equipped", tint = Color(skin.headColor))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("GAME THEMES", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }

            items(GameTheme.values()) { theme ->
                val isSelected = theme == currentTheme
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTheme(theme) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(theme.title, fontWeight = FontWeight.Bold)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                fontSize = if (isHighlight) 18.sp else 14.sp
            ),
            color = if (isHighlight) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
