package com.example.task_manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Calendar
import java.util.Date

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ДОБАВЬТЕ ЭТОТ МЕТОД - отмена уведомлений для задачи
    fun cancelNotifications(taskId: Long) {
        // Отменяем напоминание
        val reminderIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "REMINDER"
        }
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)

        // Отменяем проверку просрочки
        val overdueIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "OVERDUE"
        }
        val overduePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 1000,
            overdueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(overduePendingIntent)

        android.util.Log.d("NotificationScheduler", "Cancelled notifications for task: $taskId")
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun calculateReminderTime(dueDate: Date, minutesBefore: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = dueDate
        calendar.add(Calendar.MINUTE, -minutesBefore)
        return calendar.time
    }

    fun scheduleReminder(task: Task): Boolean {
        if (task.reminderMinutes <= 0) {
            return false
        }

        val reminderTime = calculateReminderTime(task.dueDate, task.reminderMinutes)

        if (reminderTime.before(Date())) {
            android.util.Log.d("NotificationScheduler", "Reminder time is in the past")
            return false
        }

        if (!canScheduleExactAlarms()) {
            android.util.Log.d("NotificationScheduler", "Cannot schedule exact alarms")
            return false
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "REMINDER"
            putExtra("task_id", task.id)
            putExtra("task_name", task.name)
            putExtra("task_description", task.description)
            putExtra("task_category", task.category)  // ДОБАВЬТЕ ЭТУ СТРОКУ
            putExtra("task_due_date", task.dueDate.time)
            putExtra("reminder_minutes", task.reminderMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime.time, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(reminderTime.time, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.time, pendingIntent)
            }

            android.util.Log.d("NotificationScheduler", "Scheduled reminder for task ${task.name} at ${java.util.Date(reminderTime.time)}")
            return true
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationScheduler", "Security exception: ${e.message}")
            return false
        }
    }
}