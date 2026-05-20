package com.example.task_manager

import kotlinx.coroutines.flow.Flow
import java.util.Date

class TaskRepository(private val taskDao: TaskDao) {

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(taskId: Long) = taskDao.deleteTask(taskId)

    fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()

    fun getArchivedTasks(): Flow<List<Task>> = taskDao.getArchivedTasks()

    fun getTasksForDate(date: java.util.Date): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun getTasksByCategory(category: String): Flow<List<Task>> = taskDao.getTasksByCategory(category)

    fun getTasksByPriority(priority: Priority): Flow<List<Task>> = taskDao.getTasksByPriority(priority)

    suspend fun archiveTask(taskId: Long) = taskDao.archiveTask(taskId)

    suspend fun restoreTask(taskId: Long) = taskDao.restoreTask(taskId)

    fun getPendingTasksCount(): Flow<Int> = taskDao.getPendingTasksCount()

    fun getCompletedTasksCount(): Flow<Int> = taskDao.getCompletedTasksCount()

    fun getTasksCountByCategory(category: String): Flow<Int> = taskDao.getTasksCountByCategory(category)

    fun getCompletedTasksInPeriod(startDate: java.util.Date, endDate: java.util.Date): Flow<List<Task>> =
        taskDao.getCompletedTasksInPeriod(startDate, endDate)

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    // Добавьте эти методы в TaskRepository.kt

    suspend fun getActiveRepeatInstances(parentId: Long): List<Task> =
        taskDao.getActiveRepeatInstances(parentId)

    suspend fun getAllRepeatInstances(parentId: Long): List<Task> =
        taskDao.getAllRepeatInstances(parentId)

    suspend fun archiveAllRepeatInstances(parentId: Long, completedAt: Date) =
        taskDao.archiveAllRepeatInstances(parentId, completedAt)

    suspend fun deleteAllRepeatInstances(parentId: Long) =
        taskDao.deleteAllRepeatInstances(parentId)

    fun getRepeatParentTasks(): Flow<List<Task>> =
        taskDao.getRepeatParentTasks(RepeatType.NONE)

    suspend fun getOverdueRepeatInstances(currentDate: Date): List<Task> =
        taskDao.getOverdueRepeatInstances(currentDate)
}