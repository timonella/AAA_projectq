package com.example.task_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_manager.Priority
import com.example.task_manager.RepeatType
import com.example.task_manager.Task
import com.example.task_manager.TaskViewModel
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    onBackClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentMonth = remember { mutableStateOf(getCurrentYearMonth()) }
    val selectedDateStr = remember { mutableStateOf(getCurrentLocalDate()) }
    val selectedDateObj = remember { mutableStateOf(Date()) }

    val tasksForDate by viewModel.getTasksForDate(selectedDateObj.value).collectAsState(initial = emptyList())


    val tasksByDate = remember(tasks) {
        tasks.filter { !it.isArchived && !it.isCompleted }
            .groupBy { task -> getDateKey(task.dueDate) }
    }

    val selectedDateTasks = remember(selectedDateStr.value, tasks) {
        tasks.filter { task ->
            getDateKey(task.dueDate) == selectedDateStr.value &&
                    !task.isCompleted &&
                    !task.isArchived
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { tasks.size }
            .collect {
                // Просто триггерим перерисовку
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь задач", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onAddTaskClick) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Календарь - уменьшенный
            MonthCalendar(
                currentMonth = currentMonth.value,
                selectedDate = selectedDateStr.value,
                tasksByDate = tasksByDate,
                onDateClick = { date ->
                    selectedDateStr.value = date
                    // Конвертируем строку в Date для getTasksForDate
                    val parts = date.split("-")
                    if (parts.size == 3) {
                        val calendar = Calendar.getInstance()
                        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                        selectedDateObj.value = calendar.time
                    }
                },
                onMonthChange = { newMonth ->
                    currentMonth.value = newMonth
                }
            )

            // Заголовок списка задач
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp), // Уменьшил вертикальный отступ
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateForHeader(selectedDateStr.value),
                    fontSize = 16.sp, // Уменьшил размер шрифта
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedDateTasks.isNotEmpty()) {
                    Text(
                        text = "${selectedDateTasks.size} задач",
                        fontSize = 12.sp, // Уменьшил размер шрифта
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Список задач на выбранную дату
            if (selectedDateTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Нет задач на этот день",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Нажмите на кнопку '+' чтобы добавить задачу",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDateTasks) { task ->
                        CalendarTaskCard(
                            task = task,
                            onToggleComplete = {
                                if (task.repeatType != RepeatType.NONE) {
                                    viewModel.completeRepeatTask(task)
                                } else {
                                    viewModel.toggleTaskCompletion(task)
                                }
                            },
                            onDelete = {
                                viewModel.deleteTask(task.id)
                                selectedDateStr.value = selectedDateStr.value
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCalendar(
    currentMonth: YearMonth,
    selectedDate: String,
    tasksByDate: Map<String, List<Task>>,
    onDateClick: (String) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOffset = getFirstDayOffset(currentMonth)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Уменьшил отступы
        shape = RoundedCornerShape(12.dp), // Уменьшил скругление
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Уменьшил тень
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Заголовок месяца с навигацией
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp), // Уменьшил отступы
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(currentMonth.minusMonths(1)) },
                    modifier = Modifier.size(32.dp) // Уменьшил размер кнопки
                ) {
                    Text("←", fontSize = 20.sp) // Уменьшил шрифт
                }

                Text(
                    text = formatMonth(currentMonth),
                    fontSize = 16.sp, // Уменьшил шрифт
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { onMonthChange(currentMonth.plusMonths(1)) },
                    modifier = Modifier.size(32.dp) // Уменьшил размер кнопки
                ) {
                    Text("→", fontSize = 20.sp) // Уменьшил шрифт
                }
            }

            // Дни недели
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp), // Уменьшил отступы
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Medium, // Изменил с Bold на Medium
                        fontSize = 12.sp, // Уменьшил шрифт
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Календарная сетка
            val rows = (daysInMonth + firstDayOffset + 6) / 7
            Column(
                modifier = Modifier.padding(horizontal = 4.dp) // Добавил небольшой отступ по бокам
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp), // Уменьшил вертикальные отступы
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until 7) {
                            val dayNumber = row * 7 + col - firstDayOffset + 1
                            val dateKey = if (dayNumber in 1..daysInMonth) {
                                formatDateKey(currentMonth.year, currentMonth.monthValue, dayNumber)
                            } else {
                                null
                            }

                            CalendarDayCell(
                                dateKey = dateKey,
                                dayNumber = if (dayNumber in 1..daysInMonth) dayNumber else null,
                                isSelected = dateKey == selectedDate,
                                tasks = dateKey?.let { tasksByDate[it] } ?: emptyList(),
                                onDateClick = { onDateClick(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dateKey: String?,
    dayNumber: Int?,
    isSelected: Boolean,
    tasks: List<Task>,
    onDateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dateKey == null || dayNumber == null) {
        Box(modifier = modifier.size(36.dp)) // Уменьшил размер с 48.dp до 36.dp
    } else {
        val isToday = dateKey == getCurrentLocalDate()
        val hasTasks = tasks.isNotEmpty()
        val highPriorityTasks = tasks.count { it.priority == Priority.HIGH }
        val mediumPriorityTasks = tasks.count { it.priority == Priority.MEDIUM }

        Box(
            modifier = modifier
                .size(36.dp) // Уменьшил размер ячейки с 48.dp до 36.dp
                .clip(RoundedCornerShape(6.dp)) // Уменьшил скругление
                .clickable { onDateClick(dateKey) }
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        isToday -> MaterialTheme.colorScheme.secondaryContainer
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Число месяца
                Text(
                    text = dayNumber.toString(),
                    fontSize = 12.sp, // Уменьшил шрифт с 16.sp до 12.sp
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Маркеры задач
                if (hasTasks) {
                    Row(
                        modifier = Modifier.padding(top = 1.dp), // Уменьшил отступ
                        horizontalArrangement = Arrangement.spacedBy(1.dp) // Уменьшил расстояние
                    ) {
                        if (highPriorityTasks > 0) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp) // Уменьшил размер маркера с 4.dp до 3.dp
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                        }
                        if (mediumPriorityTasks > 0) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9800))
                            )
                        }
                        if (tasks.size > (highPriorityTasks + mediumPriorityTasks)) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CalendarTaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Чекбокс
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (task.isCompleted) "Выполнено" else "Не выполнено",
                        tint = if (task.isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Информация о задаче
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted)
                            TextDecoration.LineThrough
                        else
                            null
                    )

                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Приоритет
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (task.priority) {
                                        Priority.HIGH -> Color.Red
                                        Priority.MEDIUM -> Color(0xFFFF9800)
                                        Priority.LOW -> Color(0xFF4CAF50)
                                    }
                                )
                        )

                        // Категория
                        Text(
                            text = task.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        // Время
                        task.dueTime?.let { time ->
                            Text(
                                text = time,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Кнопка удаления
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Удалить",
                    modifier = Modifier.rotate(45f),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Вспомогательные функции
fun getCurrentYearMonth(): YearMonth {
    return YearMonth.now()
}

fun getCurrentLocalDate(): String {
    val calendar = Calendar.getInstance()
    return formatDateKey(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun getDateKey(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date
    return formatDateKey(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun formatDateKey(year: Int, month: Int, day: Int): String {
    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
}

fun getFirstDayOffset(yearMonth: YearMonth): Int {
    val firstDayOfMonth = yearMonth.atDay(1)
    // Понедельник = 1, Воскресенье = 7
    val dayOfWeek = firstDayOfMonth.dayOfWeek.value
    return (dayOfWeek + 5) % 7 // Смещение для понедельника как первого дня
}

fun formatMonth(yearMonth: YearMonth): String {
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
    return yearMonth.format(formatter)
}

fun formatDateForHeader(dateKey: String): String {
    try {
        val parts = dateKey.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)

            val dateFormat = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("ru"))
            return dateFormat.format(calendar.time)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return dateKey
}