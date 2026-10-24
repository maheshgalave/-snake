package com.example.ui.snake

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Direction
import com.example.data.model.GameMode

@Composable
fun GameHud(
    gameState: SnakeGameState,
    onDirectionClick: (Direction) -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val accentColor = Color(gameState.skin.headColor)

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp)
    ) {
        // --- Top Perimeter Status Cluster ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score + Streak Multiplier
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format("%04d", gameState.score),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = accentColor
                        )
                    }

                    if (gameState.multiplier > 1.0f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "${String.format("%.1f", gameState.multiplier)}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // High Score
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Best",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "BEST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format("%04d", maxOf(gameState.highScore, gameState.score)),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // Pause Button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("pause_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // --- Bottom Virtual D-Pad (if enabled) ---
        if (gameState.showDpad) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // UP
                DpadButton(
                    direction = Direction.UP,
                    icon = Icons.Default.KeyboardArrowUp,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    if (gameState.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDirectionClick(Direction.UP)
                }

                // DOWN
                DpadButton(
                    direction = Direction.DOWN,
                    icon = Icons.Default.KeyboardArrowDown,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    if (gameState.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDirectionClick(Direction.DOWN)
                }

                // LEFT
                DpadButton(
                    direction = Direction.LEFT,
                    icon = Icons.Default.KeyboardArrowLeft,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    if (gameState.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDirectionClick(Direction.LEFT)
                }

                // RIGHT
                DpadButton(
                    direction = Direction.RIGHT,
                    icon = Icons.Default.KeyboardArrowRight,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    if (gameState.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDirectionClick(Direction.RIGHT)
                }

                // Center visual pivot
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun DpadButton(
    direction: Direction,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("dpad_btn_${direction.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = direction.name,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
