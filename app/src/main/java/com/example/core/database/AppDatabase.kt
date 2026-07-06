package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.plugins.notes.NoteDao
import com.example.plugins.notes.NoteEntity
import com.example.plugins.planner.TaskDao
import com.example.plugins.planner.TaskEntity
import com.example.plugins.planner.TaskEventDao
import com.example.plugins.planner.TaskEventEntity

@Database(
    entities = [
        ModuleSettingsEntity::class,
        TaskEntity::class,
        TaskEventEntity::class,
        NoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleSettingsDao(): ModuleSettingsDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun taskEventDao(): TaskEventDao

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
