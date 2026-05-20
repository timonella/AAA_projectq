package com.example.task_manager.notifications

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.task_manager.Task
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (task.reminderMinutesBefore == 0) return

        val calendar = Calendar.getInstance()
        calendar.time = task.dueDate
        calendar.add(Calendar.MINUTE, -task.reminderMinutesBefore)

        val now = Calendar.getInstance()
        val delayMillis = calendar.timeInMillis - now.timeInMillis

        if (delayMillis <= 0) return

        val data = workDataOf(
            "task_name" to task.name,
            "task_id" to task.id
        )

        val notificationRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_reminder_${task.id}",
            androidx.work.ExistingWorkPolicy.REPLACE,
            notificationRequest
        )
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("task_reminder_$taskId")
    }
}
