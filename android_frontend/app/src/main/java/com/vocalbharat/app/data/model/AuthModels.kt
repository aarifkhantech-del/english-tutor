package com.vocalbharat.app.data.model

import com.google.gson.annotations.SerializedName

data class OTPRequestIn(
    val email: String
)

data class OTPRequestOut(
    val message: String,
    val email: String
)

data class OTPVerifyIn(
    val email: String,
    val otp: String
)

data class GoogleSignInIn(
    @SerializedName("id_token") val idToken: String
)

data class TokenOut(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val email: String,
    @SerializedName("is_new_user") val isNewUser: Boolean = false
)

data class UserOut(
    val id: String,
    val email: String,
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    @SerializedName("full_name") val fullName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String = "",
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("last_login_at") val lastLoginAt: String? = null
)
