package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.plugins.notes.NoteDao
import com.example.plugins.notes.NoteEntity
import com.example.plugins.planner.TaskDao
import com.example.plugins.planner.TaskEntity

@Database(
    entities = [
        ModuleSettingsEntity::class,
        TaskEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleSettingsDao(): ModuleSettingsDao
    abstract fun taskDao(): TaskDao
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
