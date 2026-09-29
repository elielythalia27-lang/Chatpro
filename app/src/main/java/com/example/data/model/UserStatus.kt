package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class UserStatus(
    val id: String = UUID.randomUUID().toString(),
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatar: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val viewersCount: Int = 0,
    val viewers: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap() // username -> emoji
) {
    val isExpired: Boolean
        get() = (System.currentTimeMillis() - createdAt) > (24 * 60 * 60 * 1000)
}
