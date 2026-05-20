package com.example.task_manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_manager.Task
import com.example.task_manager.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: TaskViewModel,
    onBackClick: () -> Unit
) {
    val archivedTasks by viewModel.archivedTasks.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPriority by remember { mutableStateOf<com.example.task_manager.Priority?>(null) }

    val categories = archivedTasks.map { it.category }.distinct()
    val priorities = com.example.task_manager.Priority.values().toList()

    val filteredTasks = archivedTasks.filter { task ->
        (selectedCategory == null || task.category == selectedCategory) &&
                (selectedPriority == null || task.priority == selectedPriority)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Архив") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Фильтры
            Text("Фильтры:", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

            FilterChips(
                options = listOf(null) + categories,
                selected = selectedCategory,
                onSelected = { selectedCategory = it },
                labelProvider = { it ?: "Все категории" }
            )

            FilterChips(
                options = listOf(null) + priorities,
                selected = selectedPriority,
                onSelected = { selectedPriority = it },
                labelProvider = { p ->
                    when (p) {
                        null -> "Все приоритеты"
                        com.example.task_manager.Priority.LOW -> "Низкий"
                        com.example.task_manager.Priority.MEDIUM -> "Средний"
                        com.example.task_manager.Priority.HIGH -> "Высокий"
                    }
                }
            )

            Divider()

            Text("Архивные задачи (${filteredTasks.size}):", fontSize = 14.sp)

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет задач в архиве", fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks) { task ->
                        ArchivedTaskCard(
                            task = task,
                            onRestoreClick = { viewModel.restoreTask(task.id) },
                            onDeleteClick = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> FilterChips(
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit,
    labelProvider: (T?) -> String
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(options.size) { index ->
            FilterChip(
                selected = options[index] == selected,
                onClick = { onSelected(if (options[index] == selected) null else options[index]) },
                label = { Text(labelProvider(options[index]), fontSize = 12.sp) }
            )
        }
    }
}

@Composable
fun ArchivedTaskCard(
    task: Task,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Форматирование даты и времени
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val dueDateString = task.dueDate?.let { dateFormat.format(it) } ?: "Не указана"
    val dueTimeString = if (task.dueTime != null && task.dueTime.isNotBlank()) {
        task.dueTime
    } else {
        task.dueDate?.let { date -> timeFormat.format(date) } ?: "Не указано"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Первая строка: название и кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✓ " + task.name,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = onRestoreClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("↩️", fontSize = 16.sp)
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            }

            // Описание задачи (если есть)
            if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Информация о дате и времени выполнения
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text("дата: $dueDateString", fontSize = 11.sp)
                    }
                )
                AssistChip(
                    onClick = { },
                    label = {
                        Text("время: $dueTimeString", fontSize = 11.sp)
                    }
                )
            }

            // Категория и приоритет
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text("${task.category}", fontSize = 10.sp) }
                )
                AssistChip(
                    onClick = { },
                    label = {
                        val priorityText = when (task.priority) {
                            com.example.task_manager.Priority.LOW -> "🟢 Низкий"
                            com.example.task_manager.Priority.MEDIUM -> "🟡 Средний"
                            com.example.task_manager.Priority.HIGH -> "🔴 Высокий"
                        }
                        Text(priorityText, fontSize = 10.sp)
                    }
                )
                // Показать дату завершения (если есть)
                task.completedAt?.let { completedDate ->
                    val completedTimeString = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(completedDate)
                    AssistChip(
                        onClick = { },
                        label = {
                            Text("Завершена: $completedTimeString", fontSize = 10.sp)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить задачу") },
            text = { Text("Вы уверены, что хотите безвозвратно удалить задачу \"${task.name}\"?") },
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