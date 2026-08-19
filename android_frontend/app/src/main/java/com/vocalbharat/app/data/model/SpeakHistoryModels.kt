package com.vocalbharat.app.data.model

data class SpeakHistoryEntry(
    val id: String = "",
    val createdAt: Long = 0L,
    val hindi: String = "",
    val english: String = "",
    val explanation: String = "",
    val practice: String = "",
    val encouragement: String = ""
)
