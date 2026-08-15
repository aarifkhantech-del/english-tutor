package com.englishtutor.app.data.remote

import com.englishtutor.app.data.model.TutorResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class TutorApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Step 1: Transcribe recorded audio to Hindi text
     */
    suspend fun transcribeAudio(baseUrl: String, audioFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val endpoint = "$cleanUrl/transcribe"

                val mediaType = "audio/m4a".toMediaTypeOrNull()
                val requestBody = audioFile.asRequestBody(mediaType)

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.name, requestBody)
                    .build()

                val request = Request.Builder()
                    .url(endpoint)
                    .post(multipartBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        return@withContext Result.failure(
                            Exception("Transcription status ${response.code}: $errorBody")
                        )
                    }

                    val jsonString = response.body?.string() ?: ""
                    val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
                    val text = jsonObject.get("text")?.asString ?: ""
                    Result.success(text)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Step 2: Submit confirmed Hindi text to translate into English & generate audio
     */
    suspend fun submitText(baseUrl: String, text: String): Result<TutorResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val endpoint = "$cleanUrl/tutor/text"

                val jsonPayload = JsonObject().apply {
                    addProperty("text", text)
                }.toString()

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonPayload.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        return@withContext Result.failure(
                            Exception("Translation status ${response.code}: $errorBody")
                        )
                    }

                    val jsonString = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response body from server"))

                    val tutorResponse = gson.fromJson(jsonString, TutorResponse::class.java)
                    Result.success(tutorResponse)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Legacy direct audio submission fallback
     */
    suspend fun submitAudio(baseUrl: String, audioFile: File): Result<TutorResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val endpoint = "$cleanUrl/tutor"

                val mediaType = "audio/m4a".toMediaTypeOrNull()
                val requestBody = audioFile.asRequestBody(mediaType)

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.name, requestBody)
                    .build()

                val request = Request.Builder()
                    .url(endpoint)
                    .post(multipartBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        return@withContext Result.failure(
                            Exception("Server returned status ${response.code}: $errorBody")
                        )
                    }

                    val jsonString = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response body from server"))

                    val tutorResponse = gson.fromJson(jsonString, TutorResponse::class.java)
                    Result.success(tutorResponse)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun checkHealth(baseUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val request = Request.Builder()
                    .url("$cleanUrl/health")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
