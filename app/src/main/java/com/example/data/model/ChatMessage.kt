package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class MessageType {
    TEXT,
    PHOTO,
    AUDIO,
    FILE
}

enum class MessageStatus {
    SENDING,
    SENT,
    READ
}

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderUsername: String,
    val recipientUsername: String,
    val text: String = "",
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val reactions: Map<String, String> = emptyMap(), // username -> emoji
    val replyToId: String? = null,
    val replyToSnippet: String? = null,
    val audioDurationSeconds: Int = 0,
    val isViewOnce: Boolean = false,
    val isViewed: Boolean = false
)
