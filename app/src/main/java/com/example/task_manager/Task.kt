package com.example.task_manager

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Date,
    val dueTime: String? = null,
    val reminderMinutesBefore: Int = 0,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Date = Date(),
    val completedAt: Date? = null,
    val reminderMinutes: Int = 0,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatParentId: Long? = null,  // ID родительской задачи (для дочерних копий)
    val repeatEndDate: Date? = null,    // Дата окончания повторений (опционально)
    val repeatInstanceId: Long? = null,

) {
    val date: String
        get() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dueDate)
}

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class RepeatType  {
    NONE, DAILY, WEEKLY, MONTHLY
}
