package com.englishtutor.app.data.model

import com.google.gson.annotations.SerializedName

data class CorrectionResult(
    @SerializedName("hindi_input")
    val hindiInput: String = "",

    @SerializedName("english_translation")
    val englishTranslation: String = "",

    @SerializedName("corrected")
    val corrected: String = "",

    @SerializedName("explanation")
    val explanation: String = "",

    @SerializedName("practice")
    val practice: String = "",

    @SerializedName("encouragement")
    val encouragement: String = ""
)

data class TutorResponse(
    @SerializedName("transcription")
    val transcription: String = "",

    @SerializedName("correction")
    val correction: CorrectionResult = CorrectionResult(),

    @SerializedName("audio_b64")
    val audioB64: String = ""
)

data class HealthResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("version")
    val version: String = ""
)
