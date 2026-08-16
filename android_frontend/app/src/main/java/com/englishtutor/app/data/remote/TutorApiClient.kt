package com.englishtutor.app.data.remote

import com.englishtutor.app.data.model.GrammarResponse
import com.englishtutor.app.data.model.OTPRequestIn
import com.englishtutor.app.data.model.OTPRequestOut
import com.englishtutor.app.data.model.OTPVerifyIn
import com.englishtutor.app.data.model.TokenOut
import com.englishtutor.app.data.model.UserOut
import com.englishtutor.app.data.model.PlansOut
import com.englishtutor.app.data.model.InitiatePaymentIn
import com.englishtutor.app.data.model.InitiatePaymentOut
import com.englishtutor.app.data.model.ConfirmPaymentIn
import com.englishtutor.app.data.model.SubscriptionStatusOut
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

    /**
     * Grammar Explainer — POST /grammar/explain
     */
    suspend fun explainGrammar(baseUrl: String, topic: String): Result<GrammarResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val endpoint = "$cleanUrl/grammar/explain"

                val jsonPayload = JsonObject().apply {
                    addProperty("topic", topic)
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
                            Exception("Grammar API error ${response.code}: $errorBody")
                        )
                    }
                    val jsonString = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty grammar response"))
                    val grammarResponse = gson.fromJson(jsonString, GrammarResponse::class.java)
                    Result.success(grammarResponse)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ── Auth & OTP Methods ───────────────────────────────────────────────────

    suspend fun requestOtp(baseUrl: String, email: String): Result<OTPRequestOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val payload = gson.toJson(OTPRequestIn(email = email))
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = payload.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$cleanUrl/auth/request-otp")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "Failed to send OTP (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, OTPRequestOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun verifyOtp(baseUrl: String, email: String, otp: String): Result<TokenOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val payload = gson.toJson(OTPVerifyIn(email = email, otp = otp))
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = payload.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$cleanUrl/auth/verify-otp")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "OTP verification failed (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, TokenOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMe(baseUrl: String, token: String): Result<UserOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val request = Request.Builder()
                    .url("$cleanUrl/auth/me")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "Failed to fetch user profile (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, UserOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ── Subscription & Payment Methods ───────────────────────────────────────

    suspend fun getPlans(baseUrl: String): Result<PlansOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val request = Request.Builder()
                    .url("$cleanUrl/subscription/plans")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "Failed to fetch plans (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, PlansOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun initiatePayment(baseUrl: String, token: String, plan: String): Result<InitiatePaymentOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val payload = gson.toJson(InitiatePaymentIn(plan = plan))
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = payload.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$cleanUrl/subscription/initiate")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "Failed to initiate payment (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, InitiatePaymentOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun confirmPayment(
        baseUrl: String,
        token: String,
        orderId: String,
        paymentId: String,
        signature: String? = null
    ): Result<SubscriptionStatusOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val payload = gson.toJson(ConfirmPaymentIn(orderId = orderId, paymentId = paymentId, signature = signature))
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = payload.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$cleanUrl/subscription/confirm")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "Payment confirmation failed (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, SubscriptionStatusOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getSubscriptionStatus(baseUrl: String, token: String): Result<SubscriptionStatusOut> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val request = Request.Builder()
                    .url("$cleanUrl/subscription/status")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception(parseErrorMessage(jsonString, "Failed to fetch subscription status (${response.code})"))
                        )
                    }
                    val out = gson.fromJson(jsonString, SubscriptionStatusOut::class.java)
                    Result.success(out)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseErrorMessage(jsonString: String, defaultMsg: String): String {
        return try {
            val json = gson.fromJson(jsonString, JsonObject::class.java)
            json.get("detail")?.asString ?: json.get("message")?.asString ?: defaultMsg
        } catch (_: Exception) {
            defaultMsg
        }
    }

    /**
     * Submit user feedback (rating + message) to the owner's email.
     */
    suspend fun submitFeedback(
        baseUrl: String,
        payload: com.englishtutor.app.data.model.FeedbackRequest
    ): Result<com.englishtutor.app.data.model.FeedbackResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val endpoint = "$cleanUrl/feedback/submit"
                val jsonBody = gson.toJson(payload)
                    .toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder().url(endpoint).post(jsonBody).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val result = gson.fromJson(body, com.englishtutor.app.data.model.FeedbackResponse::class.java)
                        Result.success(result)
                    } else {
                        Result.failure(Exception(parseErrorMessage(body, "Feedback submission failed")))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Submit a help/support request to the owner's email.
     */
    suspend fun submitHelp(
        baseUrl: String,
        payload: com.englishtutor.app.data.model.HelpRequest
    ): Result<com.englishtutor.app.data.model.FeedbackResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                val endpoint = "$cleanUrl/feedback/help"
                val jsonBody = gson.toJson(payload)
                    .toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder().url(endpoint).post(jsonBody).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val result = gson.fromJson(body, com.englishtutor.app.data.model.FeedbackResponse::class.java)
                        Result.success(result)
                    } else {
                        Result.failure(Exception(parseErrorMessage(body, "Help request failed")))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}



