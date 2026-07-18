package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.goal.GoalDao
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalEventDao
import com.example.core.goal.GoalEventEntity
import com.example.core.snapshot.BehaviorSnapshotEntity
import com.example.core.snapshot.GoalProgressSnapshotEntity
import com.example.core.snapshot.SnapshotDao
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
        GoalEventEntity::class,
        GoalProgressSnapshotEntity::class,
        BehaviorSnapshotEntity::class,
        TaskEntity::class,
        TaskEventEntity::class,
        NoteEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleSettingsDao(): ModuleSettingsDao
    abstract fun goalDao(): GoalDao
    abstract fun goalEventDao(): GoalEventDao
    abstract fun snapshotDao(): SnapshotDao
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

        /**
         * v6 → v7: Drop the denormalized `goalName` cache column.
         * Goal linkage is now resolved exclusively via the `goalId` FK.
         * Recreates the table to remove the column (SQLite cannot DROP COLUMN
         * on all supported API levels); all other columns and rows are preserved.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks RENAME TO tasks_old")
                db.execSQL("""
                    CREATE TABLE tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        priority TEXT,
                        isCompleted INTEGER NOT NULL,
                        dateEpochMs INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        reminderHour INTEGER,
                        reminderMinute INTEGER,
                        goalId INTEGER,
                        lifeAreaId INTEGER,
                        valueTag TEXT,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO tasks (id, title, priority, isCompleted, dateEpochMs, timestamp,
                                       reminderHour, reminderMinute, goalId, lifeAreaId, valueTag)
                    SELECT id, title, priority, isCompleted, dateEpochMs, timestamp,
                           reminderHour, reminderMinute, goalId, lifeAreaId, valueTag
                    FROM tasks_old
                """.trimIndent())
                db.execSQL("DROP TABLE tasks_old")
            }
        }

        /**
         * v7 → v8: Add the `goal_events` table for goal lifecycle signal logging
         * (created/completed/paused/resumed/abandoned). No existing table is modified;
         * the table is created fresh and empty for new installs and migrators alike.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goal_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_events_goalId ON goal_events(goalId)")
            }
        }

        /**
         * v8 → v9: Add the Phase 3 progress/behavior projections.
         *
         * Two new tables, both rebuildable from existing `tasks`/`task_events` signal:
         * - `goal_progress_snapshot` (per goal, per day): completed/total/rate.
         * - `behavior_snapshot` (per day): completed/created/streak/velocity/rescheduleRate.
         * No existing table is modified; the tables are created fresh and empty for new installs
         * and migrators alike. Historical rows are filled by the App-Launch Backfill Engine.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goal_progress_snapshot (
                        dateEpochMs INTEGER NOT NULL,
                        goalId INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        total INTEGER NOT NULL,
                        rate REAL NOT NULL,
                        PRIMARY KEY(dateEpochMs, goalId),
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_progress_snapshot_goalId ON goal_progress_snapshot(goalId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS behavior_snapshot (
                        dateEpochMs INTEGER NOT NULL PRIMARY KEY,
                        completed INTEGER NOT NULL,
                        created INTEGER NOT NULL,
                        streak INTEGER NOT NULL,
                        velocity TEXT NOT NULL,
                        rescheduleRate REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * v9 → v10: Phase 5.1 Goal Foundation.
         *
         * Additive, non-destructive columns on `goals`:
         * - `why TEXT`        (optional motivation; nullable)
         * - `deadlineEpochMs INTEGER` (optional deadline; nullable)
         *
         * `archived` is introduced as a new status *value* but does not require a schema change:
         * `status` is already a free-form TEXT column, so no enum/column migration is needed.
         * No tables are created or dropped. `fallbackToDestructiveMigration()` remains the safety net.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN why TEXT")
                db.execSQL("ALTER TABLE goals ADD COLUMN deadlineEpochMs INTEGER")
            }
        }

        /**
         * v10 → v11: Phase 5.4 performance indexes (see ADR-0009).
         *
         * Purely additive secondary indexes — no schema/column change, fully non-destructive:
         * - `tasks(dateEpochMs)`      → speeds day-scoped + insight queries (BETWEEN on scheduled day)
         * - `task_events(eventType)`  → speeds reschedule/completion aggregations (WHERE eventType)
         * - `goals(status)`           → speeds status-filtered goal/list loads
         *
         * `CREATE INDEX IF NOT EXISTS` is idempotent so a re-run is safe. `fallbackToDestructiveMigration()`
         * remains the safety net for any unhandled jump.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dateEpochMs ON tasks(dateEpochMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_events_eventType ON task_events(eventType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_status ON goals(status)")
            }
        }

        /**
         * v11 → v12: Phase 6.5.6 task deadline.
         *
         * Additive, non-destructive column on `tasks`:
         * - `deadlineEpochMs INTEGER` (optional due date; nullable)
         *
         * Mirrors `goals.deadlineEpochMs` (added in v9→v10). Nullable so no default is required and
         * existing rows are preserved untouched. `fallbackToDestructiveMigration()` remains the safety net.
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN deadlineEpochMs INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vision_planner_database"
                )
                .addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
