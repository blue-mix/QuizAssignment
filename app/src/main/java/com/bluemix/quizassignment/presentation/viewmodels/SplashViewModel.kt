package com.bluemix.quizassignment.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.quizassignment.data.local.database.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class SplashUiState {
    object Initialising   : SplashUiState()
    object NavigateToHome : SplashUiState()
    data class Error(val message: String) : SplashUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class SplashViewModel : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Initialising)

    /** Observed by the Splash composable to drive navigation and animation. */
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        initialiseSplash()
    }

    private fun initialiseSplash() {
        viewModelScope.launch {
            runCatching {

                val splashTimerDeferred = async {
                    // Minimum brand animation display time.
                    delay(SPLASH_DURATION_MS)
                }

                val dbPreWarmDeferred = async {
                    withContext(Dispatchers.IO) {
                        val db = get<AppDatabase>()   // triggers build() on IO thread
                        db.quizDao()                  // warms the DAO singleton cache
                    }
                }

                // Suspend until BOTH the splash timer AND the pre-warm finish.
                splashTimerDeferred.await()
                dbPreWarmDeferred.await()

            }.onSuccess {
                _uiState.value = SplashUiState.NavigateToHome
            }.onFailure { throwable ->
                _uiState.value = SplashUiState.Error(
                    message = throwable.message ?: "Failed to initialise the database."
                )
            }
        }
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1_500L
    }
}