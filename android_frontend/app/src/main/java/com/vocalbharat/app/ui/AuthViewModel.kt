package com.vocalbharat.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vocalbharat.app.data.local.SessionManager
import com.vocalbharat.app.data.model.UserOut
import com.vocalbharat.app.data.remote.TutorApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val userProfile: UserOut? = null,
    val isSendingOtp: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val isOtpSent: Boolean = false,
    val resendCountdown: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)
    private val apiClient = TutorApiClient()

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = sessionManager.getAuthToken() != null,
            userEmail = sessionManager.getUserEmail()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn, userEmail = sessionManager.getUserEmail()) }
            }
        }
    }

    fun requestOtp(serverUrl: String, email: String) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingOtp = true, errorMessage = null, successMessage = null) }
            val result = apiClient.requestOtp(serverUrl, trimmedEmail)
            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isSendingOtp = false,
                        isOtpSent = true,
                        successMessage = res.message,
                        errorMessage = null
                    )
                }
                startCountdown()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSendingOtp = false,
                        errorMessage = err.message ?: "Failed to send OTP. Please try again."
                    )
                }
            }
        }
    }

    fun verifyOtp(serverUrl: String, email: String, otp: String, onLoginSuccess: () -> Unit = {}) {
        val trimmedOtp = otp.trim()
        if (trimmedOtp.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter the complete 6-digit OTP.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingOtp = true, errorMessage = null) }
            val result = apiClient.verifyOtp(serverUrl, email.trim().lowercase(), trimmedOtp)
            result.onSuccess { tokenOut ->
                sessionManager.saveAuthSession(tokenOut.accessToken, tokenOut.email)
                _uiState.update {
                    it.copy(
                        isVerifyingOtp = false,
                        isLoggedIn = true,
                        userEmail = tokenOut.email,
                        isOtpSent = false,
                        successMessage = "Welcome to VocalBharat!",
                        errorMessage = null
                    )
                }
                onLoginSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isVerifyingOtp = false,
                        errorMessage = err.message ?: "Incorrect OTP. Please check and try again."
                    )
                }
            }
        }
    }

    fun loadProfile(serverUrl: String) {
        val token = sessionManager.getAuthToken() ?: return
        viewModelScope.launch {
            val result = apiClient.getMe(serverUrl, token)
            result.onSuccess { user ->
                _uiState.update { it.copy(userProfile = user) }
            }.onFailure {
                if (it.message?.contains("401") == true || it.message?.contains("Unauthorized") == true) {
                    sessionManager.clearSession()
                }
            }
        }
    }

    fun resetOtpState() {
        _uiState.update { it.copy(isOtpSent = false, errorMessage = null, successMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun logout() {
        countdownJob?.cancel()
        sessionManager.clearSession()
        _uiState.update {
            AuthUiState(isLoggedIn = false, userEmail = null)
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 60 downTo 0) {
                _uiState.update { it.copy(resendCountdown = sec) }
                delay(1000)
            }
        }
    }
}
