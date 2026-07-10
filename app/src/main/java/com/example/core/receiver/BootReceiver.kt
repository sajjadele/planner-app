package com.example.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules all active task reminders after device reboot.
 * Android clears all AlarmManager alarms on reboot, so this receiver
 * restores them from the database.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val tasks = database.taskDao().getActiveReminders()
                for (task in tasks) {
                    if (task.reminderHour != null && task.reminderMinute != null && !task.isCompleted) {
                        ReminderScheduler.schedule(context, task)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
