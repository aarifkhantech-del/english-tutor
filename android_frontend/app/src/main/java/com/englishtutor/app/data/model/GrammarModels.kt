package com.englishtutor.app.data.model

import com.google.gson.annotations.SerializedName

data class GrammarRequest(
    @SerializedName("topic") val topic: String
)

data class GrammarExample(
    @SerializedName("sentence") val sentence: String,
    @SerializedName("explanation") val explanation: String
)

data class GrammarResponse(
    @SerializedName("topic") val topic: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("hindi_definition") val hindiDefinition: String,
    @SerializedName("examples") val examples: List<GrammarExample> = emptyList(),
    @SerializedName("tips") val tips: List<String> = emptyList(),
    @SerializedName("difficulty") val difficulty: String = "Beginner"
)
