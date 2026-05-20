package com.example.task_manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_manager.TaskViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Класс для товара в магазине
data class StoreItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val icon: String,
    val type: String // theme, icon, background и т.д.
)

// Класс для управления баллами пользователя
class UserPointsManager {
    private val _userPoints = MutableStateFlow(100) // Начальные баллы
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    fun addPoints(amount: Int) {
        _userPoints.value += amount
    }

    fun spendPoints(amount: Int): Boolean {
        return if (_userPoints.value >= amount) {
            _userPoints.value -= amount
            true
        } else {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    viewModel: TaskViewModel,
    onBackClick: () -> Unit
) {
    val pointsManager = remember { UserPointsManager() }
    val userPoints by pointsManager.userPoints.collectAsState()

    // Список доступных товаров
    val storeItems = remember {
        listOf(
            StoreItem(1, "Темная тема", "Измените внешний вид приложения", 50, "🌙", "theme"),
            StoreItem(2, "Золотая иконка", "Премиальная иконка приложения", 100, "👑", "icon"),
            StoreItem(3, "Фоновое изображение", "Красивый фон для приложения", 80, "🖼️", "background"),
            StoreItem(4, "Без рекламы", "Отключите всю рекламу", 200, "🚫", "premium"),
            StoreItem(5, "Эксклюзивные стикеры", "Набор стикеров для задач", 30, "🎨", "stickers"),
            StoreItem(6, "Расширенная статистика", "Детальная аналитика задач", 150, "📊", "analytics")
        )
    }

    // Купленные товары
    val purchasedItems = remember { mutableStateListOf<Int>() }

    // Показываем уведомление
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Магазин", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Отображение баллов пользователя
                    Card(
                        modifier = Modifier.padding(end = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⭐", fontSize = 16.sp)
                            Text(
                                text = userPoints.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Информация о баллах
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ваши баллы",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = userPoints.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Подсказка как получить баллы
                    Button(
                        onClick = {
                            snackbarMessage = "Выполняйте задачи, чтобы получать баллы!"
                            showSnackbar = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Как получить?")
                    }
                }
            }

            // Список товаров
            Text(
                text = "Доступные товары:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(storeItems) { item ->
                    StoreItemCard(
                        item = item,
                        isPurchased = purchasedItems.contains(item.id),
                        canPurchase = userPoints >= item.price && !purchasedItems.contains(item.id),
                        onPurchase = {
                            if (pointsManager.spendPoints(item.price)) {
                                purchasedItems.add(item.id)
                                snackbarMessage = "Вы купили: ${item.name}! Спасибо за покупку!"
                                showSnackbar = true
                            } else {
                                snackbarMessage = "Недостаточно баллов для покупки ${item.name}"
                                showSnackbar = true
                            }
                        }
                    )
                }
            }
        }



            // Автоматически скрываем через 2 секунды
            LaunchedEffect(showSnackbar) {
                kotlinx.coroutines.delay(2000)
                showSnackbar = false
            }
        }
    }


@Composable
fun StoreItemCard(
    item: StoreItem,
    isPurchased: Boolean,
    canPurchase: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка товара
                Text(
                    text = item.icon,
                    fontSize = 40.sp
                )

                // Информация о товаре
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.price} баллов",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Кнопка покупки
            when {
                isPurchased -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Text("✅ Куплено")
                    }
                }
                canPurchase -> {
                    Button(
                        onClick = onPurchase,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Купить")
                    }
                }
                else -> {
                    Button(
                        onClick = {},
                        enabled = false
                    ) {
                        Text("❌ Недостаточно баллов")
                    }
                }
            }
        }
    }
}