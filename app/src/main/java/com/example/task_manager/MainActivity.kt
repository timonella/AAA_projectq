package com.example.task_manager

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.task_manager.ui.screens.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TaskViewModel

    // Регистрируем результат запроса разрешения на уведомления
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notifications permission granted")
            checkAndRequestExactAlarmPermission()
        } else {
            android.util.Log.d("MainActivity", "Notifications permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализируем ViewModel
        val repository = (application as TaskApplication).taskRepository
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)

        // Инициализируем уведомления
        viewModel.initNotifications(this)

        // Запрашиваем разрешения
        requestNotificationPermission()
        checkAndRequestExactAlarmPermission()

        setContent {
            TaskManagerApp(factory = factory, viewModel = viewModel)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "Notifications permission already granted")
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }
}

@Composable
fun TaskManagerApp(
    factory: TaskViewModelFactory,
    viewModel: TaskViewModel = viewModel(factory = factory)
) {
    androidx.compose.material3.MaterialTheme {
        val navController = rememberNavController()

        LaunchedEffect(Unit) {
            viewModel.createMissedRepeats()
        }

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreateTask = { navController.navigate("create_task") },
                    onNavigateToTodayTasks = { navController.navigate("today_tasks") },
                    onNavigateToCalendar = { navController.navigate("calendar") },
                    onNavigateToStatistics = { navController.navigate("statistics") },
                    onNavigateToArchive = { navController.navigate("archive") },
                    onNavigateToStore = { navController.navigate("store") }
                )
            }
            composable("create_task") {
                CreateTaskScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("today_tasks") {
                TodayTasksScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("calendar") {
                CalendarScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAddTaskClick = { navController.navigate("create_task") }
                )
            }
            composable("statistics") {
                StatisticsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("archive") {
                ArchiveScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("store") {
                StoreScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}