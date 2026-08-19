package com.vocalbharat.app.data.model

import com.google.gson.annotations.SerializedName

data class FeedbackRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("rating") val rating: Int,
    @SerializedName("category") val category: String,
    @SerializedName("message") val message: String,
    @SerializedName("page_source") val pageSource: String = "android"
)

data class HelpRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("issue_type") val issueType: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("description") val description: String,
    @SerializedName("device") val device: String = "android",
    @SerializedName("page_source") val pageSource: String = "android"
)

data class FeedbackResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("ticket_id") val ticketId: String
)
