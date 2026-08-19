package com.englishtutor.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(getAuthToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(getUserEmail())
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _requestCount = MutableStateFlow(prefs.getInt(KEY_REQUEST_COUNT, 0))
    val requestCount: StateFlow<Int> = _requestCount.asStateFlow()

    fun saveAuthSession(token: String, email: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        _isLoggedIn.value = true
        _userEmail.value = email
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getLocalRequestCount(): Int {
        return _requestCount.value
    }

    fun incrementLocalRequestCount(): Int {
        return setLocalRequestCount(getLocalRequestCount() + 1)
    }

    fun recordConsumedRequest(serverUsed: Int = 0): Int {
        val next = max(getLocalRequestCount() + 1, serverUsed)
        return setLocalRequestCount(next)
    }

    fun setLocalRequestCount(count: Int): Int {
        val safe = max(0, count)
        prefs.edit().putInt(KEY_REQUEST_COUNT, safe).apply()
        _requestCount.value = safe
        return safe
    }

    fun resetLocalRequestCount() {
        setLocalRequestCount(0)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .apply()
        _isLoggedIn.value = false
        _userEmail.value = null
    }

    companion object {
        private const val PREF_NAME = "vocalbharat_session"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_REQUEST_COUNT = "key_request_count"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
