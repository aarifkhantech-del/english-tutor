package com.vocalbharat.app.data.local

import android.content.Context
import com.vocalbharat.app.data.model.CorrectionResult
import com.vocalbharat.app.data.model.SpeakHistoryEntry
import com.vocalbharat.app.data.model.TutorResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

/**
 * Persists Hindi→English practice history in the app's private files directory.
 * Entries never leave the device (not sent to MongoDB or any backend).
 */
class SpeakHistoryStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<SpeakHistoryEntry>>() {}.type

    fun load(): List<SpeakHistoryEntry> = synchronized(this) {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            if (json.isBlank()) emptyList()
            else gson.fromJson<MutableList<SpeakHistoryEntry>>(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addFromResponse(response: TutorResponse): List<SpeakHistoryEntry> {
        val hindi = response.transcription.ifBlank { response.correction.hindiInput }
        val english = response.correction.englishTranslation.ifBlank { response.correction.corrected }
        if (hindi.isBlank() && english.isBlank()) return load()

        val entry = SpeakHistoryEntry(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            hindi = hindi,
            english = english,
            explanation = response.correction.explanation,
            practice = response.correction.practice,
            encouragement = response.correction.encouragement
        )
        return add(entry)
    }

    fun add(entry: SpeakHistoryEntry): List<SpeakHistoryEntry> = synchronized(this) {
        val next = load().toMutableList()
        next.add(0, entry)
        while (next.size > MAX_ENTRIES) {
            next.removeAt(next.lastIndex)
        }
        persist(next)
        next
    }

    fun delete(id: String): List<SpeakHistoryEntry> = synchronized(this) {
        val next = load().filterNot { it.id == id }
        persist(next)
        next
    }

    fun clear(): List<SpeakHistoryEntry> = synchronized(this) {
        persist(emptyList())
        emptyList()
    }

    private fun persist(entries: List<SpeakHistoryEntry>) {
        file.writeText(gson.toJson(entries))
    }

    companion object {
        private const val FILE_NAME = "hindi_english_speak_history.json"
        private const val MAX_ENTRIES = 100

        @Volatile
        private var instance: SpeakHistoryStore? = null

        fun getInstance(context: Context): SpeakHistoryStore {
            return instance ?: synchronized(this) {
                instance ?: SpeakHistoryStore(context.applicationContext).also { instance = it }
            }
        }
    }
}

fun SpeakHistoryEntry.toCorrection(): CorrectionResult = CorrectionResult(
    hindiInput = hindi,
    englishTranslation = english,
    corrected = english,
    explanation = explanation,
    practice = practice,
    encouragement = encouragement
)
