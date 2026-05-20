package com.example.task_manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*

class activity_statistics : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var totalTasksTextView: TextView
    private lateinit var completedTasksTextView: TextView
    private lateinit var pendingTasksTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var categoriesStatsContainer: LinearLayout
    private lateinit var dailyStatsContainer: LinearLayout
    private lateinit var btnRefreshStats: Button
    private lateinit var btnBackToMain: Button

    private var allTasks = mutableListOf<Task>()
    private var completedTasksSet = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        totalTasksTextView = findViewById(R.id.totalTasksTextView)
        completedTasksTextView = findViewById(R.id.completedTasksTextView)
        pendingTasksTextView = findViewById(R.id.pendingTasksTextView)
        progressTextView = findViewById(R.id.progressTextView)
        progressBar = findViewById(R.id.progressBar)
        categoriesStatsContainer = findViewById(R.id.categoriesStatsContainer)
        dailyStatsContainer = findViewById(R.id.dailyStatsContainer)
        btnRefreshStats = findViewById(R.id.btnRefreshStats)
        btnBackToMain = findViewById(R.id.btnBackToMain)

        sharedPreferences = getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)

        loadStatistics()

        btnRefreshStats.setOnClickListener {
            loadStatistics()
        }

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    private fun loadStatistics() {
        allTasks = loadAllTasks()

        completedTasksSet = sharedPreferences.getStringSet("completed_tasks", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        updateGeneralStatistics()

        updateCategoriesStatistics()

        updateDailyStatistics()
    }

    private fun loadAllTasks(): MutableList<Task> {
        val tasksSet = sharedPreferences.getStringSet("tasks_list", mutableSetOf()) ?: return mutableListOf()
        val tasks = mutableListOf<Task>()

        for (taskString in tasksSet) {
            val parts = taskString.split("|")
            if (parts.size >= 5) {
                val taskDate = parts[0]
                val taskKey = "$taskDate|${parts[1]}"
                val isCompleted = completedTasksSet.contains(taskKey)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val dueDate = try {
                    dateFormat.parse(taskDate) ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                val task = Task(
                    name = parts[1],
                    description = parts[2],
                    category = parts[3],
                    dueDate = dueDate,
                    reminderMinutesBefore = parts[4].toIntOrNull() ?: 0,
                    isCompleted = isCompleted
                )
                tasks.add(task)
            }
        }

        return tasks
    }

    private fun updateGeneralStatistics() {
        val total = allTasks.size
        val completed = allTasks.count { it.isCompleted }
        val pending = total - completed
        val progress = if (total > 0) (completed * 100 / total) else 0

        totalTasksTextView.text = total.toString()
        completedTasksTextView.text = completed.toString()
        pendingTasksTextView.text = pending.toString()
        progressTextView.text = "$progress%"
        progressBar.progress = progress

        progressBar.animate()
    }

    private fun updateCategoriesStatistics() {
        categoriesStatsContainer.removeAllViews()

        val categoriesMap = mutableMapOf<String, MutableList<Task>>()

        for (task in allTasks) {
            if (!categoriesMap.containsKey(task.category)) {
                categoriesMap[task.category] = mutableListOf()
            }
            categoriesMap[task.category]?.add(task)
        }

        val sortedCategories = categoriesMap.keys.sorted()

        if (sortedCategories.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "Нет задач"
            emptyText.textSize = 14f
            emptyText.setTextColor(resources.getColor(android.R.color.darker_gray))
            categoriesStatsContainer.addView(emptyText)
            return
        }

        for (category in sortedCategories) {
            val tasks = categoriesMap[category] ?: continue
            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            val progress = if (total > 0) (completed * 100 / total) else 0

            val categoryCard = createCategoryCard(category, total, completed, progress)
            categoriesStatsContainer.addView(categoryCard)
        }
    }

    private fun createCategoryCard(category: String, total: Int, completed: Int, progress: Int): CardView {
        val cardView = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 16)
        cardView.layoutParams = layoutParams
        cardView.radius = 8f
        cardView.elevation = 4f

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(16, 16, 16, 16)

        val categoryName = TextView(this)
        categoryName.text = category
        categoryName.textSize = 16f
        categoryName.setTextColor(resources.getColor(android.R.color.black))

        val statsLayout = LinearLayout(this)
        statsLayout.orientation = LinearLayout.HORIZONTAL
        statsLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val totalText = TextView(this)
        totalText.text = "Всего: $total"
        totalText.textSize = 14f
        totalText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val completedText = TextView(this)
        completedText.text = "$completed"
        completedText.textSize = 14f
        completedText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        completedText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val progressText = TextView(this)
        progressText.text = "$progress%"
        progressText.textSize = 14f
        progressText.setTextColor(resources.getColor(android.R.color.holo_blue_dark))

        statsLayout.addView(totalText)
        statsLayout.addView(completedText)
        statsLayout.addView(progressText)

        // Прогресс бар категории
        val categoryProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        categoryProgressBar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            15
        )
        categoryProgressBar.progress = progress
        categoryProgressBar.progressTintList = when (category) {
            "Работа" -> resources.getColorStateList(android.R.color.holo_blue_dark)
            "Личное" -> resources.getColorStateList(android.R.color.holo_green_dark)
            "Учеба" -> resources.getColorStateList(android.R.color.holo_orange_dark)
            "Дом" -> resources.getColorStateList(android.R.color.holo_purple)
            else -> resources.getColorStateList(android.R.color.holo_red_dark)
        }

        linearLayout.addView(categoryName)
        linearLayout.addView(statsLayout)
        linearLayout.addView(categoryProgressBar)

        cardView.addView(linearLayout)
        return cardView
    }

    private fun updateDailyStatistics() {
        dailyStatsContainer.removeAllViews()

        val dailyMap = mutableMapOf<String, MutableList<Task>>()

        for (task in allTasks) {
            if (!dailyMap.containsKey(task.date)) {
                dailyMap[task.date] = mutableListOf()
            }
            dailyMap[task.date]?.add(task)
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val sortedDates = dailyMap.keys.sortedByDescending {
            try {
                dateFormat.parse(it)
            } catch (e: Exception) {
                Date()
            }
        }

        val last7Days = sortedDates.take(7)

        if (last7Days.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "Нет задач"
            emptyText.textSize = 14f
            emptyText.setTextColor(resources.getColor(android.R.color.darker_gray))
            dailyStatsContainer.addView(emptyText)
            return
        }

        for (date in last7Days) {
            val tasks = dailyMap[date] ?: continue
            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            val progress = if (total > 0) (completed * 100 / total) else 0

            val dayCard = createDayCard(date, total, completed, progress)
            dailyStatsContainer.addView(dayCard)
        }
    }

    private fun createDayCard(date: String, total: Int, completed: Int, progress: Int): CardView {
        val cardView = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 8)
        cardView.layoutParams = layoutParams
        cardView.radius = 8f
        cardView.elevation = 2f

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.setPadding(16, 12, 16, 12)

        val dateText = TextView(this)
        dateText.text = date
        dateText.textSize = 14f
        dateText.setTextColor(resources.getColor(android.R.color.black))
        dateText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )

        val statsText = TextView(this)
        statsText.text = "$completed/$total"
        statsText.textSize = 14f
        statsText.setTextColor(resources.getColor(android.R.color.darker_gray))
        statsText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val progressText = TextView(this)
        progressText.text = "$progress%"
        progressText.textSize = 14f
        progressText.setTextColor(when {
            progress >= 80 -> resources.getColor(android.R.color.holo_green_dark)
            progress >= 50 -> resources.getColor(android.R.color.holo_orange_dark)
            else -> resources.getColor(android.R.color.holo_red_dark)
        })

        linearLayout.addView(dateText)
        linearLayout.addView(statsText)
        linearLayout.addView(progressText)

        cardView.addView(linearLayout)
        return cardView
    }
}