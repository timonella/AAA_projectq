package com.example.task_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_manager.Priority
import com.example.task_manager.RepeatType
import com.example.task_manager.Task
import com.example.task_manager.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTodayTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToStore: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isCompact = screenWidth < 600.dp

    // Все активные задачи (не выполненные и не архивные)
    val activeTasks = remember(tasks) {
        tasks.filter { !it.isCompleted && !it.isArchived }
    }

    // Выполненные задачи
    val completedCount = remember(tasks) {
        tasks.count { it.isCompleted && !it.isArchived }
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val today = dateFormat.format(Date())

    // Разделяем задачи на категории
    val todayTasks = remember(activeTasks, today) {
        activeTasks.filter { dateFormat.format(it.dueDate) == today }
    }

    val upcomingTasks = remember(activeTasks, today) {
        activeTasks.filter { dateFormat.format(it.dueDate) > today }
    }

    val overdueTasks = remember(activeTasks, today) {
        activeTasks.filter { dateFormat.format(it.dueDate) < today }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Менеджер задач",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 18.sp else 24.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать задачу")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Статистика
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            title = "Активных",
                            value = activeTasks.size.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            title = "Сегодня",
                            value = todayTasks.size.toString(),
                            color = MaterialTheme.colorScheme.secondary
                        )

                    }
                }
            }

            // Быстрые кнопки
            ``````````// Аккуратные навигационные кнопки
            item {
                // Вариант 1: Сетка 2x2 (более аккуратно)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickButton(
                            text = "Сегодня",
                            icon = Icons.Default.Today,
                            onClick = onNavigateToTodayTasks,
                            modifier = Modifier.weight(1f)
                        )
                        QuickButton(
                            text = "Календарь",
                            icon = Icons.Default.DateRange,
                            onClick = onNavigateToCalendar,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickButton(
                            text = "Статистика",
             ``````````               icon = Icons.Default.BarChart,
                            onClick = onNavigateToStatistics,
                            modifier = Modifier.weight(1f)
                        )
                        QuickButton(
                            text = "Архив",
                            icon = Icons.Default.Archive,
                            onClick = onNavigateToArchive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Просроченные задачи
            if (overdueTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Просроченные",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(overdueTasks) { task ->
                    TaskCard1(
                        task = task,
                        onCheckClick = {
                            if (task.repeatType != RepeatType.NONE) {
                                viewModel.completeRepeatTask(task)
                            } else {
                                viewModel.toggleTaskCompletion(task)
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteTask(task.id)
                        }
                    )
                }
            }

            // Задачи на сегодня
            if (todayTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Сегодня",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(todayTasks) { task ->
                    TaskCard1(
                        task = task,
                        onCheckClick = {
                            if (task.repeatType != RepeatType.NONE) {
                                viewModel.completeRepeatTask(task)
                            } else {
                                viewModel.toggleTaskCompletion(task)
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteTask(task.id)
                        }
                    )
                }
            }

            // Предстоящие задачи
            if (upcomingTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Предстоящие",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(upcomingTasks) { task ->
                    TaskCard1(
                        task = task,
                        onCheckClick = {
                            if (task.repeatType != RepeatType.NONE) {
                                viewModel.completeRepeatTask(task)
                            } else {
                                viewModel.toggleTaskCompletion(task)
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteTask(task.id)
                        }
                    )
                }
            }

            // Если нет задач
            if (activeTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет активных задач\nНажмите + чтобы добавить",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String, color: Color) {
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
fun QuickButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TaskCard1(
    task: Task,
    onCheckClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.dueDate.before(Date()) && !task.isCompleted)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (task.priority) {
                                Priority.HIGH -> Color.Red
                                Priority.MEDIUM -> Color(0xFFFF9800)
                                Priority.LOW -> Color(0xFF4CAF50)
                            }
                        )
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Text(
                            text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(task.dueDate),
                            fontSize = 11.sp,
                            color = if (task.dueDate.before(Date()) && !task.isCompleted)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (task.repeatType != RepeatType.NONE) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Повторяющаяся",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Кнопка выполнения
            IconButton(
                onClick = onCheckClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выполнить",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Кнопка удаления
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить задачу") },
            text = { Text("Вы уверены, что хотите удалить задачу \"${task.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

}