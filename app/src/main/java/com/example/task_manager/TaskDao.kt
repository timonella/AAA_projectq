package com.example.task_manager

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY dueDate ASC, priority DESC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isArchived = 1 ORDER BY completedAt DESC")
    fun getArchivedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isArchived = 0 ORDER BY dueTime ASC")
    fun getTasksForDate(date: Date): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE category = :category AND isArchived = 0")
    fun getTasksByCategory(category: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE priority = :priority AND isArchived = 0")
    fun getTasksByPriority(priority: Priority): Flow<List<Task>>

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean, completedAt: Date?)

    @Query("UPDATE tasks SET isArchived = 1 WHERE id = :taskId")
    suspend fun archiveTask(taskId: Long)

    @Query("UPDATE tasks SET isArchived = 0 WHERE id = :taskId")
    suspend fun restoreTask(taskId: Long)

    // Statistics queries
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND isArchived = 0")
    fun getCompletedTasksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND isArchived = 0")
    fun getPendingTasksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE category = :category")
    fun getTasksCountByCategory(category: String): Flow<Int>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt >= :startDate AND completedAt <= :endDate")
    fun getCompletedTasksInPeriod(startDate: Date, endDate: Date): Flow<List<Task>>

    // В файле TaskDao.kt добавьте этот метод:
    @Query("SELECT * FROM tasks WHERE isArchived = 0")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date(dueDate / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch') AND isArchived = 0")
    fun getTasksForDateSync(date: Date): List<Task>

    // Добавьте эти методы в существующий TaskDao.kt

    @Query("SELECT * FROM tasks WHERE repeatParentId = :parentId AND isArchived = 0 ORDER BY dueDate ASC")
    suspend fun getActiveRepeatInstances(parentId: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE repeatParentId = :parentId")
    suspend fun getAllRepeatInstances(parentId: Long): List<Task>

    @Query("UPDATE tasks SET isArchived = 1, isCompleted = true, completedAt = :completedAt WHERE repeatParentId = :parentId")
    suspend fun archiveAllRepeatInstances(parentId: Long, completedAt: Date)

    @Query("DELETE FROM tasks WHERE repeatParentId = :parentId")
    suspend fun deleteAllRepeatInstances(parentId: Long)

    @Query("SELECT * FROM tasks WHERE repeatParentId IS NULL AND repeatType != :noneType ORDER BY dueDate ASC")
    fun getRepeatParentTasks(noneType: RepeatType): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate <= :currentDate AND repeatParentId IS NOT NULL AND isCompleted = 0 AND isArchived = 0")
    suspend fun getOverdueRepeatInstances(currentDate: Date): List<Task>

}
