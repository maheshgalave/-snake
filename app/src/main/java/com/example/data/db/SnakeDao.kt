package com.example.data.db

import androidx.room.*
import com.example.data.model.GameMode
import com.example.data.model.HighScore
import com.example.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow

@Dao
interface SnakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(highScore: HighScore): Long

    @Query("SELECT * FROM high_scores WHERE mode = :mode ORDER BY score DESC LIMIT 10")
    fun getHighScoresForMode(mode: GameMode): Flow<List<HighScore>>

    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 20")
    fun getAllHighScores(): Flow<List<HighScore>>

    @Query("SELECT MAX(score) FROM high_scores WHERE mode = :mode")
    suspend fun getBestScore(mode: GameMode): Int?

    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    fun getPlayerStats(): Flow<PlayerStats?>

    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    suspend fun getPlayerStatsDirect(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: PlayerStats)
}
