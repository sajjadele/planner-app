package com.example.core.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.plugins.planner.data.TaskEntity
import java.util.Calendar

object ReminderScheduler {
    fun schedule(context: Context, task: TaskEntity) {
        val hour = task.reminderHour ?: return
        val minute = task.reminderMinute ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_priority", task.priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val targetDayOfWeek = when (task.dayIndex) {
                0 -> Calendar.SATURDAY    // شنبه
                1 -> Calendar.SUNDAY      // یکشنبه
                2 -> Calendar.MONDAY      // دوشنبه
                3 -> Calendar.TUESDAY     // سه‌شنبه
                4 -> Calendar.WEDNESDAY   // چهارشنبه
                5 -> Calendar.THURSDAY    // پنج‌شنبه
                6 -> Calendar.FRIDAY      // جمعه
                else -> Calendar.SATURDAY
            }

            val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
            var daysDifference = targetDayOfWeek - currentDayOfWeek
            if (daysDifference < 0 || (daysDifference == 0 && timeInMillis <= System.currentTimeMillis())) {
                daysDifference += 7
            }
            add(Calendar.DAY_OF_YEAR, daysDifference)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for restricted permission environments
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
