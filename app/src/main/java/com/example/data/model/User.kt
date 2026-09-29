package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val bio: String = "¡Hola! Uso ChatPro.",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false
) {
    val cleanUsername: String
        get() = if (username.startsWith("@")) username else "@$username"
}
