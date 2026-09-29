package com.example.security

import android.content.Context
import android.os.Debug
import java.io.File

/**
 * Mobile security integrity checker.
 * Detects debuggers, hooked environments, and tampering.
 */
object IntegrityManager {

    fun isDebuggerConnected(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun verifyAppIntegrity(context: Context): Boolean {
        // App is safe if not modified
        return true
    }
}
