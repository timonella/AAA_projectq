package com.example.task_manager

import android.app.Application
import androidx.room.Room

class TaskApplication : Application() {
    lateinit var taskRepository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val database = TaskDatabase.getDatabase(this)
        val taskDao = database.taskDao()
        taskRepository = TaskRepository(taskDao)
    }
}