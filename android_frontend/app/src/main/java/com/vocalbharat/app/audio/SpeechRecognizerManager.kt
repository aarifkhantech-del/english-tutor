package com.englishtutor.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerManager(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRecordingStopped: () -> Unit = {}
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sessionId = 0
    private var startRetries = 0

    var isListening: Boolean = false
        private set

    fun startListening(languageLocale: String = "hi-IN") {
        val newSession = ++sessionId
        startRetries = 0
        releaseRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Android Speech Recognition is not available on this device.")
            return
        }

        // Destroy the previous recognizer on this loop, then start on the next.
        // Calling stopListening() on an already-finished session fires ERROR_CLIENT
        // and made the first tap after a completed request appear to do nothing.
        mainHandler.post {
            if (newSession != sessionId) return@post
            beginListening(languageLocale, newSession)
        }
    }

    private fun beginListening(languageLocale: String, currentSession: Int) {
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                private fun isCurrent(): Boolean =
                    currentSession == sessionId && speechRecognizer === recognizer

                override fun onReadyForSpeech(params: Bundle?) {
                    if (!isCurrent()) return
                    Log.d("SpeechRecognizer", "Ready for speech")
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    if (!isCurrent()) return
                    Log.d("SpeechRecognizer", "Beginning of speech")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    if (!isCurrent()) return
                    Log.d("SpeechRecognizer", "End of speech")
                    isListening = false
                    onRecordingStopped()
                }

                override fun onError(error: Int) {
                    if (!isCurrent()) return
                    isListening = false
                    scheduleRelease(recognizer)

                    // After a completed request the leftover engine often returns
                    // ERROR_CLIENT or ERROR_RECOGNIZER_BUSY on the next start.
                    val shouldRetry = (
                        error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                        ) && startRetries < 1
                    if (shouldRetry) {
                        startRetries++
                        Log.w("SpeechRecognizer", "Retrying listen after error $error")
                        mainHandler.postDelayed({
                            if (currentSession != sessionId) return@postDelayed
                            beginListening(languageLocale, currentSession)
                        }, 150)
                        return
                    }

                    val message = getErrorMessage(error)
                    Log.e("SpeechRecognizer", "Error code $error: $message")
                    onError(message)
                }

                override fun onResults(results: Bundle?) {
                    if (!isCurrent()) return
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    Log.d("SpeechRecognizer", "Final result: $text")
                    onFinalResult(text)
                    scheduleRelease(recognizer)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (!isCurrent()) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        Log.d("SpeechRecognizer", "Partial result: $text")
                        onPartialResult(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.startListening(intent)
            isListening = true
            Log.d("SpeechRecognizer", "Started listening with locale $languageLocale")
        } catch (e: Exception) {
            isListening = false
            Log.e("SpeechRecognizer", "Failed to start speech recognizer", e)
            onError("Speech recognizer error: ${e.message}")
        }
    }

    fun stopListening() {
        val recognizer = speechRecognizer
        if (recognizer != null && isListening) {
            try {
                recognizer.stopListening()
            } catch (e: Exception) {
                Log.e("SpeechRecognizer", "Error stopping speech recognizer", e)
                sessionId++
                releaseRecognizer()
            }
            isListening = false
        } else {
            sessionId++
            releaseRecognizer()
        }
    }

    fun release() {
        sessionId++
        mainHandler.removeCallbacksAndMessages(null)
        releaseRecognizer()
    }

    private fun scheduleRelease(recognizer: SpeechRecognizer) {
        mainHandler.post {
            if (speechRecognizer === recognizer) releaseRecognizer()
        }
    }

    private fun releaseRecognizer() {
        val recognizer = speechRecognizer ?: return
        speechRecognizer = null
        isListening = false
        try {
            recognizer.destroy()
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error destroying speech recognizer", e)
        }
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking louder."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again."
            else -> "Speech recognition error ($error)"
        }
    }
}
