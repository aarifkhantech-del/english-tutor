package com.englishtutor.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishtutor.app.audio.AudioPlayerManager
import com.englishtutor.app.audio.SpeechRecognizerManager
import com.englishtutor.app.data.local.SessionManager
import com.englishtutor.app.data.local.SpeakHistoryStore
import com.englishtutor.app.data.model.SpeakHistoryEntry
import com.englishtutor.app.data.model.TutorResponse
import com.englishtutor.app.data.remote.TutorApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TutorUiState(
    val isRecording: Boolean = false,
    val isTranslating: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val isServerOnline: Boolean = true,
    val serverUrl: String = "http://192.168.1.8:8000",
    val pendingTranscription: String? = null,
    val result: TutorResponse? = null,
    val speakHistory: List<SpeakHistoryEntry> = emptyList(),
    val errorMessage: String? = null,
    val showSettingsDialog: Boolean = false
) {
    val isProcessing: Boolean get() = isTranslating
}

class TutorViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = AudioPlayerManager(application)
    private val apiClient = TutorApiClient()

    private val speechRecognizerManager = SpeechRecognizerManager(
        context = application,
        onPartialResult = { partialText ->
            _uiState.update { it.copy(pendingTranscription = partialText) }
        },
        onFinalResult = { finalText ->
            val textToUse = finalText.ifBlank { "नमस्ते, मैं अंग्रेजी सीखना चाहता हूँ" }
            _uiState.update {
                it.copy(
                    isRecording = false,
                    pendingTranscription = textToUse
                )
            }
        },
        onError = { errorMsg ->
            // On timeout or no match, stop recording cleanly
            _uiState.update {
                it.copy(
                    isRecording = false,
                    errorMessage = if (errorMsg.contains("No speech")) errorMsg else null
                )
            }
        },
        onRecordingStopped = {
            // No automatic processing after recording stops; user decides to proceed or cancel.
            _uiState.update { it.copy(isRecording = false) }
        }
    )

    private val sessionManager = SessionManager.getInstance(application)
    private val speakHistoryStore = SpeakHistoryStore.getInstance(application)
    private val prefs = application.getSharedPreferences("tutor_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        TutorUiState(
            serverUrl = prefs.getString("server_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        )
    )
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { speakHistoryStore.load() }
            _uiState.update { it.copy(speakHistory = history) }
        }
        checkServerHealth()
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            val healthy = apiClient.checkHealth(_uiState.value.serverUrl)
            _uiState.update { it.copy(isServerOnline = healthy) }
        }
    }

    /**
     * Toggles 0ms instant native Android Speech Recognizer
     */
    fun toggleRecording() {
        _uiState.update { it.copy(errorMessage = null) }

        if (_uiState.value.isRecording || speechRecognizerManager.isListening) {
            speechRecognizerManager.stopListening()
            _uiState.update { it.copy(isRecording = false) }
        } else {
            // Free the mic if pronunciation audio is still playing from the last request.
            playerManager.stop()
            _uiState.update {
                it.copy(
                    isRecording = true,
                    isPlayingAudio = false,
                    pendingTranscription = null,
                    result = null
                )
            }
            speechRecognizerManager.startListening(languageLocale = "hi-IN")
        }
    }

    /**
     * User tapped PROCEED -> Convert confirmed Hindi text to English
     */
    fun proceedToTranslate(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, errorMessage = null) }

            val result = apiClient.submitText(
                _uiState.value.serverUrl,
                text,
                sessionManager.getAuthToken()
            )

            result.fold(
                onSuccess = { response ->
                    sessionManager.recordConsumedRequest(response.requestsUsed)
                    val history = withContext(Dispatchers.IO) {
                        speakHistoryStore.addFromResponse(response)
                    }
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            pendingTranscription = null,
                            result = response,
                            speakHistory = history
                        )
                    }

                    // Auto-play pronunciation audio in memory
                    if (response.audioB64.isNotBlank()) {
                        playAudioPronunciation(response.audioB64)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            errorMessage = error.message ?: "Failed to reach AI Tutor backend"
                        )
                    }
                }
            )
        }
    }

    /**
     * User tapped CANCEL -> Discard speech request
     */
    fun cancelSpeakRequest() {
        speechRecognizerManager.stopListening()
        playerManager.stop()
        _uiState.update {
            it.copy(
                isRecording = false,
                isTranslating = false,
                isPlayingAudio = false,
                pendingTranscription = null,
                result = null,
                errorMessage = null
            )
        }
    }

    fun togglePlayAudio() {
        val currentResponse = _uiState.value.result ?: return
        val audioB64 = currentResponse.audioB64

        if (audioB64.isBlank()) return

        if (_uiState.value.isPlayingAudio) {
            playerManager.stop()
            _uiState.update { it.copy(isPlayingAudio = false) }
        } else {
            playAudioPronunciation(audioB64)
        }
    }

    private fun playAudioPronunciation(audioB64: String) {
        _uiState.update { it.copy(isPlayingAudio = true) }
        playerManager.playBase64Audio(audioB64) {
            _uiState.update { it.copy(isPlayingAudio = false) }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun deleteSpeakHistoryEntry(id: String) {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { speakHistoryStore.delete(id) }
            _uiState.update { it.copy(speakHistory = history) }
        }
    }

    fun clearSpeakHistory() {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { speakHistoryStore.clear() }
            _uiState.update { it.copy(speakHistory = history) }
        }
    }

    fun saveServerUrl(newUrl: String) {
        val cleanUrl = newUrl.trim().trimEnd('/')
        prefs.edit().putString("server_url", cleanUrl).apply()
        _uiState.update { it.copy(serverUrl = cleanUrl, showSettingsDialog = false) }
        checkServerHealth()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.release()
        playerManager.release()
    }
}
