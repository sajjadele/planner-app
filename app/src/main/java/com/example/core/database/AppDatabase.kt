package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.goal.GoalDao
import com.example.core.goal.GoalEntity
import com.example.plugins.notes.data.NoteDao
import com.example.plugins.notes.data.NoteEntity
import com.example.plugins.planner.data.InsightDao
import com.example.plugins.planner.data.TaskDao
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventDao
import com.example.plugins.planner.data.TaskEventEntity

@Database(
    entities = [
        ModuleSettingsEntity::class,
        GoalEntity::class,
        TaskEntity::class,
        TaskEventEntity::class,
        NoteEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleSettingsDao(): ModuleSettingsDao
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun taskEventDao(): TaskEventDao
    abstract fun insightDao(): InsightDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vision_planner_database"
                )
                .fallbackToDestructiveMigration() // safe for local offline dev versions
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
