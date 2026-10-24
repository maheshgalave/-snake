package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.TaskFlowDatabase
import com.example.data.model.GameMode
import com.example.data.model.HighScore
import com.example.data.model.PlayerStats
import com.example.data.model.SnakeSkin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: TaskFlowDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context matches Cyber Snake`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cyber Snake", appName)
    }

    @Test
    fun `test snake high scores insertion and query`() = runBlocking {
        val dao = db.snakeDao()

        val record1 = HighScore(
            mode = GameMode.CLASSIC,
            score = 350,
            snakeLength = 12,
            applesEaten = 15,
            skinUsed = SnakeSkin.NEON_CYAN
        )
        val id1 = dao.insertHighScore(record1)
        assertTrue(id1 > 0)

        val record2 = HighScore(
            mode = GameMode.CLASSIC,
            score = 720,
            snakeLength = 22,
            applesEaten = 30,
            skinUsed = SnakeSkin.EMERALD_VIPER
        )
        dao.insertHighScore(record2)

        val best = dao.getBestScore(GameMode.CLASSIC)
        assertEquals(720, best)
    }

    @Test
    fun `test snake player stats update`() = runBlocking {
        val dao = db.snakeDao()

        val initial = PlayerStats(
            gamesPlayed = 3,
            totalApplesEaten = 45,
            bestScore = 600,
            longestSnake = 18,
            totalPlayTimeSec = 180
        )
        dao.insertOrUpdateStats(initial)

        val fetched = dao.getPlayerStatsDirect()
        assertNotNull(fetched)
        assertEquals(3, fetched!!.gamesPlayed)
        assertEquals(45, fetched.totalApplesEaten)
        assertEquals(600, fetched.bestScore)
    }
}
