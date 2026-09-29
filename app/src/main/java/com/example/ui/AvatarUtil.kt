package com.example.ui

import androidx.compose.ui.graphics.Color

fun getAvatarColor(username: String): Color {
    val colors = listOf(
        Color(0xFF00796B),
        Color(0xFF0097A7),
        Color(0xFF0288D1),
        Color(0xFF3949AB),
        Color(0xFF5E35B1),
        Color(0xFF8E24AA),
        Color(0xFFD81B60),
        Color(0xFFE53935),
        Color(0xFFF4511E),
        Color(0xFFFB8C00),
        Color(0xFF43A047)
    )
    val hash = kotlin.math.abs(username.hashCode())
    return colors[hash % colors.size]
}
