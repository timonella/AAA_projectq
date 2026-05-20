package com.example.task_manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class activity_today_task : AppCompatActivity() {

    private lateinit var dateTextView: TextView
    private lateinit var tasksCountTextView: TextView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var emptyTasksTextView: TextView
    private lateinit var btnAddTask: Button
    private lateinit var btnShowAllTasks: Button
    private lateinit var btnBackToMain: Button
    private lateinit var btnDebug: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var taskAdapter: TaskAdapter
    private var tasksList = mutableListOf<Task>()
    private var allTasksList = mutableListOf<Task>()
    private var completedTasksSet = mutableSetOf<String>()
    private var showOnlyToday = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_task)

        initViews()

        sharedPreferences = getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)

        setupRecyclerView()

        setupButtons()

        loadTodayTasks()
    }

    private fun initViews() {
        dateTextView = findViewById(R.id.dateTextView)
        tasksCountTextView = findViewById(R.id.tasksCountTextView)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        emptyTasksTextView = findViewById(R.id.emptyTasksTextView)
        btnAddTask = findViewById(R.id.btnGoToCreateTask)
        btnShowAllTasks = findViewById(R.id.btnShowAllTasks)
        btnBackToMain = findViewById(R.id.btnBackToMain)
        btnDebug = findViewById(R.id.btnDebug)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(tasksList) { task, position ->
            updateTaskStatus(task, position)
        }

        tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@activity_today_task)
            adapter = taskAdapter
            setHasFixedSize(true)
        }

        println("=== RECYCLERVIEW НАСТРОЕН ===")
        println("LayoutManager: ${tasksRecyclerView.layoutManager}")
        println("Adapter: ${tasksRecyclerView.adapter}")
    }

    private fun setupButtons() {
        btnAddTask.setOnClickListener {
            val intent = Intent(this, create_task::class.java)
            startActivity(intent)
        }

        btnShowAllTasks.setOnClickListener {
            showOnlyToday = !showOnlyToday
            if (showOnlyToday) {
                btnShowAllTasks.text = "📋 Показать все задачи"
                loadTodayTasks()
                Toast.makeText(this, "Показываю задачи на сегодня", Toast.LENGTH_SHORT).show()
            } else {
                btnShowAllTasks.text = "📅 Показать задачи на сегодня"
                loadAllTasksForDisplay()
                Toast.makeText(this, "Показываю все задачи", Toast.LENGTH_SHORT).show()
            }
        }

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        btnDebug.setOnClickListener {
            showDebugInfo()
        }
    }

    override fun onResume() {
        super.onResume()
        if (showOnlyToday) {
            loadTodayTasks()
        } else {
            loadAllTasksForDisplay()
        }
    }

    private fun loadTodayTasks() {
        println("\n=== ЗАГРУЗКА ЗАДАЧ НА СЕГОДНЯ ===")

        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val todayDate = dateFormat.format(today.time)

        println("Сегодняшняя дата: $todayDate")

        val dayOfWeek = getDayOfWeek(today)
        dateTextView.text = "📅 $dayOfWeek, $todayDate"

        completedTasksSet = sharedPreferences.getStringSet("completed_tasks", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        println("Выполненных задач в кэше: ${completedTasksSet.size}")

        allTasksList = loadAllTasksFromStorage()
        println("Всего задач в хранилище: ${allTasksList.size}")

        tasksList.clear()
        for (task in allTasksList) {
            println("Проверка: '${task.name}' - дата: '${task.date}' vs '${todayDate}'")
            if (task.date == todayDate) {
                tasksList.add(task)
                println("  ✅ ДОБАВЛЕНА: ${task.name}")
            }
        }

        println("Итого задач на сегодня: ${tasksList.size}")

        updateUI()
    }

    private fun loadAllTasksForDisplay() {
        println("\n=== ЗАГРУЗКА ВСЕХ ЗАДАЧ ===")

        allTasksList = loadAllTasksFromStorage()
        tasksList.clear()
        tasksList.addAll(allTasksList)

        dateTextView.text = "📋 Все задачи"

        println("Всего задач: ${tasksList.size}")

        updateUI()
    }

    private fun loadAllTasksFromStorage(): MutableList<Task> {
        val tasksSet = sharedPreferences.getStringSet("tasks_list", mutableSetOf()) ?: return mutableListOf()
        val tasks = mutableListOf<Task>()

        println("\n--- ЗАГРУЗКА ИЗ ХРАНИЛИЩА ---")
        println("Строк в хранилище: ${tasksSet.size}")

        for (taskString in tasksSet) {
            println("Строка: $taskString")
            val parts = taskString.split("|")

            if (parts.size >= 5) {
                val taskDate = parts[0]
                val taskName = parts[1]
                val taskDescription = parts[2]
                val taskCategory = parts[3]
                val taskReminder = parts[4]

                val taskKey = "$taskDate|$taskName"
                val isCompleted = completedTasksSet.contains(taskKey)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val dueDate = try {
                    dateFormat.parse(taskDate) ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                val task = Task(
                    name = taskName,
                    description = taskDescription,
                    category = taskCategory,
                    dueDate = dueDate,
                    reminderMinutesBefore = taskReminder.toIntOrNull() ?: 0,
                    isCompleted = isCompleted
                )
                tasks.add(task)
                println("  ✅ Загружена: $taskName ($taskDate) - Выполнена: $isCompleted")
            } else {
                println("  ❌ ОШИБКА: неверный формат, частей: ${parts.size}")
            }
        }

        println("--- ИТОГО ЗАГРУЖЕНО: ${tasks.size} ---\n")
        return tasks.sortedBy { it.date }.reversed().toMutableList()
    }

    private fun updateUI() {
        println("\n=== ОБНОВЛЕНИЕ UI ===")
        println("Задач в tasksList: ${tasksList.size}")
        println("Режим (только сегодня): $showOnlyToday")

        val completedCount = tasksList.count { it.isCompleted }
        tasksCountTextView.text = "📊 Всего: ${tasksList.size} | ✅ Выполнено: $completedCount"

        if (tasksList.isEmpty()) {
            println("Список пуст, показываем empty view")
            tasksRecyclerView.visibility = android.view.View.GONE
            emptyTasksTextView.visibility = android.view.View.VISIBLE
            if (showOnlyToday) {
                emptyTasksTextView.text = "📭 Нет задач на сегодня\n\nНажмите ➕ чтобы добавить задачу"
            } else {
                emptyTasksTextView.text = "📭 Нет ни одной задачи\n\nНажмите ➕ чтобы добавить задачу"
            }
        } else {
            println("Список не пуст, показываем RecyclerView с ${tasksList.size} задачами")
            tasksRecyclerView.visibility = android.view.View.VISIBLE
            emptyTasksTextView.visibility = android.view.View.GONE

            updateAdapter()
        }

        debugRecyclerView()
    }

    private fun updateAdapter() {
        println("Обновление адаптера с ${tasksList.size} задачами")

        tasksList.forEachIndexed { index, task ->
            println("  $index. ${task.name} - ${task.date} - Выполнена: ${task.isCompleted}")
        }

        taskAdapter.updateTasks(tasksList)

        println("После updateTasks, в адаптере задач: ${taskAdapter.itemCount}")
    }

    private fun debugRecyclerView() {
        println("\n--- ОТЛАДКА RECYCLERVIEW ---")
        println("RecyclerView visibility: ${tasksRecyclerView.visibility}")
        println("RecyclerView adapter: ${tasksRecyclerView.adapter}")
        println("RecyclerView layoutManager: ${tasksRecyclerView.layoutManager}")
        println("Adapter item count: ${taskAdapter.itemCount}")
        println("RecyclerView is attached: ${tasksRecyclerView.isAttachedToWindow}")
        println("--- КОНЕЦ ОТЛАДКИ ---\n")
    }

    private fun updateTaskStatus(task: Task, position: Int) {
        println("\n=== ОБНОВЛЕНИЕ СТАТУСА ===")
        println("Задача: ${task.name}, Новый статус: ${task.isCompleted}")

        tasksList[position] = task

        val taskKey = "${task.date}|${task.name}"

        if (task.isCompleted) {
            completedTasksSet.add(taskKey)
            Toast.makeText(this, "✅ Выполнено: ${task.name}", Toast.LENGTH_SHORT).show()
        } else {
            completedTasksSet.remove(taskKey)
            Toast.makeText(this, "🔄 Отменено: ${task.name}", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences.edit().putStringSet("completed_tasks", completedTasksSet).apply()

        val completedCount = tasksList.count { it.isCompleted }
        tasksCountTextView.text = "📊 Всего: ${tasksList.size} | ✅ Выполнено: $completedCount"

        taskAdapter.notifyItemChanged(position)

        println("Статус обновлен, completed_set теперь: ${completedTasksSet.size} элементов")
    }

    private fun getDayOfWeek(calendar: Calendar): String {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Понедельник"
            Calendar.TUESDAY -> "Вторник"
            Calendar.WEDNESDAY -> "Среда"
            Calendar.THURSDAY -> "Четверг"
            Calendar.FRIDAY -> "Пятница"
            Calendar.SATURDAY -> "Суббота"
            Calendar.SUNDAY -> "Воскресенье"
            else -> ""
        }
    }

    private fun showDebugInfo() {
        val tasksSet = sharedPreferences.getStringSet("tasks_list", mutableSetOf())
        val completedSet = sharedPreferences.getStringSet("completed_tasks", mutableSetOf())

        val message = StringBuilder()
        message.append("=== ОТЛАДКА ===\n")
        message.append("Задач в хранилище: ${tasksSet?.size ?: 0}\n")
        message.append("Выполненных: ${completedSet?.size ?: 0}\n")
        message.append("В списке сейчас: ${tasksList.size}\n")
        message.append("Режим: ${if (showOnlyToday) "Сегодня" else "Все"}\n\n")

        if (tasksSet.isNullOrEmpty()) {
            message.append("❌ НЕТ СОХРАНЕННЫХ ЗАДАЧ!\n")
            message.append("Сначала создайте задачу")
        } else {
            message.append("Последние 3 задачи:\n")
            tasksSet.take(3).forEachIndexed { index, task ->
                message.append("${index + 1}. $task\n")
            }
            if (tasksSet.size > 3) {
                message.append("... и еще ${tasksSet.size - 3}\n")
            }
        }

        message.append("\nАдаптер: ${taskAdapter.itemCount} задач")

        Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show()

        println(message.toString())

        if (showOnlyToday) {
            loadTodayTasks()
        } else {
            loadAllTasksForDisplay()
        }
    }
}