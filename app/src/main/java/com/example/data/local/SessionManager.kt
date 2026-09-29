package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AccountSession(
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val token: String = "",
    val isAdmin: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("chatpro_accounts_prefs", Context.MODE_PRIVATE)

    private val _currentAccount = MutableStateFlow<AccountSession?>(null)
    val currentAccount: StateFlow<AccountSession?> = _currentAccount.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountSession>>(emptyList())
    val accounts: StateFlow<List<AccountSession>> = _accounts.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        val jsonString = prefs.getString(KEY_ACCOUNTS, "[]") ?: "[]"
        val activeUsername = prefs.getString(KEY_ACTIVE_USERNAME, "") ?: ""
        val list = mutableListOf<AccountSession>()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AccountSession(
                        username = obj.optString("username"),
                        displayName = obj.optString("displayName"),
                        avatarUrl = obj.optString("avatarUrl"),
                        token = obj.optString("token"),
                        isAdmin = obj.optBoolean("isAdmin", false),
                        lastActive = obj.optLong("lastActive", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        _accounts.value = list
        val selected = list.find { it.username.equals(activeUsername, ignoreCase = true) } ?: list.firstOrNull()
        _currentAccount.value = selected
    }

    fun saveAccount(account: AccountSession) {
        val currentList = _accounts.value.toMutableList()
        // If already exists, replace it
        val index = currentList.indexOfFirst { it.username.equals(account.username, ignoreCase = true) }
        if (index >= 0) {
            currentList[index] = account
        } else {
            if (currentList.size >= MAX_ACCOUNTS) {
                currentList.removeAt(0) // Keep max 3 accounts like Telegram
            }
            currentList.add(account)
        }

        persistAccounts(currentList, account.username)
    }

    fun switchAccount(username: String) {
        val account = _accounts.value.find { it.username.equals(username, ignoreCase = true) }
        if (account != null) {
            prefs.edit().putString(KEY_ACTIVE_USERNAME, username).apply()
            _currentAccount.value = account
        }
    }

    fun removeAccount(username: String) {
        val updated = _accounts.value.filterNot { it.username.equals(username, ignoreCase = true) }
        val newActive = updated.firstOrNull()?.username ?: ""
        persistAccounts(updated, newActive)
    }

    private fun persistAccounts(list: List<AccountSession>, activeUser: String) {
        val jsonArray = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("username", it.username)
            obj.put("displayName", it.displayName)
            obj.put("avatarUrl", it.avatarUrl)
            obj.put("token", it.token)
            obj.put("isAdmin", it.isAdmin)
            obj.put("lastActive", it.lastActive)
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_ACCOUNTS, jsonArray.toString())
            .putString(KEY_ACTIVE_USERNAME, activeUser)
            .apply()

        _accounts.value = list
        _currentAccount.value = list.find { it.username.equals(activeUser, ignoreCase = true) }
    }

    companion object {
        private const val KEY_ACCOUNTS = "encrypted_accounts_list"
        private const val KEY_ACTIVE_USERNAME = "active_account_username"
        const val MAX_ACCOUNTS = 3
    }
}
