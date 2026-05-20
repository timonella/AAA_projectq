package com.example.task_manager.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class WindowSizeClass {
    object Compact : WindowSizeClass()      // < 600dp
    object Medium : WindowSizeClass()       // 600-840dp
    object Expanded : WindowSizeClass()     // >= 840dp
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp

    return when {
        width < 600 -> WindowSizeClass.Compact
        width < 840 -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}

@Composable
fun getAdaptivePadding(): Dp {
    val sizeClass = rememberWindowSizeClass()
    return when (sizeClass) {
        WindowSizeClass.Compact -> 16.dp
        WindowSizeClass.Medium -> 24.dp
        WindowSizeClass.Expanded -> 32.dp
    }
}

@Composable
fun getStatusBarPadding() = WindowInsets.statusBars.asPaddingValues()

@Composable
fun getNavigationBarPadding() = WindowInsets.navigationBars.asPaddingValues()