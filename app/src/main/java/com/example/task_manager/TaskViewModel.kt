package com.example.task_manager

import android.content.Context
import java.util.Calendar
import kotlinx.coroutines.flow.flowOn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var notificationHelper: NotificationHelper

    val repeatParentTasks = repository.getRepeatParentTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    // Активные задачи (не архивированные)
    val tasks: StateFlow<List<Task>> = repository.getActiveTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Архивные задачи
    val archivedTasks: StateFlow<List<Task>> = repository.getArchivedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTasks: StateFlow<List<Task>> = repository.getActiveTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val repeatManager = RepeatManager()


    // Счётчики
    private val _pendingTasksCount = MutableStateFlow(0)
    val pendingTasksCount: StateFlow<Int> = _pendingTasksCount.asStateFlow()

    private val _completedTasksCount = MutableStateFlow(0)
    val completedTasksCount: StateFlow<Int> = _completedTasksCount.asStateFlow()

    private val _archivedTasksCount = MutableStateFlow(0)
    val archivedTasksCount: StateFlow<Int> = _archivedTasksCount.asStateFlow()

    private val _todayTasksCount = MutableStateFlow(0)
    val todayTasksCount: StateFlow<Int> = _todayTasksCount.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getActiveTasks().collect { taskList ->
                updateCounters(taskList)
            }
        }
        viewModelScope.launch {
            repository.getArchivedTasks().collect { archivedList ->
                _archivedTasksCount.value = archivedList.size
            }
        }
    }

    private fun updateCounters(activeTasks: List<Task>) {
        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        _pendingTasksCount.value = activeTasks.count { !it.isCompleted }
        _completedTasksCount.value = activeTasks.count { it.isCompleted }
        _todayTasksCount.value = activeTasks.count { task ->
            val taskDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(task.dueDate)
            taskDate == currentDate && !task.isCompleted
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch { repository.insertTask(task) }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun initNotifications(context: Context) {
        notificationScheduler = NotificationScheduler(context)
        notificationHelper = NotificationHelper(context)
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            // Если нужно также отменить уведомления
            notificationScheduler?.cancelNotifications(taskId)
        }
    }
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (!task.isCompleted) {
                if (task.repeatType != RepeatType.NONE) {
                    // Для повторяющихся задач используем специальный метод
                    completeRepeatTask(task)
                } else {
                    // Для обычных задач - архивируем
                    val updatedTask = task.copy(
                        isCompleted = true,
                        isArchived = true,
                        completedAt = Date()
                    )
                    repository.updateTask(updatedTask)
                }
            } else {
                // Если задача уже была выполнена - просто обновляем
                val updatedTask = task.copy(isCompleted = !task.isCompleted)
                repository.updateTask(updatedTask)
            }
        }
    }

    fun archiveTask(taskId: Long) {
        viewModelScope.launch { repository.archiveTask(taskId) }
    }

    fun restoreTask(taskId: Long) {
        viewModelScope.launch {
            // Сначала получаем задачу, чтобы сохранить её данные
            val task = repository.getTaskById(taskId)
            task?.let {
                val restoredTask = it.copy(
                    isArchived = false,
                    isCompleted = false,  // Сбрасываем статус выполнения
                    completedAt = null     // Очищаем дату выполнения
                    // dueDate и dueTime остаются без изменений
                )
                repository.updateTask(restoredTask)
            }
        }
    }

    fun deleteTaskAndRefresh(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            // Принудительно обновляем потоки для дат
            tasks.value // триггерим обновление
            archivedTasks.value
        }
    }

    // Поток задач для конкретной даты – возвращаем только НЕАРХИВИРОВАННЫЕ задачи
    fun getTasksForDate(date: Date): Flow<List<Task>> {
        return repository.getTasksForDate(date).map { taskList ->
            taskList.filter { !it.isArchived }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    fun getTasksByCategory(category: String): Flow<List<Task>> {
        return repository.getTasksByCategory(category).map { taskList ->
            taskList.filter { !it.isArchived }
        }
    }

    fun getTasksByPriority(priority: Priority): Flow<List<Task>> {
        return repository.getTasksByPriority(priority).map { taskList ->
            taskList.filter { !it.isArchived }
        }
    }

    fun getPendingTasksCount(): Flow<Int> = repository.getPendingTasksCount()
    fun getCompletedTasksCount(): Flow<Int> = repository.getCompletedTasksCount()
    fun getTasksCountByCategory(category: String): Flow<Int> = repository.getTasksCountByCategory(category)
    fun getCompletedTasksInPeriod(startDate: Date, endDate: Date): Flow<List<Task>> =
        repository.getCompletedTasksInPeriod(startDate, endDate)


    // Добавьте в TaskViewModel.kt
    val activeDisplayTasks: StateFlow<List<Task>> = combine(
        tasks,  // реальные задачи из БД
        repository.getRepeatParentTasks()
    ) { allTasks, parentTasks ->
        val result = allTasks.toMutableList()

        // Для каждой родительской повторяющейся задачи
        parentTasks.forEach { parentTask ->
            // Получаем все экземпляры этой задачи
            val instances = allTasks.filter { it.repeatParentId == parentTask.id }

            // Генерируем недостающие экземпляры
            if (parentTask.repeatType != RepeatType.NONE && instances.isEmpty()) {
                // Если нет ни одного экземпляра, генерируем первый
                val firstInstance = generateNextInstance(parentTask, parentTask.dueDate)
                firstInstance?.let { result.add(it) }
            }
        }

        result.filter { !it.isArchived && !it.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Вспомогательная функция для генерации следующего экземпляра
    private fun generateNextInstance(parentTask: Task, fromDate: Date): Task? {
        val repeatManager = RepeatManager()
        return repeatManager.createNextInstance(parentTask, fromDate)
    }

    private suspend fun archiveAllRepeatInstances(parentId: Long) {
        val instances = repository.getAllRepeatInstances(parentId)
        instances.forEach { instance ->
            val archivedInstance = instance.copy(
                isArchived = true,
                isCompleted = true,
                completedAt = Date()
            )
            repository.updateTask(archivedInstance)
        }
    }


    private fun shouldTaskAppearOnDate(parentTask: Task, date: Date): Boolean {
        if (parentTask.repeatType == RepeatType.NONE) return false

        // Проверяем, что дата не раньше даты создания
        if (date.before(parentTask.dueDate)) return false

        // Проверяем дату окончания
        parentTask.repeatEndDate?.let { endDate ->
            if (date.after(endDate)) return false
        }

        val parentCalendar = Calendar.getInstance().apply { time = parentTask.dueDate }
        val targetCalendar = Calendar.getInstance().apply { time = date }

        return when (parentTask.repeatType) {
            RepeatType.DAILY -> true
            RepeatType.WEEKLY -> {
                parentCalendar.get(Calendar.WEEK_OF_YEAR) == targetCalendar.get(Calendar.WEEK_OF_YEAR)
            }
            RepeatType.MONTHLY -> {
                parentCalendar.get(Calendar.DAY_OF_MONTH) == targetCalendar.get(Calendar.DAY_OF_MONTH)
            }
            RepeatType.NONE -> false
        }
    }

    // Вспомогательная функция для сравнения дат
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // Выполнение задачи (не трогаем существующую логику)
    fun completeTaskAndCreateNext(task: Task) {
        viewModelScope.launch {
            // Отмечаем текущую задачу как выполненную
            val updatedTask = task.copy(
                isCompleted = true,
                isArchived = true,
                completedAt = Date()
            )
            repository.updateTask(updatedTask)

            // Если задача повторяющаяся, создаем следующую
            if (task.repeatType != RepeatType.NONE) {
                createNextTaskCopy(task, task.id)
            }
        }
    }

    fun addTaskWithRepeat(task: Task) {
        viewModelScope.launch {
            android.util.Log.d("TaskViewModel", "Creating task: ${task.name}, reminder: ${task.reminderMinutes}")

            val taskId = repository.insertTask(task)

            if (::notificationScheduler.isInitialized) {
                val scheduled = notificationScheduler.scheduleReminder(task)
                if (!scheduled) {
                    android.util.Log.w("TaskViewModel", "Failed to schedule reminder for: ${task.name}")
                }
            }

            if (task.repeatType != RepeatType.NONE && task.repeatParentId != null) {
                createNextTaskCopy(task, taskId)
            }
        }
    }

    private suspend fun createNextTaskCopy(originalTask: Task, originalId: Long) {
        val calendar = Calendar.getInstance()
        calendar.time = originalTask.dueDate

        when (originalTask.repeatType) {
            RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RepeatType.NONE -> return
        }

        val nextDate = calendar.time

        // Проверяем дату окончания
        originalTask.repeatEndDate?.let { endDate ->
            if (nextDate.after(endDate)) return
        }

        val nextTask = originalTask.copy(
            id = 0,
            dueDate = nextDate,
            isCompleted = false,
            isArchived = false,
            completedAt = null,
            createdAt = Date()
        )

        repository.insertTask(nextTask)
    }

    // В TaskViewModel.kt метод completeRepeatTask
    fun completeRepeatTask(task: Task) {
        viewModelScope.launch {
            if (::notificationScheduler.isInitialized) {
                notificationScheduler.cancelNotifications(task.id)  // Теперь этот метод существует
                if (::notificationHelper.isInitialized) {
                    notificationHelper.showTestNotification("✅ Задача выполнена", task.name)
                }
            }

            val updatedTask = task.copy(
                isCompleted = true,
                isArchived = true,
                completedAt = Date()
            )
            repository.updateTask(updatedTask)

            if (task.repeatType != RepeatType.NONE) {
                createNextTaskCopy(task, task.id)
            }
        }
    }

    // Создание всех пропущенных повторений при запуске
    fun createMissedRepeats() {
        viewModelScope.launch {
            val allTasks = repository.getActiveTasks().first()
            val currentDate = Date()

            // Находим все активные повторяющиеся задачи
            val repeatTasks = allTasks.filter {
                it.repeatType != RepeatType.NONE &&
                        !it.isCompleted &&
                        !it.isArchived
            }

            for (task in repeatTasks) {
                // Создаем копии для всех пропущенных дат
                generateMissingCopies(task)
            }
        }
    }

    // Генерация всех пропущенных копий
    private suspend fun generateMissingCopies(task: Task) {
        val currentDate = Date()
        var nextDate = task.dueDate

        // Если дата задачи уже в прошлом, создаем копии до текущей даты
        while (nextDate.before(currentDate)) {
            val calendar = Calendar.getInstance()
            calendar.time = nextDate

            when (task.repeatType) {
                RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RepeatType.NONE -> return
            }

            nextDate = calendar.time

            // Проверяем дату окончания
            task.repeatEndDate?.let { endDate ->
                if (nextDate.after(endDate)) break
            }

            // Создаем копию
            val newTask = task.copy(
                id = 0,
                dueDate = nextDate,
                isCompleted = false,
                isArchived = false,
                completedAt = null,
                createdAt = Date()
            )
            repository.insertTask(newTask)
            android.util.Log.d("REPEAT", "Missed copy created for: $nextDate")
        }
    }
}