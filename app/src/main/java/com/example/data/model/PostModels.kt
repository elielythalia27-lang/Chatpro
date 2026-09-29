package com.example.data.model

import com.squareup.moshi.JsonClass

enum class PostMediaType {
    NONE,
    IMAGE,
    AUDIO,
    VIDEO
}

@JsonClass(generateAdapter = true)
data class PostComment(
    val id: String = "comment_${System.currentTimeMillis()}_${(100..999).random()}",
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Post(
    val id: String = "post_${System.currentTimeMillis()}_${(1000..9999).random()}",
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String? = null,
    val content: String = "",
    val mediaType: PostMediaType = PostMediaType.NONE,
    val mediaUrl: String? = null,
    val audioDurationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),
    val comments: List<PostComment> = emptyList()
) {
    val likesCount: Int get() = likes.size
    val commentsCount: Int get() = comments.size
    fun isLikedBy(username: String): Boolean = likes.contains(username)
}
