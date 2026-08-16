package com.englishtutor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishtutor.app.data.local.SessionManager
import com.englishtutor.app.data.model.InitiatePaymentOut
import com.englishtutor.app.data.model.PlanInfo
import com.englishtutor.app.data.model.SubscriptionStatusOut
import com.englishtutor.app.data.remote.TutorApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val plans: List<PlanInfo> = emptyList(),
    val status: SubscriptionStatusOut = SubscriptionStatusOut(),
    val selectedPlanId: String? = "monthly",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)
    private val apiClient = TutorApiClient()

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun selectPlan(planId: String) {
        _uiState.update { it.copy(selectedPlanId = planId) }
    }

    fun loadPlansAndStatus(serverUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Fetch available plans
            val plansResult = apiClient.getPlans(serverUrl)
            plansResult.onSuccess { plansOut ->
                _uiState.update { it.copy(plans = plansOut.plans) }
            }.onFailure { err ->
                // Fallback default plans if offline
                _uiState.update {
                    it.copy(
                        plans = listOf(
                            PlanInfo("monthly", "Monthly Pro Plan", "Unlimited access to all AI coaching and grammar lessons.", 300, "INR", 30, "Recommended")
                        )
                    )
                }
            }

            // 2. Fetch user's subscription status if logged in
            val token = sessionManager.getAuthToken()
            if (token != null) {
                val statusResult = apiClient.getSubscriptionStatus(serverUrl, token)
                statusResult.onSuccess { subStatus ->
                    _uiState.update { it.copy(status = subStatus) }
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun subscribeToPlan(
        serverUrl: String,
        planId: String,
        onRequireLogin: () -> Unit,
        onLaunchRazorpay: (InitiatePaymentOut) -> Unit,
        onPaymentSuccess: () -> Unit = {}
    ) {
        val token = sessionManager.getAuthToken()
        if (token == null) {
            onRequireLogin()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = true, errorMessage = null, successMessage = null) }

            // 1. Initiate payment order
            val initiateResult = apiClient.initiatePayment(serverUrl, token, planId)
            initiateResult.onSuccess { orderOut ->
                if (orderOut.gateway == "mock") {
                    // Simulated mock payment: complete instantly
                    val confirmResult = apiClient.confirmPayment(
                        baseUrl = serverUrl,
                        token = token,
                        orderId = orderOut.orderId,
                        paymentId = "mock_pay_android_${System.currentTimeMillis()}"
                    )
                    confirmResult.onSuccess { newStatus ->
                        _uiState.update {
                            it.copy(
                                isProcessingPayment = false,
                                status = newStatus,
                                successMessage = "🎉 Monthly subscription activated successfully! Enjoy unlimited spoken English practice."
                            )
                        }
                        onPaymentSuccess()
                    }.onFailure { err ->
                        _uiState.update {
                            it.copy(
                                isProcessingPayment = false,
                                errorMessage = "Confirmation failed: ${err.message}"
                            )
                        }
                    }
                } else {
                    // For Razorpay / other real gateways: trigger Checkout sheet
                    _uiState.update { it.copy(isProcessingPayment = false) }
                    onLaunchRazorpay(orderOut)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessingPayment = false,
                        errorMessage = err.message ?: "Failed to initiate payment."
                    )
                }
            }
        }
    }

    fun confirmCompletedPayment(
        serverUrl: String,
        orderId: String,
        paymentId: String,
        signature: String?,
        onSuccess: () -> Unit = {}
    ) {
        val token = sessionManager.getAuthToken()
        if (token == null) {
            _uiState.update { it.copy(errorMessage = "User session expired. Please sign in again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = true, errorMessage = null, successMessage = null) }
            val confirmResult = apiClient.confirmPayment(
                baseUrl = serverUrl,
                token = token,
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            )
            confirmResult.onSuccess { newStatus ->
                _uiState.update {
                    it.copy(
                        isProcessingPayment = false,
                        status = newStatus,
                        successMessage = "🎉 Payment verified & Monthly Pro activated! Enjoy unlimited AI practice."
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessingPayment = false,
                        errorMessage = "Payment verification failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun onPaymentFailed(reason: String) {
        _uiState.update {
            it.copy(
                isProcessingPayment = false,
                errorMessage = "Payment was cancelled or failed: $reason"
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
