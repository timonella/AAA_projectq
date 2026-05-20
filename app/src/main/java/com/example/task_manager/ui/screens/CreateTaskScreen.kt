package com.example.task_manager.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_manager.Priority
import com.example.task_manager.RepeatType
import com.example.task_manager.Task
import com.example.task_manager.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: TaskViewModel,
    onBackClick: () -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Работа") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedRepeatType by remember { mutableStateOf(RepeatType.NONE) }
    var showRepeatEndDate by remember { mutableStateOf(false) }
    var repeatEndDate by remember { mutableStateOf<Calendar?>(null) }
    var reminderMinutes by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val categories = listOf("Работа", "Личное", "Учеба", "Дом", "Здоровье", "Другое")
    val priorities = listOf(
        Priority.LOW to "Низкий",
        Priority.MEDIUM to "Средний",
        Priority.HIGH to "Высокий"
    )
    val repeatTypes = listOf(
        RepeatType.NONE to "Нет",
        RepeatType.DAILY to "Ежедневно",
        RepeatType.WEEKLY to "Еженедельно",
        RepeatType.MONTHLY to "Ежемесячно"
    )
    val reminderOptions = listOf(
        "Без напоминания" to 0,
        "За 5 минут" to 5,
        "За 15 минут" to 15,
        "За 30 минут" to 30,
        "За 1 час" to 60,
        "За 1 день" to 1440
    )

    // Форматирование даты и времени для отображения
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val endDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // DatePicker диалог
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                showDatePicker = false
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Выберите дату")
            show()
        }
        showDatePicker = false
    }

    // DatePicker для даты окончания повторений
    if (showEndDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                if (repeatEndDate == null) {
                    repeatEndDate = Calendar.getInstance()
                }
                repeatEndDate?.set(year, month, dayOfMonth)
                showEndDatePicker = false
            },
            (repeatEndDate?.get(Calendar.YEAR) ?: selectedDate.get(Calendar.YEAR)),
            (repeatEndDate?.get(Calendar.MONTH) ?: selectedDate.get(Calendar.MONTH)),
            (repeatEndDate?.get(Calendar.DAY_OF_MONTH) ?: selectedDate.get(Calendar.DAY_OF_MONTH))
        ).apply {
            setTitle("Дата окончания повторений")
            show()
        }
        showEndDatePicker = false
    }

    // TimePicker диалог
    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                selectedDate.set(Calendar.MINUTE, minute)
                showTimePicker = false
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true
        ).apply {
            setTitle("Выберите время")
            show()
        }
        showTimePicker = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать задачу") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Название задачи
            OutlinedTextField(
                value = taskName,
                onValueChange = {
                    if (it.length <= 20) {
                        taskName = it
                    }
                },
                label = { Text("Название задачи") },
                modifier = Modifier.fillMaxWidth(),
                isError = taskName.length > 20,
                singleLine = true
            )
            Text(
                text = "${taskName.length}/20 символов",
                fontSize = 12.sp,
                color = if (taskName.length > 20)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= 100) {
                        description = it
                    }
                },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                minLines = 3,
                isError = description.length > 100,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${description.length}/100 символов")
                        if (100 - description.length <= 10) {
                            Text(
                                "Осталось ${100 - description.length} символов",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            )

            // Категория
            Text("Категория:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // Приоритет
            Text("Приоритет:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            var expandedPriority by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedPriority,
                onExpandedChange = { expandedPriority = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = priorities.find { it.first == selectedPriority }?.second ?: "Средний",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedPriority,
                    onDismissRequest = { expandedPriority = false }
                ) {
                    priorities.forEach { (priority, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedPriority = priority
                                expandedPriority = false
                            }
                        )
                    }
                }
            }

            // Выбор даты и времени
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Дата и время выполнения",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dateFormat.format(selectedDate.time))
                        }

                        Button(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(timeFormat.format(selectedDate.time))
                        }
                    }
                }
            }

            // Повторение
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Повторение задачи",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Тип повторения
                    var expandedRepeatType by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedRepeatType,
                        onExpandedChange = { expandedRepeatType = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = repeatTypes.find { it.first == selectedRepeatType }?.second ?: "Нет",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRepeatType) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRepeatType,
                            onDismissRequest = { expandedRepeatType = false }
                        ) {
                            repeatTypes.forEach { (type, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedRepeatType = type
                                        expandedRepeatType = false
                                    }
                                )
                            }
                        }
                    }

                    // Дата окончания повторений (показываем только если выбран тип повторения)
                    if (selectedRepeatType != RepeatType.NONE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "",
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (repeatEndDate != null)
                                            endDateFormat.format(repeatEndDate?.time)
                                        else
                                            "Без окончания"
                                    )
                                }

                                if (repeatEndDate != null) {
                                    IconButton(
                                        onClick = { repeatEndDate = null }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Очистить"
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Новые задачи будут создаваться до этой даты",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Напоминание
            Text("Напоминание:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            var expandedReminder by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedReminder,
                onExpandedChange = { expandedReminder = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = reminderOptions.find { it.second == reminderMinutes }?.first ?: "Без напоминания",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReminder) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedReminder,
                    onDismissRequest = { expandedReminder = false }
                ) {
                    reminderOptions.forEach { (label, minutes) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                reminderMinutes = minutes
                                expandedReminder = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка сохранения
            // В кнопке сохранения:
            Button(
                onClick = {
                    if (taskName.isNotEmpty()) {
                        val newTask = Task(
                            name = taskName,
                            description = description,
                            category = selectedCategory,
                            priority = selectedPriority,
                            dueDate = selectedDate.time,
                            dueTime = null,
                            repeatType = selectedRepeatType,
                            reminderMinutes = reminderMinutes,  // Добавлено поле напоминания
                            repeatEndDate = repeatEndDate?.time,  // Дата окончания повторений
                            repeatParentId = null,
                            repeatInstanceId = null
                        )
                        viewModel.addTaskWithRepeat(newTask)
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = taskName.isNotEmpty()
            ) {
                Text("Создать задачу", fontSize = 16.sp)
            }
        }
    }
}