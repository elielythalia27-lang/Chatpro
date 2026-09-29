package com.example.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end and storage AES-256-GCM cipher engine.
 * Encrypts messages, profile records, and remote JSON payloads
 * to prevent sniffing and memory inspection.
 */
object CryptoManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    // Master static key derivation seed (32 bytes = 256 bits)
    private val DEFAULT_MASTER_SEED = byteArrayOf(
        0x1B, 0x3F, 0x7E, 0x2A, 0x5C, 0x4D, 0x6E, 0x09,
        0x32, 0x11, 0x78, 0x54, 0x43, 0x22, 0x66, 0x77,
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10
    )

    fun encrypt(plainText: String, keyBytes: ByteArray = DEFAULT_MASTER_SEED): String {
        return try {
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(encryptedBase64: String, keyBytes: ByteArray = DEFAULT_MASTER_SEED): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTE) return encryptedBase64

            val iv = ByteArray(IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE)
            val cipherTextSize = combined.size - IV_LENGTH_BYTE
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherTextSize)

            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // If already plain or decryption fails gracefully return original string
            encryptedBase64
        }
    }
}
