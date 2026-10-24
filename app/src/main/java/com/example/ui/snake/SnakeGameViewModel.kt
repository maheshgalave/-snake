package com.example.ui.snake

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.SnakeDao
import com.example.data.db.TaskFlowDatabase
import com.example.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ScreenState {
    TITLE,
    PLAYING,
    PAUSED,
    GAME_OVER,
    LEADERBOARD,
    SKIN_SELECT
}

data class SnakeGameState(
    val screenState: ScreenState = ScreenState.TITLE,
    val mode: GameMode = GameMode.CLASSIC,
    val skin: SnakeSkin = SnakeSkin.NEON_CYAN,
    val theme: GameTheme = GameTheme.CYBER_NIGHT,
    val gridWidth: Int = 20,
    val gridHeight: Int = 24,
    val snake: List<GridPoint> = listOf(GridPoint(10, 12), GridPoint(10, 13), GridPoint(10, 14)),
    val currentDirection: Direction = Direction.UP,
    val food: FoodItem = FoodItem(GridPoint(10, 6), FoodType.APPLE),
    val bonusFood: FoodItem? = null,
    val obstacles: List<GridPoint> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val multiplier: Float = 1.0f,
    val comboCount: Int = 0,
    val lastEatTime: Long = 0,
    val applesEaten: Int = 0,
    val speedMs: Long = 140L,
    val screenShake: Float = 0f,
    val particles: List<Particle> = emptyList(),
    val showDpad: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val gameStartTime: Long = 0,
    val gameDurationSec: Long = 0
)

class SnakeGameViewModel(application: Application) : AndroidViewModel(application) {

    private val snakeDao: SnakeDao

    private val _gameState = MutableStateFlow(SnakeGameState())
    val gameState: StateFlow<SnakeGameState> = _gameState.asStateFlow()

    private val directionBuffer = ArrayDeque<Direction>()
    private var gameLoopJob: Job? = null
    private var particleLoopJob: Job? = null

    val highScores: StateFlow<List<HighScore>>
    val playerStats: StateFlow<PlayerStats?>

    init {
        val db = TaskFlowDatabase.getDatabase(application)
        snakeDao = db.snakeDao()

        highScores = snakeDao.getAllHighScores()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        playerStats = snakeDao.getPlayerStats()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        // Load initial high score for default mode
        viewModelScope.launch {
            val best = snakeDao.getBestScore(GameMode.CLASSIC) ?: 0
            _gameState.update { it.copy(highScore = best) }
        }

        startParticleLoop()
    }

    fun setScreenState(state: ScreenState) {
        if (state == ScreenState.PLAYING && _gameState.value.screenState != ScreenState.PAUSED) {
            startNewGame()
        } else if (state == ScreenState.PLAYING && _gameState.value.screenState == ScreenState.PAUSED) {
            resumeGame()
        } else if (state == ScreenState.PAUSED) {
            pauseGame()
        } else {
            _gameState.update { it.copy(screenState = state) }
        }
    }

    fun selectGameMode(mode: GameMode) {
        viewModelScope.launch {
            val best = snakeDao.getBestScore(mode) ?: 0
            _gameState.update { it.copy(mode = mode, highScore = best) }
        }
    }

    fun selectSkin(skin: SnakeSkin) {
        _gameState.update { it.copy(skin = skin) }
    }

    fun selectTheme(theme: GameTheme) {
        _gameState.update { it.copy(theme = theme) }
    }

    fun toggleDpad() {
        _gameState.update { it.copy(showDpad = !it.showDpad) }
    }

