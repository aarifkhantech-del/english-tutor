package com.vocalbharat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocalbharat.app.data.model.FeedbackRequest
import com.vocalbharat.app.data.model.HelpRequest
import com.vocalbharat.app.data.remote.TutorApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI States ────────────────────────────────────────────────────────────────

data class FeedbackUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String = "",
    val ticketId: String = "",

    // Form fields
    val name: String = "",
    val email: String = "",
    val rating: Int = 0,
    val category: String = "general",
    val message: String = "",
)

data class HelpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String = "",
    val ticketId: String = "",

    // Form fields
    val name: String = "",
    val email: String = "",
    val issueType: String = "payment",
    val subject: String = "",
    val description: String = "",
    val device: String = "android",
)

// ── ViewModel ────────────────────────────────────────────────────────────────

class FeedbackViewModel : ViewModel() {

    private val apiClient = TutorApiClient()

    // Feedback state
    private val _feedbackState = MutableStateFlow(FeedbackUiState())
    val feedbackState: StateFlow<FeedbackUiState> = _feedbackState.asStateFlow()

    // Help state
    private val _helpState = MutableStateFlow(HelpUiState())
    val helpState: StateFlow<HelpUiState> = _helpState.asStateFlow()

    // ── Feedback Field Updates ──────────────────────────────────────────────

    fun onFeedbackNameChange(v: String)    { _feedbackState.value = _feedbackState.value.copy(name = v) }
    fun onFeedbackEmailChange(v: String)   { _feedbackState.value = _feedbackState.value.copy(email = v) }
    fun onFeedbackRatingChange(v: Int)     { _feedbackState.value = _feedbackState.value.copy(rating = v) }
    fun onFeedbackCategoryChange(v: String){ _feedbackState.value = _feedbackState.value.copy(category = v) }
    fun onFeedbackMessageChange(v: String) { _feedbackState.value = _feedbackState.value.copy(message = v) }

    // ── Help Field Updates ──────────────────────────────────────────────────

    fun onHelpNameChange(v: String)       { _helpState.value = _helpState.value.copy(name = v) }
    fun onHelpEmailChange(v: String)      { _helpState.value = _helpState.value.copy(email = v) }
    fun onHelpIssueTypeChange(v: String)  { _helpState.value = _helpState.value.copy(issueType = v) }
    fun onHelpSubjectChange(v: String)    { _helpState.value = _helpState.value.copy(subject = v) }
    fun onHelpDescriptionChange(v: String){ _helpState.value = _helpState.value.copy(description = v) }
    fun onHelpDeviceChange(v: String)     { _helpState.value = _helpState.value.copy(device = v) }

    // ── Submit Feedback ─────────────────────────────────────────────────────

    fun submitFeedback(serverUrl: String) {
        val s = _feedbackState.value
        if (s.name.isBlank() || s.email.isBlank() || s.message.isBlank()) {
            _feedbackState.value = s.copy(errorMessage = "Please fill in all required fields.")
            return
        }
        if (s.rating == 0) {
            _feedbackState.value = s.copy(errorMessage = "Please select a star rating.")
            return
        }

        viewModelScope.launch {
            _feedbackState.value = s.copy(isLoading = true, errorMessage = null)
            val result = apiClient.submitFeedback(
                serverUrl,
                FeedbackRequest(
                    name = s.name.trim(),
                    email = s.email.trim(),
                    rating = s.rating,
                    category = s.category,
                    message = s.message.trim()
                )
            )
            result.fold(
                onSuccess = { res ->
                    _feedbackState.value = _feedbackState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = res.message,
                        ticketId = res.ticketId
                    )
                },
                onFailure = { err ->
                    _feedbackState.value = _feedbackState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Submission failed. Please try again."
                    )
                }
            )
        }
    }

    fun resetFeedback() {
        _feedbackState.value = FeedbackUiState()
    }

    // ── Submit Help ─────────────────────────────────────────────────────────

    fun submitHelp(serverUrl: String) {
        val s = _helpState.value
        if (s.name.isBlank() || s.email.isBlank() || s.subject.isBlank() || s.description.isBlank()) {
            _helpState.value = s.copy(errorMessage = "Please fill in all required fields.")
            return
        }

        viewModelScope.launch {
            _helpState.value = s.copy(isLoading = true, errorMessage = null)
            val result = apiClient.submitHelp(
                serverUrl,
                HelpRequest(
                    name = s.name.trim(),
                    email = s.email.trim(),
                    issueType = s.issueType,
                    subject = s.subject.trim(),
                    description = s.description.trim(),
                    device = s.device
                )
            )
            result.fold(
                onSuccess = { res ->
                    _helpState.value = _helpState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = res.message,
                        ticketId = res.ticketId
                    )
                },
                onFailure = { err ->
                    _helpState.value = _helpState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Help request failed. Please try again."
                    )
                }
            )
        }
    }

    fun resetHelp() {
        _helpState.value = HelpUiState()
    }

    // ── Error Dismissal ─────────────────────────────────────────────────────

    fun clearFeedbackError() { _feedbackState.value = _feedbackState.value.copy(errorMessage = null) }
    fun clearHelpError()     { _helpState.value = _helpState.value.copy(errorMessage = null) }
}
