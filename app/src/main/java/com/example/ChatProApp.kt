package com.example

import android.app.Application
import com.example.data.local.ChatProDatabase
import com.example.data.local.SessionManager
import com.example.data.network.CloudStorage
import com.example.data.network.SyncService
import com.example.security.IntegrityManager

class ChatProApp : Application() {

    lateinit var database: ChatProDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var cloudStorage: CloudStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ChatProDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        cloudStorage = CloudStorage(this)

        // Verify device environment integrity
        IntegrityManager.verifyAppIntegrity(this)

        // Start real-time foreground sync service if an account is currently active
        if (sessionManager.currentAccount.value != null) {
            SyncService.start(this)
        }
    }

    companion object {
        lateinit var instance: ChatProApp
            private set
    }
}
