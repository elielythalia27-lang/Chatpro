package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.User
import com.example.data.model.UserStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderUsername: String,
    val recipientUsername: String,
    val text: String,
    val type: String,
    val mediaUrl: String,
    val timestamp: Long,
    val status: String,
    val reactionsRaw: String = "",
    val replyToId: String? = null,
    val replyToSnippet: String? = null,
    val audioDurationSeconds: Int = 0,
    val isViewOnce: Boolean = false,
    val isViewed: Boolean = false
) {
    fun toChatMessage(): ChatMessage {
        val parsedReactions = if (reactionsRaw.isBlank()) {
            emptyMap()
        } else {
            reactionsRaw.split(";").filter { it.contains(":") }.associate {
                val parts = it.split(":")
                parts[0] to parts[1]
            }
        }

        return ChatMessage(
            id = id,
            chatId = chatId,
            senderUsername = senderUsername,
            recipientUsername = recipientUsername,
            text = text,
            type = try { MessageType.valueOf(type) } catch (e: Exception) { MessageType.TEXT },
            mediaUrl = mediaUrl,
            timestamp = timestamp,
            status = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.SENT },
            reactions = parsedReactions,
            replyToId = replyToId,
            replyToSnippet = replyToSnippet,
            audioDurationSeconds = audioDurationSeconds,
            isViewOnce = isViewOnce,
            isViewed = isViewed
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage): MessageEntity {
            val reactionsString = msg.reactions.entries.joinToString(";") { "${it.key}:${it.value}" }
            return MessageEntity(
                id = msg.id,
                chatId = msg.chatId,
                senderUsername = msg.senderUsername,
                recipientUsername = msg.recipientUsername,
                text = msg.text,
                type = msg.type.name,
                mediaUrl = msg.mediaUrl,
                timestamp = msg.timestamp,
                status = msg.status.name,
                reactionsRaw = reactionsString,
                replyToId = msg.replyToId,
                replyToSnippet = msg.replyToSnippet,
                audioDurationSeconds = msg.audioDurationSeconds,
                isViewOnce = msg.isViewOnce,
                isViewed = msg.isViewed
            )
        }
    }
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false
) {
    fun toUser() = User(
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        isOnline = isOnline,
        lastSeen = lastSeen,
        isAdmin = isAdmin,
        isSuspended = isSuspended
    )

    companion object {
        fun fromUser(user: User) = UserEntity(
            username = user.username,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            bio = user.bio,
            isOnline = user.isOnline,
            lastSeen = user.lastSeen,
            isAdmin = user.isAdmin,
            isSuspended = user.isSuspended
        )
    }
}

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatar: String,
    val text: String,
    val mediaUrl: String,
    val createdAt: Long,
    val viewersCount: Int,
    val viewersRaw: String = ""
) {
    fun toUserStatus(): UserStatus {
        val viewersList = if (viewersRaw.isBlank()) emptyList() else viewersRaw.split(",")
        return UserStatus(
            id = id,
            authorUsername = authorUsername,
            authorDisplayName = authorDisplayName,
            authorAvatar = authorAvatar,
            text = text,
            mediaUrl = mediaUrl,
            createdAt = createdAt,
            viewersCount = viewersCount,
            viewers = viewersList
        )
    }

    companion object {
        fun fromUserStatus(status: UserStatus): StatusEntity {
            return StatusEntity(
                id = status.id,
                authorUsername = status.authorUsername,
                authorDisplayName = status.authorDisplayName,
                authorAvatar = status.authorAvatar,
                text = status.text,
                mediaUrl = status.mediaUrl,
                createdAt = status.createdAt,
                viewersCount = status.viewersCount,
                viewersRaw = status.viewers.joinToString(",")
            )
        }
    }
}

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String? = null,
    val content: String = "",
    val mediaType: String = "NONE",
    val mediaUrl: String? = null,
    val audioDurationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val likesRaw: String = "",
    val commentsRaw: String = ""
) {
    fun toPost(): com.example.data.model.Post {
        val likesList = if (likesRaw.isBlank()) emptyList() else likesRaw.split(",").filter { it.isNotBlank() }
        val commentsList = if (commentsRaw.isBlank()) {
            emptyList()
        } else {
            commentsRaw.split(";;").mapNotNull { entry ->
                val parts = entry.split("||")
                if (parts.size >= 5) {
                    com.example.data.model.PostComment(
                        id = parts[0],
                        authorUsername = parts[1],
                        authorDisplayName = parts[2],
                        authorAvatarUrl = parts[3].ifBlank { null },
                        text = parts[4],
                        timestamp = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    )
                } else null
            }
        }

        return com.example.data.model.Post(
            id = id,
            authorUsername = authorUsername,
            authorDisplayName = authorDisplayName,
            authorAvatarUrl = authorAvatarUrl,
            content = content,
            mediaType = try { com.example.data.model.PostMediaType.valueOf(mediaType) } catch (e: Exception) { com.example.data.model.PostMediaType.NONE },
            mediaUrl = mediaUrl,
            audioDurationSeconds = audioDurationSeconds,
            timestamp = timestamp,
            likes = likesList,
            comments = commentsList
        )
    }

    companion object {
        fun fromPost(post: com.example.data.model.Post): PostEntity {
            val likesString = post.likes.joinToString(",")
            val commentsString = post.comments.joinToString(";;") { comment ->
                "${comment.id}||${comment.authorUsername}||${comment.authorDisplayName}||${comment.authorAvatarUrl ?: ""}||${comment.text.replace("||", " ").replace(";;", " ")}||${comment.timestamp}"
            }
            return PostEntity(
                id = post.id,
                authorUsername = post.authorUsername,
                authorDisplayName = post.authorDisplayName,
                authorAvatarUrl = post.authorAvatarUrl,
                content = post.content,
                mediaType = post.mediaType.name,
                mediaUrl = post.mediaUrl,
                audioDurationSeconds = post.audioDurationSeconds,
                timestamp = post.timestamp,
                likesRaw = likesString,
                commentsRaw = commentsString
            )
        }
    }
}

