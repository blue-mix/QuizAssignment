package com.bluemix.quizassignment.presentation.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.domain.usecase.GetQuizByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

/**
 * All renderable states for the Quiz Detail screen.
 *
 * [Loading] → shown while the use case fetches the quiz from Room.
 * [Success] → the full [Quiz] domain model is available to display.
 * [Error]   → quiz not found or DB error; surface a message and back-nav.
 */
sealed class QuizDetailUiState {
    object Loading                      : QuizDetailUiState()
    data class Success(val quiz: Quiz)  : QuizDetailUiState()
    data class Error(val message: String) : QuizDetailUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel for the Quiz Detail screen.
 *
 * ── Responsibility ────────────────────────────────────────────────────────────
 * The navigation route carries only a primitive [quizId]. This ViewModel
 * bridges that thin route argument to the full [Quiz] domain model that
 * [QuizDetailScreen] needs to render its hero section and metadata.
 *
 * ── Zero Android dependencies ─────────────────────────────────────────────────
 * No `Context`, no `Resources`, no Android imports beyond [ViewModel].
 * The Lumo UI screen composable can consume [uiState] without adaptation.
 *
 * @param quizId         The quiz primary key, extracted from [Screen.QuizDetail]
 *                       and passed via Koin [parametersOf] in [ViewModelModule].
 * @param getQuizById    Use case injected by Koin — resolves from [domainModule].
 */
class QuizDetailViewModel(
    private val quizId: Long,
    private val getQuizById: GetQuizByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizDetailUiState>(QuizDetailUiState.Loading)

    /** Observed by [QuizDetailScreen] to render quiz metadata. */
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    init {
        loadQuiz()
    }

    // ── Private logic ─────────────────────────────────────────────────────────

    private fun loadQuiz() {
        viewModelScope.launch {
            runCatching { getQuizById(quizId) }
                .onSuccess { quiz ->
                    _uiState.value = if (quiz != null) {
                        QuizDetailUiState.Success(quiz)
                    } else {
                        QuizDetailUiState.Error("Quiz not found. It may have been removed.")
                    }
                }
                .onFailure { throwable ->
                    _uiState.value = QuizDetailUiState.Error(
                        throwable.message ?: "Failed to load quiz details."
                    )
                }
        }
    }
}