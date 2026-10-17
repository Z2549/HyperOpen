package com.hyperos.quickopen.ui

import androidx.compose.runtime.compositionLocalOf

/** 设置页界面风格（对应 QuickOpenConfig.UI_STYLE_*） */
enum class UiStyle { MATERIAL, MIUIX }

val LocalUiStyle = compositionLocalOf { UiStyle.MATERIAL }
