package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminStats(
    val totalStorageBytes: Long = 0L,
    val totalUsers: Int = 1,
    val activeUsers24h: Int = 1,
    val messagesToday: Int = 0,
    val activeStatuses: Int = 0,
    val storageByFolder: Map<String, Long> = mapOf(
        "mensajes" to 0L,
        "estados" to 0L,
        "avatares" to 0L,
        "archivos" to 0L
    ),
    val weeklyActivity: List<Pair<String, Int>> = listOf(
        "Lun" to 0,
        "Mar" to 0,
        "Mié" to 0,
        "Jue" to 0,
        "Vie" to 0,
        "Sáb" to 0,
        "Dom" to 0
    )
)

@JsonClass(generateAdapter = true)
data class SystemLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val isError: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GlobalConfig(
    val isRegistrationOpen: Boolean = true,
    val isTwoFactorRequired: Boolean = false,
    val areCallsEnabled: Boolean = true,
    val maxFileSizeBytes: Long = 4 * 1024 * 1024L,
    val statusExpiryHours: Int = 24
)
