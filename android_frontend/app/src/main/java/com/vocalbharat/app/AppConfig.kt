package com.vocalbharat.app

object AppConfig {
    const val PRODUCTION_API_URL = "https://english-tutor-6fx2.onrender.com"
    const val PRIVACY_POLICY_URL = "$PRODUCTION_API_URL/privacy"

    val apiBaseUrl: String
        get() = BuildConfig.API_BASE_URL

    val allowServerOverride: Boolean
        get() = BuildConfig.DEBUG
}