    fun toggleSound() {
        _gameState.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun toggleHaptic() {
        _gameState.update { it.copy(hapticEnabled = !it.hapticEnabled) }
    }

    fun queueDirection(newDir: Direction) {
        val lastBuffered = directionBuffer.lastOrNull() ?: _gameState.value.currentDirection
        if (!newDir.isOpposite(lastBuffered) && newDir != lastBuffered && directionBuffer.size < 3) {
            directionBuffer.add(newDir)
        }
    }

    fun startNewGame() {
        directionBuffer.clear()
        val mode = _gameState.value.mode
        val width = _gameState.value.gridWidth
        val height = _gameState.value.gridHeight

        val initialSnake = listOf(
            GridPoint(width / 2, height / 2),
            GridPoint(width / 2, height / 2 + 1),
            GridPoint(width / 2, height / 2 + 2)
        )

        val obstacles = if (mode == GameMode.MAZE) generateObstacles(width, height, initialSnake) else emptyList()
        val foodPoint = findEmptyGridPoint(initialSnake, obstacles, width, height)

        viewModelScope.launch {
            val best = snakeDao.getBestScore(mode) ?: 0
            _gameState.update {
                it.copy(
                    screenState = ScreenState.PLAYING,
                    snake = initialSnake,
                    currentDirection = Direction.UP,
                    food = FoodItem(foodPoint, FoodType.APPLE),
                    bonusFood = null,
                    obstacles = obstacles,
                    score = 0,
                    highScore = best,
                    isNewHighScore = false,
                    multiplier = 1.0f,
                    comboCount = 0,
                    applesEaten = 0,
                    speedMs = if (mode == GameMode.SPEED_RUSH) 110L else 140L,
                    screenShake = 0f,
                    particles = emptyList(),
                    gameStartTime = System.currentTimeMillis()
                )
            }
            startGameLoop()
        }
    }

    fun pauseGame() {
        gameLoopJob?.cancel()
        _gameState.update { it.copy(screenState = ScreenState.PAUSED) }
    }

    fun resumeGame() {
        _gameState.update { it.copy(screenState = ScreenState.PLAYING) }
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive && _gameState.value.screenState == ScreenState.PLAYING) {
                val delayTime = _gameState.value.speedMs
                delay(delayTime)
                stepGame()
            }
        }
    }

    private fun stepGame() {
        val state = _gameState.value
        val dir = if (directionBuffer.isNotEmpty()) directionBuffer.removeFirst() else state.currentDirection

        val head = state.snake.first()
        var newX = head.x + dir.dx
        var newY = head.y + dir.dy

        // Wall handling based on GameMode
        if (state.mode == GameMode.PORTAL) {
            newX = (newX + state.gridWidth) % state.gridWidth
            newY = (newY + state.gridHeight) % state.gridHeight
        } else {
            // Check wall collision
            if (newX < 0 || newX >= state.gridWidth || newY < 0 || newY >= state.gridHeight) {
                triggerGameOver()
                return
            }
        }

        val newHead = GridPoint(newX, newY)

        // Check obstacle collision in Maze mode
        if (state.obstacles.contains(newHead)) {
            triggerGameOver()
            return
        }

        // Check self collision (ignoring tail tip since it moves away unless growing)
        val willEat = (newHead == state.food.point) || (state.bonusFood != null && newHead == state.bonusFood.point)
        val bodyToCheck = if (willEat) state.snake else state.snake.dropLast(1)
        if (bodyToCheck.contains(newHead)) {
            triggerGameOver()
            return
        }

        // Move snake
        val newSnake = mutableListOf(newHead)
        newSnake.addAll(state.snake)
        if (!willEat) {
            newSnake.removeAt(newSnake.size - 1)
        }

        var newScore = state.score
        var newApplesEaten = state.applesEaten
        var newMultiplier = state.multiplier
        var newCombo = state.comboCount
        var newFood = state.food
        var newBonusFood = state.bonusFood
        var newSpeed = state.speedMs
        val now = System.currentTimeMillis()

        // Eating check
        if (newHead == state.food.point) {
            val pts = (state.food.type.points * state.multiplier).toInt()
            newScore += pts
            newApplesEaten++

            // Combo multiplier (streak if eaten within 3.5 seconds)
            if (now - state.lastEatTime < 3500) {
                newCombo++
                newMultiplier = (1.0f + (newCombo * 0.2f)).coerceAtMost(3.0f)
            } else {
                newCombo = 1
                newMultiplier = 1.0f
            }

            // Spawn particles
            spawnFoodParticles(newHead, state.food.type.colorHex)

            // Spawn next food
            val nextFoodPt = findEmptyGridPoint(newSnake, state.obstacles, state.gridWidth, state.gridHeight)
            newFood = FoodItem(nextFoodPt, FoodType.APPLE)

            // Speed progression
            if (state.mode == GameMode.SPEED_RUSH) {
                newSpeed = (newSpeed - 3).coerceAtLeast(45L)
            } else {
                newSpeed = (140L - (newApplesEaten * 1.5).toLong()).coerceAtLeast(65L)
            }

            // Chance to spawn special bonus fruit (Golden, Speed, Freeze)
            if (newBonusFood == null && Random.nextFloat() < 0.35f) {
                val bonusType = when (Random.nextInt(3)) {
                    0 -> FoodType.GOLDEN
                    1 -> FoodType.SPEED
                    else -> FoodType.FREEZE
                }
                val bonusPt = findEmptyGridPoint(newSnake + nextFoodPt, state.obstacles, state.gridWidth, state.gridHeight)
                newBonusFood = FoodItem(bonusPt, bonusType, expiresAt = now + 9000L)
            }
        } else if (state.bonusFood != null && newHead == state.bonusFood.point) {
            val pts = (state.bonusFood.type.points * state.multiplier).toInt()
            newScore += pts
            spawnFoodParticles(newHead, state.bonusFood.type.colorHex)

            if (state.bonusFood.type == FoodType.FREEZE) {
                newSpeed = (newSpeed + 30).coerceAtMost(160L) // temporary freeze
            }

            newBonusFood = null
        }

        // Expire bonus food if timed out
        if (newBonusFood != null && newBonusFood.expiresAt != null && now > newBonusFood.expiresAt) {
            newBonusFood = null
        }

        // Check high score
        val isNewRecord = newScore > state.highScore && state.highScore > 0

        _gameState.update {
            it.copy(
                snake = newSnake,
                currentDirection = dir,
                food = newFood,
                bonusFood = newBonusFood,
                score = newScore,
                highScore = if (newScore > it.highScore) newScore else it.highScore,
                isNewHighScore = isNewRecord,
                multiplier = newMultiplier,
                comboCount = newCombo,
                applesEaten = newApplesEaten,
                lastEatTime = if (willEat) now else it.lastEatTime,
                speedMs = newSpeed
            )
        }
    }

    private fun triggerGameOver() {
        gameLoopJob?.cancel()
        val current = _gameState.value
        val playDuration = (System.currentTimeMillis() - current.gameStartTime) / 1000L

        // Screen shake
        _gameState.update {
            it.copy(
                screenState = ScreenState.GAME_OVER,
                screenShake = 16f,
                gameDurationSec = playDuration
            )
        }

        // Persist high score and update player stats
        viewModelScope.launch {
            if (current.score > 0) {
                snakeDao.insertHighScore(
                    HighScore(
                        mode = current.mode,
                        score = current.score,
                        snakeLength = current.snake.size,
                        applesEaten = current.applesEaten,
                        skinUsed = current.skin
                    )
                )
            }

            val existingStats = snakeDao.getPlayerStatsDirect() ?: PlayerStats()
            snakeDao.insertOrUpdateStats(
                existingStats.copy(
                    gamesPlayed = existingStats.gamesPlayed + 1,
                    totalApplesEaten = existingStats.totalApplesEaten + current.applesEaten,
                    bestScore = maxOf(existingStats.bestScore, current.score),
                    longestSnake = maxOf(existingStats.longestSnake, current.snake.size),
                    totalPlayTimeSec = existingStats.totalPlayTimeSec + playDuration
                )
            )
        }
    }

    private fun spawnFoodParticles(point: GridPoint, colorHex: Long) {
        val newParticles = mutableListOf<Particle>()
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 4.5f + 1.5f
            newParticles.add(
                Particle(
                    x = point.x + 0.5f,
                    y = point.y + 0.5f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = colorHex,
                    size = Random.nextFloat() * 6f + 3f,
                    alpha = 1.0f,
                    life = 1.0f
                )
            )
        }
        _gameState.update { it.copy(particles = it.particles + newParticles) }
    }

    private fun startParticleLoop() {
        particleLoopJob?.cancel()
        particleLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(16) // ~60fps
                if (_gameState.value.particles.isNotEmpty() || _gameState.value.screenShake > 0f) {
                    _gameState.update { state ->
                        val updated = state.particles.mapNotNull { p ->
                            val newLife = p.life - 0.045f
                            if (newLife <= 0) null
                            else p.copy(
                                x = p.x + p.vx * 0.05f,
                                y = p.y + p.vy * 0.05f,
                                alpha = newLife,
                                life = newLife
                            )
                        }
                        val newShake = (state.screenShake * 0.85f).let { if (it < 0.5f) 0f else it }
                        state.copy(particles = updated, screenShake = newShake)
                    }
                }
            }
        }
    }

    private fun findEmptyGridPoint(
        snake: List<GridPoint>,
        obstacles: List<GridPoint>,
        width: Int,
        height: Int
    ): GridPoint {
        val occupied = (snake + obstacles).toSet()
        var candidate: GridPoint
        var attempts = 0
        do {
            candidate = GridPoint(Random.nextInt(width), Random.nextInt(height))
            attempts++
        } while (occupied.contains(candidate) && attempts < 200)
        return candidate
    }

    private fun generateObstacles(width: Int, height: Int, snake: List<GridPoint>): List<GridPoint> {
        val obstacles = mutableListOf<GridPoint>()
        val safeZone = snake.toSet()
        // Generate 4-6 obstacle clusters
        for (i in 0 until 5) {
            val ox = Random.nextInt(2, width - 2)
            val oy = Random.nextInt(2, height - 2)
            for (dx in 0..1) {
                val pt = GridPoint(ox + dx, oy)
                if (!safeZone.contains(pt)) obstacles.add(pt)
            }
        }
        return obstacles.distinct()
    }
}
