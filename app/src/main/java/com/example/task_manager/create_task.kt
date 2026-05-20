package com.example.task_manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog


class create_task : AppCompatActivity() {

    private lateinit var taskNameEditText: EditText
    private lateinit var descriptionEditText: EditText

    private lateinit var categorySpinner: Spinner

    private lateinit var reminderSpinner: Spinner

    private lateinit var addTaskButton: Button

    private lateinit var selectDateButton: Button

    private lateinit var btnGoToCalendar: Button

    private lateinit var btnBackToMain: Button

    private lateinit var sharedPreferences: SharedPreferences



    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)


        taskNameEditText = findViewById(R.id.taskNameEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        reminderSpinner = findViewById(R.id.napominaniya)
        addTaskButton = findViewById(R.id.addTaskButton)
        selectDateButton = findViewById(R.id.selectDateButton)
        btnGoToCalendar = findViewById(R.id.btnGoToCalendar)
        btnBackToMain = findViewById(R.id.btnBackToMain)

        sharedPreferences = getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)

        setupCategorySpinner()
        setupReminderSpinner()
        updateDateButtonText()

        selectDateButton.setOnClickListener {
            showDatePickerDialog()
        }

        addTaskButton.setOnClickListener {
            saveTask()
        }

        btnGoToCalendar.setOnClickListener {
            val intent = Intent(this, activity_calendar::class.java)
            startActivity(intent)
        }

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth -> selectedDate.set(year, month, dayOfMonth)
                updateDateButtonText()
            }

            ,selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateButtonText() {

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        selectDateButton.text = "📅 ${dateFormat.format(selectedDate.time)}"

    }


    private fun setupCategorySpinner() {
        val categories = arrayOf("Работа", "Личное", "Учеба", "Дом", "Здоровье", "Другое")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun setupReminderSpinner() {
        val reminders = arrayOf("Без напоминания", "За 5 минут", "За 15 минут", "За 30 минут", "За 1 час", "За 1 день")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reminders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reminderSpinner.adapter = adapter

    }

    private fun saveTask() {
        val taskName = taskNameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val reminder = reminderSpinner.selectedItem.toString()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val taskDate = dateFormat.format(selectedDate.time)


        // ОТЛАДКУ НЕ ТРОГАТЬ Я СКАЗАЛ
        println("Сохранение задачи:")
        println("Выбранная дата: ${selectedDate.time}")
        println("Форматированная дата: $taskDate")

        if (taskName.isEmpty()) {

            Toast.makeText(this, "Введите название задачи", Toast.LENGTH_SHORT).show()
            return
        }

        saveTaskToStorage(taskName, description, category, reminder, taskDate)

        Toast.makeText(this, "✅ Задача добавлена на $taskDate", Toast.LENGTH_SHORT).show()

        taskNameEditText.text.clear()
        descriptionEditText.text.clear()

    }

    private fun saveTaskToStorage(name: String, desc: String, category: String, reminder: String, date: String) {
        val tasksSet = sharedPreferences.getStringSet("tasks_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val taskString = "$date|$name|$desc|$category|$reminder"
        tasksSet.add(taskString)
        sharedPreferences.edit().putStringSet("tasks_list", tasksSet).apply()

        val savedSet = sharedPreferences.getStringSet("tasks_list", mutableSetOf())
        println("=== ПРОВЕРКА СОХРАНЕНИЯ ===")
        println("Добавлена строка: $taskString")
        println("Всего задач после сохранения: ${savedSet?.size ?: 0}")
        savedSet?.forEach { println("  - $it") }
    }



}