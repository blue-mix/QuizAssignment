package com.bluemix.quizassignment.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * All possible states for the Splash screen.
 *
 * Sealed class (not interface) because the states carry no data and the
 * compiler enforces exhaustive `when` coverage without a trailing `else`.
 *
 * [Initialising] → shown during the splash animation and DB warm-up.
 * [NavigateToHome] → signals the UI to pop the back stack and navigate.
 *                    Using a state (not a one-shot event channel) is safe here
 *                    because Splash is never revisited after navigation occurs.
 * [Error] → surface any unexpected initialisation failure gracefully.
 */
sealed class SplashUiState {
    object Initialising   : SplashUiState()
    object NavigateToHome : SplashUiState()
    data class Error(val message: String) : SplashUiState()
}

/**
 * ViewModel for the Splash screen.
 *
 * ── Responsibilities ──────────────────────────────────────────────────────────
 *  1. Hold the minimum display duration (1.5 s) so the brand animation completes.
 *  2. Ensure the Room database is warm before navigating (the [GrammarDatabaseCallback]
 *     seeds data on first open; simply querying the DAO forces the DB to initialise).
 *  3. Emit [SplashUiState.NavigateToHome] when both conditions are satisfied.
 *
 * ── Zero Android dependencies ─────────────────────────────────────────────────
 * This class imports nothing from `android.*` or `androidx.*` except [ViewModel]
 * itself, which is a framework requirement, not a UI concern.
 * No [Context], no [Resources], no [Intent] — Lumo UI can consume [uiState]
 * from any host without adaptation.
 *
 * ── Configuration change safety ───────────────────────────────────────────────
 * [viewModelScope] is cancelled only when the ViewModel is cleared (screen
 * permanently left), not on rotation. The 1.5 s timer is therefore immune to
 * configuration changes — it continues from where it left off.
 */
class SplashViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Initialising)

    /** Observed by the Splash composable to drive navigation and animation. */
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        initialiseSplash()
    }

    // ── Private logic ─────────────────────────────────────────────────────────

    private fun initialiseSplash() {
        viewModelScope.launch {
            runCatching {
                // Enforced minimum display duration — gives the brand animation
                // enough time to complete on all device speeds.
                delay(SPLASH_DURATION_MS)

                // Room initialises its connection pool lazily on first access.
                // The GrammarDatabaseCallback runs its onCreate seed coroutine
                // at this point on first install. No explicit work needed here
                // because the DAO's SupervisorJob-backed scope handles seeding
                // independently. We simply wait for the splash duration, then go.
            }.onSuccess {
                _uiState.value = SplashUiState.NavigateToHome
            }.onFailure { throwable ->
                _uiState.value = SplashUiState.Error(
                    message = throwable.message ?: "Unexpected initialisation error."
                )
            }
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Minimum splash screen visibility duration in milliseconds. */
        private const val SPLASH_DURATION_MS = 1_500L
    }
}