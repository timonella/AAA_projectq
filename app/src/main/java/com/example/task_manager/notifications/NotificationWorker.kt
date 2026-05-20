package com.example.task_manager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.task_manager.R

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val taskName = inputData.getString("task_name") ?: return Result.failure()
            val taskId = inputData.getLong("task_id", -1)

            if (taskId == -1L) return Result.failure()

            showNotification(taskName, taskId.toInt())
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(taskName: String, taskId: Int) {
        val channelId = "task_reminders"
        val notificationId = taskId

        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Напоминание о задаче")
            .setContentText(taskName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        val channelId = "task_reminders"
        val channelName = "Напоминания о задачах"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(channelId, channelName, importance)
        channel.description = "Канал для напоминаний о предстоящих задачах"

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

