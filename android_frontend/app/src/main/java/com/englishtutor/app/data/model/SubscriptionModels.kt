package com.englishtutor.app.data.model

import com.google.gson.annotations.SerializedName

data class PlanInfo(
    val id: String, // "trial" or "monthly"
    val name: String,
    val description: String,
    val amount: Int,
    val currency: String,
    @SerializedName("duration_days") val durationDays: Int,
    val badge: String? = null
)

data class PlansOut(
    val plans: List<PlanInfo> = emptyList()
)

data class InitiatePaymentIn(
    val plan: String
)

data class InitiatePaymentOut(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("subscription_id") val subscriptionId: String,
    val gateway: String,
    val amount: Int,
    val currency: String,
    @SerializedName("gateway_key") val gatewayKey: String? = null
)

data class ConfirmPaymentIn(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("payment_id") val paymentId: String,
    val signature: String? = null
)

data class SubscriptionStatusOut(
    @SerializedName("has_subscription") val hasSubscription: Boolean = false,
    val plan: String? = null,
    val status: String? = null,
    @SerializedName("is_active") val isActive: Boolean = false,
    @SerializedName("starts_at") val startsAt: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("days_remaining") val daysRemaining: Int = 0,
    @SerializedName("requests_used") val requestsUsed: Int = 0,
    @SerializedName("requests_limit") val requestsLimit: Int = 8,
    @SerializedName("requests_remaining") val requestsRemaining: Int = 8,
    @SerializedName("quota_exceeded") val quotaExceeded: Boolean = false
)
