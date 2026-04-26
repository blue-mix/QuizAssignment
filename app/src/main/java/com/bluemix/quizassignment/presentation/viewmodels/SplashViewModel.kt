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

//
///**
// * All possible states for the Splash screen.
// *
// * Sealed class (not interface) because the states carry no data and the
// * compiler enforces exhaustive `when` coverage without a trailing `else`.
// *
// * [Initialising] → shown during the splash animation and DB warm-up.
// * [NavigateToHome] → signals the UI to pop the back stack and navigate.
// *                    Using a state (not a one-shot event channel) is safe here
// *                    because Splash is never revisited after navigation occurs.
// * [Error] → surface any unexpected initialisation failure gracefully.
// */
//sealed class SplashUiState {
//    object Initialising   : SplashUiState()
//    object NavigateToHome : SplashUiState()
//    data class Error(val message: String) : SplashUiState()
//}
//
///**
// * ViewModel for the Splash screen.
// *
// * ── Responsibilities ──────────────────────────────────────────────────────────
// *  1. Hold the minimum display duration (1.5 s) so the brand animation completes.
// *  2. Ensure the Room database is warm before navigating (the [GrammarDatabaseCallback]
// *     seeds data on first open; simply querying the DAO forces the DB to initialise).
// *  3. Emit [SplashUiState.NavigateToHome] when both conditions are satisfied.
// *
// * ── Zero Android dependencies ─────────────────────────────────────────────────
// * This class imports nothing from `android.*` or `androidx.*` except [ViewModel]
// * itself, which is a framework requirement, not a UI concern.
// * No [Context], no [Resources], no [Intent] — Lumo UI can consume [uiState]
// * from any host without adaptation.
// *
// * ── Configuration change safety ───────────────────────────────────────────────
// * [viewModelScope] is cancelled only when the ViewModel is cleared (screen
// * permanently left), not on rotation. The 1.5 s timer is therefore immune to
// * configuration changes — it continues from where it left off.
// */
//class SplashViewModel : ViewModel() {
//
//    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Initialising)
//
//    /** Observed by the Splash composable to drive navigation and animation. */
//    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
//
//    init {
//        initialiseSplash()
//    }
//
//    // ── Private logic ─────────────────────────────────────────────────────────
//
//    private fun initialiseSplash() {
//        viewModelScope.launch {
//            runCatching {
//                // Enforced minimum display duration — gives the brand animation
//                // enough time to complete on all device speeds.
//                delay(SPLASH_DURATION_MS)
//
//                // Room initialises its connection pool lazily on first access.
//                // The GrammarDatabaseCallback runs its onCreate seed coroutine
//                // at this point on first install. No explicit work needed here
//                // because the DAO's SupervisorJob-backed scope handles seeding
//                // independently. We simply wait for the splash duration, then go.
//            }.onSuccess {
//                _uiState.value = SplashUiState.NavigateToHome
//            }.onFailure { throwable ->
//                _uiState.value = SplashUiState.Error(
//                    message = throwable.message ?: "Unexpected initialisation error."
//                )
//            }
//        }
//    }
//
//    // ── Constants ─────────────────────────────────────────────────────────────
//
//    companion object {
//        /** Minimum splash screen visibility duration in milliseconds. */
//        private const val SPLASH_DURATION_MS = 1_500L
//    }
//}



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

