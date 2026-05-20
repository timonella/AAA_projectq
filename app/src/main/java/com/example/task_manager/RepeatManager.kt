package com.example.task_manager

import java.util.*
import java.util.Calendar

class RepeatManager {

    /**
     * Создает следующий экземпляр повторяющейся задачи
     * @return новую задачу или null если достигнута дата окончания
     */
    fun createNextInstance(parentTask: Task, currentInstanceDate: Date): Task? {
        // Проверяем, есть ли повторение
        if (parentTask.repeatType == RepeatType.NONE) return null

        // Проверяем дату окончания
        parentTask.repeatEndDate?.let { endDate ->
            if (currentInstanceDate.after(endDate)) return null
        }

        val calendar = Calendar.getInstance().apply {
            time = currentInstanceDate
        }

        // Вычисляем следующую дату
        when (parentTask.repeatType) {
            RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RepeatType.NONE -> return null
        }

        val nextDate = calendar.time

        // Проверяем дату окончания для следующего экземпляра
        parentTask.repeatEndDate?.let { endDate ->
            if (nextDate.after(endDate)) return null
        }

        // Создаем новый экземпляр
        return parentTask.copy(
            id = 0,  // Новая запись в БД
            dueDate = nextDate,
            isCompleted = false,
            isArchived = false,
            completedAt = null,
            createdAt = Date(),
            repeatParentId = parentTask.repeatParentId ?: parentTask.id,
            repeatInstanceId = UUID.randomUUID().mostSignificantBits
        )
    }

    /**
     * Проверяет, нужно ли создать новый экземпляр задачи
     */
    fun shouldCreateNextInstance(task: Task): Boolean {
        return task.repeatType != RepeatType.NONE &&
                !task.isArchived &&
                !task.isCompleted
    }

    /**
     * Получает дату следующего повторения для отображения в UI
     */
    fun getNextRepeatDate(task: Task, currentDate: Date): Date? {
        val manager = RepeatManager()
        return manager.createNextInstance(task, currentDate)?.dueDate
    }
}