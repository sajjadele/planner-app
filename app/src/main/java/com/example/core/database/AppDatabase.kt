package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
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

        /**
         * v5 → v6: Replace dayIndex (0-6, week-bound) with dateEpochMs (absolute date).
         *
         * Backfill: compute the Saturday of the task's creation week from `timestamp`,
         * then add dayIndex days. Uses UTC-based epoch math — tasks created near
         * local-midnight boundaries (< offset-from-UTC) may land ±1 day in rare cases.
         * This is acceptable for a one-time migration; fallbackToDestructiveMigration()
         * remains the safety net.
         *
         * strftime('%w') returns 0=Sunday … 6=Saturday.
         * (dow + 1) % 7 converts to Persian offset (0=Saturday … 6=Friday).
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN dateEpochMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE tasks SET dateEpochMs = (
                        (timestamp / 86400000 -
                         ((CAST(strftime('%w', timestamp / 1000, 'unixepoch') AS INTEGER) + 1) % 7) +
                         dayIndex) * 86400000
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vision_planner_database"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
