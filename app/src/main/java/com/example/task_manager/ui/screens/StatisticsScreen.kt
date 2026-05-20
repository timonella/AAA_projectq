package com.example.task_manager.ui.screens

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.task_manager.Task
import com.example.task_manager.TaskViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: TaskViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.WEEK) }
    var showExportMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val allTasks by viewModel.tasks.collectAsState()
    val archivedTasks by viewModel.archivedTasks.collectAsState()

    val allTasksList = remember(allTasks, archivedTasks) {
        allTasks + archivedTasks
    }

    val periodStats = remember(allTasksList, selectedPeriod) {
        calculatePeriodStatistics(allTasksList, selectedPeriod)
    }

    val categoriesStats = remember(allTasksList) {
        calculateCategoriesStatistics(allTasksList)
    }

    val dailyProductivity = remember(allTasksList, selectedPeriod) {
        calculateDailyProductivity(allTasksList, selectedPeriod)
    }

    // Экспорт в CSV
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    exportStatisticsToCsv(context, it, allTasksList, categoriesStats, dailyProductivity)
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Ошибка экспорта: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Экспорт")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Экспорт в CSV") },
                            onClick = {
                                showExportMenu = false
                                exportLauncher.launch("statistics_${System.currentTimeMillis()}.csv")
                            },
                            leadingIcon = { Icon(Icons.Default.Description, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Экспорт в PDF") },
                            onClick = {
                                showExportMenu = false
                                scope.launch {
                                    isLoading = true
                                    try {
                                        exportStatisticsToPdf(context, allTasksList, categoriesStats, dailyProductivity)
                                        errorMessage = null
                                    } catch (e: Exception) {
                                        errorMessage = "Ошибка экспорта PDF: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PeriodSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { selectedPeriod = it }
                    )
                }

                item {
                    Text(
                        text = "Общая статистика",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    GeneralStatisticsCard(periodStats)
                }

                item {
                    Text(
                        text = "Распределение по категориям",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(categoriesStats) { categoryStat ->
                    CategoryStatCard(categoryStat)
                }

                item {
                    Text(
                        text = "Продуктивность по дням",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(dailyProductivity) { dayStat ->
                    DayProductivityCard(dayStat)
                }
            }

            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

enum class StatisticsPeriod {
    DAY, WEEK, MONTH, ALL_TIME
}

data class PeriodStatistics(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val progressPercent: Int
)

data class CategoryStatistics(
    val category: String,
    val total: Int,
    val completed: Int,
    val progressPercent: Int
)

data class DailyProductivity(
    val date: String,
    val dateObj: Date,
    val total: Int,
    val completed: Int,
    val progressPercent: Int
)

@Composable
fun PeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodSelected: (StatisticsPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatisticsPeriod.values().forEach { period ->
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        when (period) {
                            StatisticsPeriod.DAY -> "День"
                            StatisticsPeriod.WEEK -> "Неделя"
                            StatisticsPeriod.MONTH -> "Месяц"
                            StatisticsPeriod.ALL_TIME -> "Всё время"
                        },
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
fun GeneralStatisticsCard(stats: PeriodStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    title = "Всего задач",
                    value = stats.total.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                MetricItem(
                    title = "Выполнено",
                    value = stats.completed.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                MetricItem(
                    title = "В процессе",
                    value = stats.pending.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Прогресс", fontSize = 14.sp)
                    Text("${stats.progressPercent}%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = stats.progressPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricItem(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryStatCard(stat: CategoryStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stat.category,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stat.completed}/${stat.total}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = stat.progressPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = getCategoryColor(stat.category),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stat.progressPercent}% выполнено",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DayProductivityCard(dayStat: DailyProductivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                dayStat.progressPercent >= 80 -> Color(0xFFE8F5E9)
                dayStat.progressPercent >= 50 -> Color(0xFFFFF3E0)
                else -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    text = formatDate(dayStat.dateObj),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${dayStat.total} задач",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${dayStat.completed}/${dayStat.total}",
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${dayStat.progressPercent}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    dayStat.progressPercent >= 80 -> Color(0xFF4CAF50)
                    dayStat.progressPercent >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Логика расчета статистики
fun calculatePeriodStatistics(tasks: List<Task>, period: StatisticsPeriod): PeriodStatistics {
    val now = Date()

    val filteredTasks = when (period) {
        StatisticsPeriod.DAY -> {
            tasks.filter { isSameDay(it.dueDate, now) }
        }
        StatisticsPeriod.WEEK -> {
            val weekAgo = Date(now.time - 7 * 24 * 60 * 60 * 1000)
            tasks.filter { it.dueDate >= weekAgo && it.dueDate <= now }
        }
        StatisticsPeriod.MONTH -> {
            val monthAgo = Date(now.time - 30 * 24 * 60 * 60 * 1000)
            tasks.filter { it.dueDate >= monthAgo && it.dueDate <= now }
        }
        StatisticsPeriod.ALL_TIME -> tasks
    }

    val total = filteredTasks.size
    val completed = filteredTasks.count { it.isCompleted }
    val pending = total - completed
    val progressPercent = if (total > 0) (completed * 100 / total) else 0

    return PeriodStatistics(total, completed, pending, progressPercent)
}

fun calculateCategoriesStatistics(tasks: List<Task>): List<CategoryStatistics> {
    val categoriesMap = mutableMapOf<String, MutableList<Task>>()

    for (task in tasks) {
        if (!categoriesMap.containsKey(task.category)) {
            categoriesMap[task.category] = mutableListOf()
        }
        categoriesMap[task.category]?.add(task)
    }

    return categoriesMap.map { (category, taskList) ->
        val total = taskList.size
        val completed = taskList.count { it.isCompleted }
        val progressPercent = if (total > 0) (completed * 100 / total) else 0

        CategoryStatistics(category, total, completed, progressPercent)
    }.sortedByDescending { it.total }
}

fun calculateDailyProductivity(tasks: List<Task>, period: StatisticsPeriod): List<DailyProductivity> {
    val now = Date()

    val startDate = when (period) {
        StatisticsPeriod.DAY -> {
            Date(now.time - 7 * 24 * 60 * 60 * 1000)
        }
        StatisticsPeriod.WEEK -> {
            Date(now.time - 7 * 24 * 60 * 60 * 1000)
        }
        StatisticsPeriod.MONTH -> {
            Date(now.time - 30 * 24 * 60 * 60 * 1000)
        }
        StatisticsPeriod.ALL_TIME -> {
            Date(0)
        }
    }

    val filteredTasks = tasks.filter { it.dueDate >= startDate && it.dueDate <= now }

    val dailyMap = mutableMapOf<String, MutableList<Task>>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    for (task in filteredTasks) {
        val dateKey = dateFormat.format(task.dueDate)
        if (!dailyMap.containsKey(dateKey)) {
            dailyMap[dateKey] = mutableListOf()
        }
        dailyMap[dateKey]?.add(task)
    }

    return dailyMap.map { (dateKey, taskList) ->
        val total = taskList.size
        val completed = taskList.count { it.isCompleted }
        val progressPercent = if (total > 0) (completed * 100 / total) else 0

        DailyProductivity(
            date = formatDateDisplay(dateKey),
            dateObj = dateFormat.parse(dateKey) ?: Date(),
            total = total,
            completed = completed,
            progressPercent = progressPercent
        )
    }.sortedByDescending { it.dateObj }
}

// Вспомогательные функции
fun isSameDay(date1: Date, date2: Date): Boolean {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(date1) == format.format(date2)
}

fun formatDate(date: Date): String {
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return format.format(date)
}

fun formatDateDisplay(dateString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateString)
        SimpleDateFormat("dd MMMM", Locale("ru")).format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "работа", "work" -> Color(0xFF1976D2)
        "личное", "personal" -> Color(0xFF4CAF50)
        "учеба", "study" -> Color(0xFFFF9800)
        "дом", "home" -> Color(0xFF9C27B0)
        "здоровье", "health" -> Color(0xFFE91E63)
        else -> Color(0xFF607D8B)
    }
}

// Экспорт в CSV
suspend fun exportStatisticsToCsv(
    context: Context,
    uri: Uri,
    tasks: List<Task>,
    categoriesStats: List<CategoryStatistics>,
    dailyProductivity: List<DailyProductivity>
) {
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        val csvContent = buildString {
            appendLine("Статистика задач")
            appendLine("Дата экспорта: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()

            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            val pending = total - completed
            val progress = if (total > 0) (completed * 100 / total) else 0

            appendLine("ОБЩАЯ СТАТИСТИКА")
            appendLine("Всего задач,$total")
            appendLine("Выполнено,$completed")
            appendLine("В процессе,$pending")
            appendLine("Прогресс,$progress%")
            appendLine()

            appendLine("СТАТИСТИКА ПО КАТЕГОРИЯМ")
            appendLine("Категория,Всего,Выполнено,Прогресс")
            categoriesStats.forEach { stat ->
                appendLine("${stat.category},${stat.total},${stat.completed},${stat.progressPercent}%")
            }
            appendLine()

            appendLine("ПРОДУКТИВНОСТЬ ПО ДНЯМ")
            appendLine("Дата,Всего задач,Выполнено,Прогресс")
            dailyProductivity.forEach { day ->
                appendLine("${day.date},${day.total},${day.completed},${day.progressPercent}%")
            }
        }

        outputStream.write(csvContent.toByteArray())
        outputStream.flush()
    }
}

// Исправленный экспорт в PDF через WebView
suspend fun exportStatisticsToPdf(
    context: Context,
    tasks: List<Task>,
    categoriesStats: List<CategoryStatistics>,
    dailyProductivity: List<DailyProductivity>
) = suspendCoroutine<Unit> { continuation ->

    val htmlContent = generateStatisticsHtml(tasks, categoriesStats, dailyProductivity)

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            try {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = view?.createPrintDocumentAdapter("statistics")

                if (printAdapter != null) {
                    printManager.print(
                        "Статистика задач.pdf",
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .build()
                    )
                }
                continuation.resume(Unit)
            } catch (e: Exception) {
                continuation.resume(Unit)
            }
        }
    }

    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

// Генерация HTML для PDF
fun generateStatisticsHtml(
    tasks: List<Task>,
    categoriesStats: List<CategoryStatistics>,
    dailyProductivity: List<DailyProductivity>
): String {
    val total = tasks.size
    val completed = tasks.count { it.isCompleted }
    val pending = total - completed
    val progress = if (total > 0) (completed * 100 / total) else 0

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Статистика задач</title>
    <style>
        body { font-family: 'Roboto', Arial, sans-serif; margin: 20px; padding: 20px; color: #333; }
        h1 { color: #2196F3; text-align: center; border-bottom: 2px solid #2196F3; padding-bottom: 10px; }
        h2 { color: #555; margin-top: 30px; border-left: 4px solid #2196F3; padding-left: 15px; }
        .header-info { text-align: center; color: #666; margin-bottom: 30px; }
        .stats-grid { display: flex; justify-content: space-around; margin: 20px 0; flex-wrap: wrap; }
        .stat-card { background: #f5f5f5; border-radius: 8px; padding: 15px; text-align: center; min-width: 120px; margin: 10px; }
        .stat-number { font-size: 36px; font-weight: bold; color: #2196F3; }
        .stat-label { font-size: 14px; color: #666; margin-top: 5px; }
        .progress-bar-container { background: #e0e0e0; border-radius: 10px; height: 20px; margin: 10px 0; overflow: hidden; }
        .progress-bar { background: #4CAF50; height: 100%; border-radius: 10px; width: ${progress}%; }
        table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #2196F3; color: white; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .category-row { margin: 10px 0; }
        .category-name { font-weight: bold; margin-bottom: 5px; }
        .footer { text-align: center; margin-top: 50px; color: #999; font-size: 12px; border-top: 1px solid #eee; padding-top: 20px; }
    </style>
</head>
<body>
    <h1>📊 Статистика задач</h1>
    <div class="header-info">
        Дата генерации: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}
    </div>
    
    <h2>Общая статистика</h2>
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-number">$total</div>
            <div class="stat-label">Всего задач</div>
        </div>
        <div class="stat-card">
            <div class="stat-number" style="color:#4CAF50">$completed</div>
            <div class="stat-label">Выполнено</div>
        </div>
        <div class="stat-card">
            <div class="stat-number" style="color:#FF9800">$pending</div>
            <div class="stat-label">В процессе</div>
        </div>
    </div>
    
    <div class="progress-bar-container">
        <div class="progress-bar"></div>
    </div>
    <div style="text-align: center">Общий прогресс: ${progress}%</div>
    
    <h2>📁 Распределение по категориям</h2>
    ${categoriesStats.joinToString("") { stat ->
        val catProgress = stat.progressPercent
        """
        <div class="category-row">
            <div class="category-name">${stat.category}</div>
            <div class="progress-bar-container">
                <div class="progress-bar" style="width: ${catProgress}%"></div>
            </div>
            <div class="category-stats">
                Выполнено: ${stat.completed} из ${stat.total} (${catProgress}%)
            </div>
        </div>
        """
    }}
    
    <h2>📅 Продуктивность по дням</h2>
    <table>
        <thead>
            <tr><th>Дата</th><th>Всего задач</th><th>Выполнено</th><th>Прогресс</th></tr>
        </thead>
        <tbody>
            ${dailyProductivity.take(30).joinToString("") { day ->
        """
                <tr>
                    <td>${day.date}</td>
                    <td>${day.total}</td>
                    <td>${day.completed}</td>
                    <td>${day.progressPercent}%</td>
                </tr>
                """
    }}
        </tbody>
    </table>
    
    <div class="footer">
        Приложение Менеджер задач<br>
        Отчет сгенерирован автоматически
    </div>
</body>
</html>
    """.trimIndent()
}