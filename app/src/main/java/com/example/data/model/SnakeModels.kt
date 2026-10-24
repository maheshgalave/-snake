package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(other: Direction): Boolean {
        return (dx + other.dx == 0) && (dy + other.dy == 0)
    }
}

data class GridPoint(val x: Int, val y: Int)

enum class FoodType(val points: Int, val colorHex: Long, val label: String) {
    APPLE(10, 0xFFFF0055, "Apple"),
    GOLDEN(50, 0xFFFFD700, "Golden"),
    SPEED(25, 0xFF00E5FF, "Speed"),
    FREEZE(20, 0xFF7C4DFF, "Freeze")
}

data class FoodItem(
    val point: GridPoint,
    val type: FoodType,
    val expiresAt: Long? = null // For timed special fruits
)

enum class GameMode(val title: String, val description: String) {
    CLASSIC("Classic Arcade", "Traditional rules. Walls are lethal."),
    PORTAL("Portal Wrap", "Wrap seamlessly through opposite walls."),
    SPEED_RUSH("Speed Rush", "Speed ramps up rapidly with bonus timer fruit."),
    MAZE("Obstacle Maze", "Dodge stationary cyber barriers and hazards.")
}

enum class SnakeSkin(val title: String, val headColor: Long, val bodyColor: Long, val glowColor: Long) {
    NEON_CYAN("Neon Cyber", 0xFF00F0FF, 0xFF00A3FF, 0x6600F0FF),
    EMERALD_VIPER("Emerald Viper", 0xFF00FF66, 0xFF00CC44, 0x6600FF66),
    MAGMA_DRAGON("Magma Dragon", 0xFFFF3300, 0xFFFF9900, 0x66FF3300),
    VIOLET_PULSE("Violet Pulse", 0xFFB026FF, 0xFF7B00FF, 0x66B026FF),
    GOLDEN_EMPEROR("Golden Emperor", 0xFFFFD700, 0xFFFFA000, 0x66FFD700)
}

enum class GameTheme(val title: String, val bgDark: Long, val gridColor: Long) {
    CYBER_NIGHT("Cyber Night", 0xFF070B19, 0xFF141F36),
    NEO_MATRIX("Neo Matrix", 0xFF031408, 0xFF0A2E14),
    MIDNIGHT_AMOLED("Midnight AMOLED", 0xFF000000, 0xFF121212),
    RETRO_ARCADE("Retro Arcade", 0xFF1A0B2E, 0xFF2E174D)
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Long,
    val size: Float,
    val alpha: Float,
    val life: Float // 1.0 down to 0.0
)

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: GameMode,
    val score: Int,
    val snakeLength: Int,
    val applesEaten: Int,
    val skinUsed: SnakeSkin,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val gamesPlayed: Int = 0,
    val totalApplesEaten: Int = 0,
    val bestScore: Int = 0,
    val longestSnake: Int = 3,
    val totalPlayTimeSec: Long = 0
)
