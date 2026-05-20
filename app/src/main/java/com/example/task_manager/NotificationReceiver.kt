package com.example.task_manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("NotificationReceiver", "Received notification: ${intent.action}")

        val action = intent.action

        when (action) {
            "REMINDER" -> {
                val taskId = intent.getLongExtra("task_id", -1)
                val taskName = intent.getStringExtra("task_name") ?: ""
                val taskDescription = intent.getStringExtra("task_description") ?: ""
                val reminderMinutes = intent.getIntExtra("reminder_minutes", 0)
                val taskCategory = intent.getStringExtra("task_category") ?: "Без категории"

                android.util.Log.d("NotificationReceiver", "Showing reminder for task: $taskName")

                // Создаём временный объект Task для уведомления
                val task = Task(
                    id = taskId,
                    name = taskName,
                    description = taskDescription,
                    category = taskCategory,
                    priority = Priority.MEDIUM,
                    dueDate = java.util.Date(intent.getLongExtra("task_due_date", System.currentTimeMillis())),
                    reminderMinutes = reminderMinutes
                )

                val notificationHelper = NotificationHelper(context)
                notificationHelper.showReminderNotification(task, reminderMinutes)
            }
        }
    }
}