/**
 * ViewModel for the Splash screen.
 *
 * ── Responsibilities ──────────────────────────────────────────────────────────
 *  1. Display the splash screen for at least [SPLASH_DURATION_MS].
 *  2. PRE-WARM the Room [AppDatabase] on [Dispatchers.IO] so that
 *     `HomeViewModel`'s Koin resolution of `AppDatabase` is free.
 *  3. Both tasks run concurrently via [async] — the splash window is not
 *     extended by the pre-warm time if it completes within the 1.5 s window.
 *
 * ── The Pre-Warming Pattern ───────────────────────────────────────────────────
 *
 * Problem:
 *   Koin `single` blocks are lazy — the factory lambda runs the first time
 *   `get<AppDatabase>()` is called anywhere in the app. Without pre-warming,
 *   this first call happens during `HomeViewModel` Koin resolution, which is
 *   triggered on the MAIN THREAD when the Home destination mounts.
 *
 *   `Room.databaseBuilder().build()` (the factory body) does 2–8 ms of CPU
 *   work (reflection to find `AppDatabase_Impl`, config struct construction).
 *   This stalls the main thread, potentially causing a dropped frame and being
 *   caught by `StrictMode.detectDiskReads()` in debug builds.
 *
 * Solution:
 *   During the mandatory splash window, explicitly call `get<AppDatabase>()`
 *   inside `withContext(Dispatchers.IO)`. This:
 *
 *     a) Runs `Room.databaseBuilder().build()` on an IO thread — the main
 *        thread never sees the reflection cost.
 *     b) Stores the result in Koin's singleton cache.
 *     c) By the time `HomeViewModel` calls `get<AppDatabase>()` on the main
 *        thread, it finds the ALREADY-BUILT singleton — a pointer lookup,
 *        effectively 0 ms.
 *
 * Timeline (after fix):
 *
 *   MAIN THREAD: [Splash UI animation............][Navigate to Home]
 *   IO THREAD:   [build() 7ms][DAO warm-up][idle].
 *                              ↑
 *                           Both tasks start concurrently at t=0 inside
 *                           the splash window (1500ms). The 7ms build is
 *                           invisible to the main thread.
 *
 * ── KoinComponent for programmatic `get<>()` ─────────────────────────────────
 *
 * ViewModels cannot use `koinViewModel()` (that's for composables). They use
 * constructor injection instead. But for the pre-warm we need to trigger the
 * Koin singleton imperatively. `KoinComponent` provides `get<AppDatabase>()`
 * without tying the ViewModel to a composable context.
 *
 * The alternative — injecting `AppDatabase` directly into the constructor —
 * would move the `build()` call back to the main thread (whichever thread Koin
 * uses to construct this ViewModel), defeating the purpose.
 *
 * ── Zero Android dependencies (preserved) ────────────────────────────────────
 * No `Context`, `Resources`, or `Intent` references. `KoinComponent` is a
 * Koin interface, not an Android framework class. Lumo UI can still consume
 * [uiState] without platform adaptation.
 */
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
                // ── Run both tasks concurrently with async ────────────────────
                //
                // async{} creates a Deferred that runs immediately. Both tasks
                // start at the same moment:
                //   - splashTimerDeferred: counts 1500ms on the coroutine clock
                //   - dbPreWarmDeferred:   runs build() + quizDao() on IO thread
                //
                // `awaitAll` waits for BOTH to complete. If build() finishes in
                // 7ms and the splash timer needs 1500ms, we wait for 1500ms total
                // — the pre-warm is effectively free from the user's perspective.
                //
                // If the device is extremely slow and build() somehow takes longer
                // than 1500ms (very unlikely), the splash waits for it — we never
                // navigate before the DB is ready.

                val splashTimerDeferred = async {
                    // Minimum brand animation display time.
                    delay(SPLASH_DURATION_MS)
                }

                val dbPreWarmDeferred = async {
                    // ── PERFORMANCE FIX: pre-warm AppDatabase on Dispatchers.IO ─
                    //
                    // `withContext(IO)` ensures the Koin singleton factory
                    // (Room.databaseBuilder().build()) executes on a background
                    // thread. After this call completes, the singleton is cached
                    // in Koin — all subsequent `get<AppDatabase>()` calls on any
                    // thread return the cached instance with zero cost.
                    //
                    // We also call `.quizDao()` here to warm the DAO singleton
                    // (registered separately in DatabaseModule). This is a single
                    // object allocation (<0.1ms) but it means the DAO is also
                    // cached before HomeViewModel needs it.
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