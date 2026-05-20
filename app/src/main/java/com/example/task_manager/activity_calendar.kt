package com.example.task_manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class activity_calendar : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView
    private lateinit var btnGoToTodayTask: Button
    private lateinit var btnGoToCreateTask: Button
    private lateinit var btnBackToMain: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var tasksAdapter: CalendarTasksAdapter

    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendarView = findViewById(R.id.calendarView)
        selectedDateText = findViewById(R.id.selectedDateText)
        btnGoToTodayTask = findViewById(R.id.btnGoToTodayTask)
        btnGoToCreateTask = findViewById(R.id.btnGoToCreateTask)
        btnBackToMain = findViewById(R.id.btnBackToMain)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)

        sharedPreferences = getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        taskDatabase = TaskDatabase.getDatabase(this)

        setupTasksRecyclerView()
        loadSavedDate()
        setupCalendarView()
        setupButtons()
    }

    private fun setupTasksRecyclerView() {
        tasksAdapter = CalendarTasksAdapter { task: Task ->
            showTaskDetailsDialog(task)
        }
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter
    }

    private fun showTaskDetailsDialog(task: Task) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(task.name)
            .setMessage("""
                Описание: ${task.description.ifEmpty { "Нет описания" }}
                Категория: ${task.category}
                Приоритет: ${task.priority}
                Время: ${task.dueTime ?: "Не указано"}
                Статус: ${if (task.isCompleted) "Выполнено" else "Не выполнено"}
                Повторение: ${task.repeatType}
            """.trimIndent())
            .setPositiveButton("Закрыть") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Изменить статус") { _, _ ->
                lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    taskDatabase.taskDao().updateTask(updatedTask)
                    loadTasksForSelectedDate()
                    Toast.makeText(this@activity_calendar, "Статус обновлен", Toast.LENGTH_SHORT).show()
                }
            }
            .create()
        dialog.show()
    }

    private fun setupCalendarView() {
        calendarView.date = selectedDate.timeInMillis

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            saveDate(selectedDate)
            updateDateText(selectedDate)
            loadTasksForSelectedDate()

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            Toast.makeText(this, "Выбрана дата: ${dateFormat.format(selectedDate.time)}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        btnGoToTodayTask.setOnClickListener {
            val intent = Intent(this, activity_today_task::class.java)
            startActivity(intent)
        }

        btnGoToCreateTask.setOnClickListener {
            val intent = Intent(this, create_task::class.java)
            startActivity(intent)
        }

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadTasksForSelectedDate() {
        lifecycleScope.launch {
            try {
                val tasks = taskDatabase.taskDao().getTasksForDateSync(selectedDate.time)
                tasksAdapter.updateTasks(tasks)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateCalendarMarkers() {
        calendarView.date = selectedDate.timeInMillis
        // CalendarView не поддерживает прямую перерисовку маркеров,
        // но установка той же даты заставит его обновиться
    }

    private fun saveDate(date: Calendar) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dateString = dateFormat.format(date.time)

        with(sharedPreferences.edit()) {
            putLong("selected_date", date.timeInMillis)
            putString("selected_date_string", dateString)
            apply()
        }
    }

    private fun loadSavedDate() {
        val savedDateMillis = sharedPreferences.getLong("selected_date", System.currentTimeMillis())
        selectedDate.timeInMillis = savedDateMillis
        updateDateText(selectedDate)
        loadTasksForSelectedDate()
    }

    private fun updateDateText(calendar: Calendar) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale("ru"))

        val date = dateFormat.format(calendar.time)
        val dayOfWeek = dayFormat.format(calendar.time)

        selectedDateText.text = "$dayOfWeek, $date"
    }

    fun getSelectedDate(): Calendar {
        return selectedDate
    }

    fun getSelectedDateString(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(selectedDate.time)
    }

    override fun onResume() {
        super.onResume()
        loadTasksForSelectedDate()
        // Принудительно обновляем календарь
        calendarView.date = calendarView.date
    }
}