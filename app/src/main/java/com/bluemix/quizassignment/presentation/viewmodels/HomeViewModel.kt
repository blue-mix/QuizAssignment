package com.bluemix.quizassignment.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.domain.usecase.GetAvailableQuizzesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents every renderable state of the Home dashboard.
 *
 * ── State modelling philosophy ────────────────────────────────────────────────
 * Using a sealed class produces a closed, exhaustive type hierarchy. The Lumo
 * UI composable uses a `when(state)` with no `else` branch — the compiler
 * guarantees every new state variant is handled before the code compiles.
 *
 * [Loading]  → Skeleton / shimmer shown while the DB query is in-flight.
 * [Success]  → Non-empty list ready to render.
 * [Empty]    → DB returned zero quizzes (edge case on first install before seed completes).
 * [Error]    → Failure with a user-facing message and retry affordance.
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty   : HomeUiState()
    data class Success(val quizzes: List<Quiz>) : HomeUiState()
    data class Error(val message: String)       : HomeUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Events
// ─────────────────────────────────────────────────────────────────────────────

/**
 * All user-triggered actions on the Home screen.
 *
 * Sealed class events flow in one direction: UI → ViewModel.
 * The ViewModel never calls back into the UI — it only updates [HomeUiState].
 *
 * [Refresh]  → Pull-to-refresh or explicit retry after an error.
 */
sealed class HomeEvent {
    object Refresh : HomeEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel for the Home dashboard screen.
 *
 * ── UDF contract ──────────────────────────────────────────────────────────────
 *   Events in  : [onEvent] — the single entry point for all UI interactions.
 *   State out  : [uiState] — the single source of truth the composable renders.
 *
 * ── Zero Android dependencies ─────────────────────────────────────────────────
 * Imports nothing from `android.*`. [GetAvailableQuizzesUseCase] is a pure
 * Kotlin class. [Quiz] is a pure domain data class.
 * Lumo UI components can bind to [uiState] without any platform adaptation.
 *
 * @param getAvailableQuizzes The use case injected by Koin.
 */
class HomeViewModel(
    private val getAvailableQuizzes: GetAvailableQuizzesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

    /** Observed by the Home composable to drive the quiz list UI. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadQuizzes()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Single entry point for all UI events.
     * Follows the UDF pattern — the composable never calls private functions.
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> loadQuizzes()
        }
    }

    // ── Private logic ─────────────────────────────────────────────────────────

    private fun loadQuizzes() {
        // Avoid redundant loads if data is already present.
        // Reset to Loading only when explicitly refreshing.
        if (_uiState.value !is HomeUiState.Success) {
            _uiState.value = HomeUiState.Loading
        }

        viewModelScope.launch {
            runCatching { getAvailableQuizzes() }
                .onSuccess { quizzes ->
                    _uiState.value = if (quizzes.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(quizzes)
                    }
                }
                .onFailure { throwable ->
                    _uiState.value = HomeUiState.Error(
                        message = throwable.message ?: "Failed to load quizzes. Please try again."
                    )
                }
        }
    }
}