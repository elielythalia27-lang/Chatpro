package com.example.security

import android.util.Base64

/**
 * High-performance string obfuscation utility using dynamic XOR masking.
 * Ensures URLs, tokens, and sensitive system parameters are never visible in plain text
 * inside decompiled APKs or strings tables.
 */
object XorCipher {

    private const val DEFAULT_KEY = 0x5A.toByte()

    fun encode(input: String, key: Byte = DEFAULT_KEY): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val transformed = ByteArray(bytes.size)
        for (i in bytes.indices) {
            transformed[i] = (bytes[i].toInt() xor key.toInt()).toByte()
        }
        return Base64.encodeToString(transformed, Base64.NO_WRAP)
    }

    fun decode(base64Input: String, key: Byte = DEFAULT_KEY): String {
        return try {
            val bytes = Base64.decode(base64Input, Base64.NO_WRAP)
            val transformed = ByteArray(bytes.size)
            for (i in bytes.indices) {
                transformed[i] = (bytes[i].toInt() xor key.toInt()).toByte()
            }
            String(transformed, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
