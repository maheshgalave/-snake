package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        UserAccount::class,
        Project::class,
        TaskItem::class,
        ActivityLog::class,
        TeamMember::class,
        Invoice::class,
        HighScore::class,
        PlayerStats::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskFlowDatabase : RoomDatabase() {
    abstract fun taskFlowDao(): TaskFlowDao
    abstract fun snakeDao(): SnakeDao

    companion object {
        @Volatile
        private var INSTANCE: TaskFlowDatabase? = null

        fun getDatabase(context: Context): TaskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskFlowDatabase::class.java,
                    "taskflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
