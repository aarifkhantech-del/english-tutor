package com.englishtutor.app.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishtutor.app.data.local.SessionManager
import com.englishtutor.app.data.model.GrammarResponse
import com.englishtutor.app.data.remote.TutorApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GrammarUiState(
    val topic: String = "",
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val result: GrammarResponse? = null,
    val errorMessage: String? = null
)

class GrammarViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GrammarUiState())
    val uiState: StateFlow<GrammarUiState> = _uiState.asStateFlow()

    private val apiClient = TutorApiClient()
    private val sessionManager = SessionManager.getInstance(application)
    private var speechRecognizer: SpeechRecognizer? = null

    fun setTopic(topic: String) {
        _uiState.update { it.copy(topic = topic, errorMessage = null) }
    }

    fun explainTopic(serverUrl: String) {
        val topic = _uiState.value.topic.trim()
        if (topic.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a grammar topic first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, result = null) }
            val result = apiClient.explainGrammar(serverUrl, topic, sessionManager.getAuthToken())
            result.fold(
                onSuccess = { response ->
                    _uiState.update { it.copy(isLoading = false, result = response) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed: ${error.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun startVoiceRecognition() {
        val context = getApplication<Application>()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.update { it.copy(isRecording = true, errorMessage = null) }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull() ?: ""
                _uiState.update { it.copy(isRecording = false, topic = spoken) }
            }
            override fun onError(error: Int) {
                _uiState.update { it.copy(isRecording = false, errorMessage = "Voice error. Please try again or type.") }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _uiState.update { it.copy(isRecording = false) } }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (partial.isNotEmpty()) _uiState.update { it.copy(topic = partial) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isRecording = false) }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null, topic = "", errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
