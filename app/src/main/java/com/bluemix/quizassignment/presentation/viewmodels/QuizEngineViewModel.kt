package com.bluemix.quizassignment.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.quizassignment.domain.model.Question
import com.bluemix.quizassignment.domain.usecase.GetQuizQuestionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
//
//// ─────────────────────────────────────────────────────────────────────────────
//// UI State
//// ─────────────────────────────────────────────────────────────────────────────
//
///**
// * The full renderable state of the Quiz Engine screen.
// *
// * Sealed class wrapping an inner [Active] data class lets us express
// * three distinct lifecycle phases without a nullable field soup.
// *
// * [Loading]   → Questions are being fetched from Room.
// * [Active]    → Quiz is live. All engine state lives here.
// * [Finished]  → Timer hit zero or user submitted. Carries final score.
// * [Error]     → Questions failed to load (e.g., invalid quizId).
// */
//sealed class QuizUiState {
//
//    object Loading : QuizUiState()
//
//    data class Error(val message: String) : QuizUiState()
//
//    /**
//     * Emitted once time runs out or the user explicitly submits.
//     * The NavHost observes this to navigate to the Results screen.
//     *
//     * @param score  Number of correct answers.
//     * @param total  Total question count.
//     */
//    data class Finished(
//        val score: Int,
//        val total: Int,
//    ) : QuizUiState()
//
//    /**
//     * Live state while the quiz is in progress.
//     *
//     * Every field is derived from immutable sources and is safe to
//     * read from any thread — [StateFlow] guarantees visibility.
//     *
//     * @param currentQuestion   The [Question] currently displayed.
//     * @param currentIndex      Zero-based index of the current question.
//     * @param totalQuestions    Total number of questions in this quiz.
//     * @param selectedOptionIndex The option the user has tapped, or null if none.
//     * @param isAnswerRevealed  True after the user locks in an answer;
//     *                          triggers correct/incorrect colour feedback.
//     * @param score             Running count of correct answers so far.
//     * @param timeRemainingSeconds Countdown value driven by the internal ticker.
//     * @param totalTimeSeconds  The quiz's full time budget (for progress bar denominator).
//     * @param progressFraction  [0f..1f] representing question progress through the quiz.
//     * @param timerFraction     [0f..1f] representing remaining time (for the timer arc).
//     */
//    data class Active(
//        val currentQuestion: Question,
//        val currentIndex: Int,
//        val totalQuestions: Int,
//        val selectedOptionIndex: Int?,
//        val isAnswerRevealed: Boolean,
//        val score: Int,
//        val timeRemainingSeconds: Int,
//        val totalTimeSeconds: Int,
//        val progressFraction: Float,
//        val timerFraction: Float,
//    ) : QuizUiState()
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
//// Events
//// ─────────────────────────────────────────────────────────────────────────────
//
///**
// * All user-triggered actions on the Quiz Engine screen.
// *
// * Events flow in one direction: Composable → ViewModel.
// * The ViewModel processes them and updates [QuizUiState] accordingly.
// *
// * [OptionSelected]  → User tapped an MCQ answer option.
// * [NextQuestion]    → User confirmed their answer and wants to advance.
// * [SubmitQuiz]      → User is on the last question and wants to see results.
// */
//sealed class QuizEvent {
//    data class OptionSelected(val index: Int) : QuizEvent()
//    object NextQuestion : QuizEvent()
//    object SubmitQuiz   : QuizEvent()
//}
//
//// ─────────────────────────────────────────────────────────────────────────────
//// ViewModel
//// ─────────────────────────────────────────────────────────────────────────────
//
///**
// * Core engine ViewModel for the live quiz experience.
// *
// * ── UDF contract ──────────────────────────────────────────────────────────────
// *   Events in  : [onEvent]  — single entry point for all user interactions.
// *   State out  : [uiState]  — single source of truth for the quiz composable.
// *
// * ── Timer design ──────────────────────────────────────────────────────────────
// * The countdown runs as a coroutine launched on [viewModelScope]. Because
// * [viewModelScope] survives configuration changes (rotation, fold/unfold),
// * the timer continues ticking accurately — no restart, no drift.
// *
// * The timer [Job] is stored explicitly so it can be cancelled cleanly when
// * the user submits early, preventing a ghost tick after [QuizUiState.Finished].
// *
// * ── Zero Android dependencies ─────────────────────────────────────────────────
// * No `android.*`, no `Context`, no `Resources`. The [quizId] is a plain [Long].
// * [GetQuizQuestionsUseCase] is pure Kotlin. All state emitted is pure Kotlin
// * data classes — Lumo UI binds to [uiState] without any platform bridging.
// *
// * ── Configuration-change safety ───────────────────────────────────────────────
// * State is held in [MutableStateFlow] inside the ViewModel — it survives
// * rotation. Questions are fetched once via [Flow.first] and stored in
// * [_questions] — no redundant DB queries on re-composition.
// *
// * @param quizId              The quiz to load, sourced from the navigation route.
// * @param totalTimeInMinutes  The quiz's time budget, sourced from the Quiz domain
// *                            model and passed in at construction from the Detail screen.
// *                            Defaults to 10 minutes if not supplied.
// * @param getQuizQuestions    Use case injected by Koin.
// */
//class QuizEngineViewModel(
//    private val quizId: Long,
//    private val totalTimeInMinutes: Int,
//    private val getQuizQuestions: GetQuizQuestionsUseCase,
//) : ViewModel() {
//
//    // ── Internal state ────────────────────────────────────────────────────────
//
//    /** Loaded once; never re-fetched. Survives configuration changes in the VM. */
//    private var _questions: List<Question> = emptyList()
//
//    /** Mutable backing field. [update] is atomic for concurrent coroutine safety. */
//    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
//
//    /** Handle to the countdown coroutine — stored for explicit cancellation. */
//    private var timerJob: Job? = null
//
//    // ── Running totals tracked outside of state (avoids recomposition pollution) ─
//
//    private var currentIndex  = 0
//    private var score         = 0
//    private var timeRemaining = totalTimeInMinutes * SECONDS_PER_MINUTE
//    private val totalSeconds  = totalTimeInMinutes * SECONDS_PER_MINUTE
//
//    // ── Public API ────────────────────────────────────────────────────────────
//
//    /** Observed by the Quiz Engine composable. */
//    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()
//
//    init {
//        loadQuestionsAndStartQuiz()
//    }
//
//    /**
//     * Single entry point for all user interactions.
//     * Guards are applied inside each handler — calling [onEvent] with an
//     * event that makes no sense for the current state is a silent no-op.
//     */
//    fun onEvent(event: QuizEvent) {
//        when (event) {
//            is QuizEvent.OptionSelected -> handleOptionSelected(event.index)
//            QuizEvent.NextQuestion      -> handleNextQuestion()
//            QuizEvent.SubmitQuiz        -> handleSubmitQuiz()
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        // Explicit cancellation is technically redundant (viewModelScope is
//        // cancelled automatically), but makes the lifecycle contract explicit.
//        timerJob?.cancel()
//    }
//
//    // ── Initialisation ────────────────────────────────────────────────────────
//
//    private fun loadQuestionsAndStartQuiz() {
//        viewModelScope.launch {
//            runCatching {
//                // `.first()` collects exactly one emission from the Room Flow
//                // and then cancels the upstream — no ongoing subscription needed
//                // for the quiz engine (questions don't change mid-session).
//                getQuizQuestions(quizId).first()
//            }.onSuccess { questions ->
//                if (questions.isEmpty()) {
//                    _uiState.value = QuizUiState.Error(
//                        "No questions found for this quiz. Please try another."
//                    )
//                    return@onSuccess
//                }
//                _questions = questions
//                emitActiveState()
//                startCountdownTimer()
//            }.onFailure { throwable ->
//                _uiState.value = QuizUiState.Error(
//                    throwable.message ?: "Failed to load questions. Please try again."
//                )
//            }
//        }
//    }
//
//    // ── Timer ─────────────────────────────────────────────────────────────────
//
//    /**
//     * Launches a 1-second-resolution countdown on [viewModelScope].
//     *
//     * Survives configuration changes. Cancels automatically when the ViewModel
//     * is cleared. Explicitly cancelled via [timerJob] on early submission.
//     */
//    private fun startCountdownTimer() {
//        timerJob = viewModelScope.launch {
//            while (timeRemaining > 0) {
//                delay(TIMER_TICK_MS)
//                timeRemaining--
//                // Only update the timer fraction if the quiz is still active.
//                val current = _uiState.value
//                if (current is QuizUiState.Active) {
//                    _uiState.update {
//                        current.copy(
//                            timeRemainingSeconds = timeRemaining,
//                            timerFraction        = timeRemaining.toFloat() / totalSeconds.toFloat(),
//                        )
//                    }
//                }
//            }
//            // Time's up — auto-submit with whatever score has been accumulated.
//            finaliseQuiz()
//        }
//    }
//
//    // ── Event handlers ────────────────────────────────────────────────────────
//
//    /**
//     * Records the user's selection and reveals the answer feedback.
//     * Guard: no-op if the answer has already been locked in for this question.
//     */
//    private fun handleOptionSelected(index: Int) {
//        val current = _uiState.value as? QuizUiState.Active ?: return
//        if (current.isAnswerRevealed) return  // prevent double-tap changing answer
//
//        val question = _questions[currentIndex]
//        if (question.isCorrect(index)) score++
//
//        _uiState.value = current.copy(
//            selectedOptionIndex = index,
//            isAnswerRevealed    = true,
//            score               = score,
//        )
//    }
//
//    /**
//     * Advances to the next question.
//     * Guard: no-op if no answer has been selected yet (forces the user to answer).
//     * Guard: no-op on the last question (use [QuizEvent.SubmitQuiz] instead).
//     */
//    private fun handleNextQuestion() {
//        val current = _uiState.value as? QuizUiState.Active ?: return
//        if (!current.isAnswerRevealed) return             // must select before advancing
//        if (currentIndex >= _questions.lastIndex) return  // use SubmitQuiz on last
//
//        currentIndex++
//        emitActiveState()
//    }
//
//    /**
//     * Ends the quiz and emits [QuizUiState.Finished].
//     * Can be triggered by the user on the last question, or automatically
//     * by the timer reaching zero.
//     * Guard: no-op if state is not [QuizUiState.Active] (prevents double-finish).
//     */
//    private fun handleSubmitQuiz() {
//        if (_uiState.value !is QuizUiState.Active) return
//        finaliseQuiz()
//    }
//
//    // ── State helpers ─────────────────────────────────────────────────────────
//
//    /**
//     * Builds and emits a fresh [QuizUiState.Active] for the current question index.
//     * Called on initial load and after every [handleNextQuestion].
//     */
//    private fun emitActiveState() {
//        val question = _questions[currentIndex]
//        _uiState.value = QuizUiState.Active(
//            currentQuestion      = question,
//            currentIndex         = currentIndex,
//            totalQuestions       = _questions.size,
//            selectedOptionIndex  = null,
//            isAnswerRevealed     = false,
//            score                = score,
//            timeRemainingSeconds = timeRemaining,
//            totalTimeSeconds     = totalSeconds,
//            // Progress fraction: how far through the question list we are.
//            // +1 because the current question counts as "started".
//            progressFraction     = (currentIndex + 1).toFloat() / _questions.size.toFloat(),
//            timerFraction        = timeRemaining.toFloat() / totalSeconds.toFloat(),
//        )
//    }
//
//    /**
//     * Cancels the timer and transitions to [QuizUiState.Finished].
//     * The NavHost observes [Finished] to navigate to the Results screen.
//     */
//    private fun finaliseQuiz() {
//        timerJob?.cancel()
//        _uiState.value = QuizUiState.Finished(
//            score = score,
//            total = _questions.size,
//        )
//    }
//
//    // ── Constants ─────────────────────────────────────────────────────────────
//
//    companion object {
//        private const val TIMER_TICK_MS      = 1_000L
//        private const val SECONDS_PER_MINUTE = 60
//    }
//}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class QuizUiState {

    object Loading : QuizUiState()

    data class Error(val message: String) : QuizUiState()

    data class Finished(
        val score: Int,
        val total: Int,
    ) : QuizUiState()

    /**
     * Live state while the quiz is in progress.
     *
     * @param currentQuestion    The [Question] currently displayed.
     * @param currentIndex       Zero-based index of the current question.
     * @param totalQuestions     Total number of questions in this quiz.
     * @param selectedOptionIndex The option the user tapped, or null if none.
     * @param isAnswerRevealed   True after the user locks in an answer.
     * @param score              Running count of correct answers.
     * @param timeRemainingSeconds Countdown value driven by the internal ticker.
     * @param totalTimeSeconds   The quiz's full time budget.
     * @param progressFraction   [0f..1f] question progress through the quiz.
     * @param timerFraction      [0f..1f] remaining time fraction.
     */
    data class Active(
        val currentQuestion: Question,
        val currentIndex: Int,
        val totalQuestions: Int,
        val selectedOptionIndex: Int?,
        val isAnswerRevealed: Boolean,
        val score: Int,
        val timeRemainingSeconds: Int,
        val totalTimeSeconds: Int,
        val progressFraction: Float,
        val timerFraction: Float,
    ) : QuizUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Events
// ─────────────────────────────────────────────────────────────────────────────

sealed class QuizEvent {
    data class OptionSelected(val index: Int) : QuizEvent()
    object NextQuestion : QuizEvent()
    object SubmitQuiz   : QuizEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Core engine ViewModel for the live quiz experience.
 *
 * ── FIX 2B: Atomic state updates + ViewModel-owned advance delay ──────────────
 *
 * TWO PROBLEMS in the original code that each contributed to the flicker:
 *
 * Problem 1 — Non-atomic state mutation in handleOptionSelected:
 *   The original used `_uiState.value = current.copy(...)` which is a
 *   non-atomic read-modify-write. Under concurrent coroutine access (the timer
 *   coroutine ticks on the same scope), another write between the `value` read
 *   and the assignment could be silently lost. `_uiState.update { ... }` is
 *   the correct atomic primitive — it retries if another writer changed the
 *   state between the lambda capture and the CAS write.
 *
 * Problem 2 — Auto-advance delay lived in the UI layer (QuizActiveState):
 *   The original `LaunchedEffect(state.selectedOptionIndex)` in
 *   QuizEngineScreen ran a `delay(1_200L)` then dispatched `NextQuestion`.
 *   This created a race between:
 *     (a) the LaunchedEffect coroutine calling onEvent(NextQuestion) after 1.2 s
 *     (b) the timer coroutine writing new time values to the state every 1 s
 *   Because the LaunchedEffect key was `state.selectedOptionIndex` (an Int?),
 *   any timer tick that caused a recomposition could re-key the effect with the
 *   SAME value — doing nothing — but an effect that was already mid-delay could
 *   still fire after recomposition produced a slightly different object identity.
 *   The real problem: the UI layer had no way to emit the new question's state
 *   atomically *at the moment* the delay ended. It sent an event, the ViewModel
 *   processed it, and there was a window between "reveal color shown" and
 *   "new question emitted" where the composable received a state where
 *   isAnswerRevealed was still true but currentQuestion had already changed —
 *   causing the new question's options to briefly pick up the old reveal colors.
 *
 * FIX — move the advance delay into the ViewModel:
 *   `handleOptionSelected` now launches a coroutine that:
 *     1. Writes the reveal state atomically via `_uiState.update { }`.
 *     2. Delays for REVEAL_DISPLAY_MS (1 000 ms) — the user sees the result.
 *     3. Calls `handleNextQuestion()` inside the same coroutine, which calls
 *        `emitActiveState()` — a single atomic write that sets the new question
 *        with selectedOptionIndex = null and isAnswerRevealed = false.
 *
 *   There is now ZERO window between "hide reveal" and "show new question".
 *   The state goes from:
 *     Active(isAnswerRevealed=true,  currentQuestion=Q1)
 *     → (1 000 ms delay)
 *     → Active(isAnswerRevealed=false, currentQuestion=Q2)   ← single atomic write
 *
 *   The UI composable never sees a state where isAnswerRevealed=true AND
 *   currentQuestion=Q2 simultaneously. The key(question.id) guard in the UI
 *   layer (Fix 2A) destroys the animation nodes at the same moment this write
 *   is delivered, so the new OptionCards start from neutral with fresh clocks.
 *
 * Why `_uiState.update` instead of `_uiState.value =` throughout:
 *   `update` uses a Compare-And-Swap loop. If the timer coroutine writes a
 *   new time value between the lambda read and the write in `update`, the
 *   lambda is retried with the latest value. `_uiState.value =` would silently
 *   overwrite the timer's write — causing time values to skip or freeze.
 */
class QuizEngineViewModel(
    private val quizId: Long,
    private val totalTimeInMinutes: Int,
    private val getQuizQuestions: GetQuizQuestionsUseCase,
) : ViewModel() {

    private var _questions: List<Question> = emptyList()

    /** Backing field. All writes go through `update { }` for atomicity. */
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)

    private var timerJob: Job? = null

    // ── FIX 2B: advanceJob tracks the reveal-then-advance coroutine ───────────
    //
    // Stored separately from timerJob so it can be cancelled independently
    // if the user submits early (e.g., on the last question) while a
    // previous auto-advance delay is still running.
    private var advanceJob: Job? = null

    private var currentIndex  = 0
    private var score         = 0
    private var timeRemaining = totalTimeInMinutes * SECONDS_PER_MINUTE
    private val totalSeconds  = totalTimeInMinutes * SECONDS_PER_MINUTE

    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        loadQuestionsAndStartQuiz()
    }

    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.OptionSelected -> handleOptionSelected(event.index)
            QuizEvent.NextQuestion      -> handleNextQuestion()
            QuizEvent.SubmitQuiz        -> handleSubmitQuiz()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        advanceJob?.cancel()
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private fun loadQuestionsAndStartQuiz() {
        viewModelScope.launch {
            runCatching {
                getQuizQuestions(quizId).first()
            }.onSuccess { questions ->
                if (questions.isEmpty()) {
                    _uiState.value = QuizUiState.Error(
                        "No questions found for this quiz. Please try another."
                    )
                    return@onSuccess
                }
                _questions = questions
                emitActiveState()
                startCountdownTimer()
            }.onFailure { throwable ->
                _uiState.value = QuizUiState.Error(
                    throwable.message ?: "Failed to load questions. Please try again."
                )
            }
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startCountdownTimer() {
        timerJob = viewModelScope.launch {
            while (timeRemaining > 0) {
                delay(TIMER_TICK_MS)
                timeRemaining--

                // ── FIX 2B: _uiState.update instead of _uiState.value = ───────
                //
                // `update` is a CAS loop. If handleOptionSelected's advanceJob
                // writes a reveal state between the `when` check and the write
                // below, `update` retries with the latest value instead of
                // blindly overwriting it. This prevents the timer from clobbering
                // a freshly written Active state with a stale copy.
                _uiState.update { current ->
                    if (current is QuizUiState.Active) {
                        current.copy(
                            timeRemainingSeconds = timeRemaining,
                            timerFraction        = timeRemaining.toFloat() / totalSeconds.toFloat(),
                        )
                    } else {
                        current  // do not modify Finished/Error states
                    }
                }
            }
            finaliseQuiz()
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Records the user's selection, reveals the answer, then — after a fixed
     * display period — atomically advances to the next question.
     *
     * ── FIX 2B: atomic reveal + delayed advance (moved from UI layer) ─────────
     *
     * The reveal write and the next-question write are both performed inside
     * the same `advanceJob` coroutine. The timeline is:
     *
     *   t=0 ms   → _uiState.update { reveal }     (options light up green/red)
     *   t=1000ms → handleNextQuestion()            → emitActiveState()
     *                                                (single write: new question,
     *                                                 selectedOptionIndex=null,
     *                                                 isAnswerRevealed=false)
     *
     * There is no intermediate state where isAnswerRevealed=true AND the new
     * question is showing. The key(question.id) in the UI layer destroys the
     * animation nodes at the exact frame this write is delivered.
     */
    private fun handleOptionSelected(index: Int) {
        val current = _uiState.value as? QuizUiState.Active ?: return
        if (current.isAnswerRevealed) return  // guard: no double-tap

        val question = _questions[currentIndex]
        if (question.isCorrect(index)) score++

        // ── Step 1: Write reveal state atomically ─────────────────────────────
        _uiState.update { latest ->
            if (latest is QuizUiState.Active) {
                latest.copy(
                    selectedOptionIndex = index,
                    isAnswerRevealed    = true,
                    score               = score,
                )
            } else latest
        }

        // ── Step 2: Schedule auto-advance (not on last question) ──────────────
        val isLastQuestion = currentIndex >= _questions.lastIndex
        if (!isLastQuestion) {
            advanceJob?.cancel()  // safety: cancel any orphaned advance from a previous question
            advanceJob = viewModelScope.launch {
                // Display the result for REVEAL_DISPLAY_MS so the user can see
                // the correct/incorrect feedback before the question changes.
                delay(REVEAL_DISPLAY_MS)

                // Step 3: Advance to next question.
                // emitActiveState() writes a single fresh Active with
                // selectedOptionIndex=null and isAnswerRevealed=false — there is
                // no window where both the old reveal and the new question coexist.
                handleNextQuestion()
            }
        }
        // If isLastQuestion == true, the UI renders the "See Results" button.
        // The user triggers SubmitQuiz explicitly — no auto-advance.
    }

    /**
     * Advances to the next question with a single atomic state write.
     * Guards are applied to prevent advancing beyond the last question or
     * advancing before an answer has been selected.
     */
    private fun handleNextQuestion() {
        // Guard: must have answered before advancing
        val current = _uiState.value as? QuizUiState.Active ?: return
        if (!current.isAnswerRevealed) return
        if (currentIndex >= _questions.lastIndex) return

        currentIndex++
        emitActiveState()  // single atomic write — new question, clean slate
    }

    private fun handleSubmitQuiz() {
        if (_uiState.value !is QuizUiState.Active) return
        advanceJob?.cancel()  // cancel any pending auto-advance
        finaliseQuiz()
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    /**
     * Emits a clean [QuizUiState.Active] for [currentIndex].
     *
     * selectedOptionIndex = null and isAnswerRevealed = false are always set
     * here — this is the single authoritative "reset to neutral" write that
     * the key(question.id) in the UI layer pairs with to destroy stale
     * animation nodes at the correct moment.
     */
    private fun emitActiveState() {
        val question = _questions[currentIndex]
        // Direct assignment is safe here because this is the only writer
        // of a brand-new Active state. The timer's update{} lambda will
        // see this as the latest value on its next CAS iteration.
        _uiState.value = QuizUiState.Active(
            currentQuestion      = question,
            currentIndex         = currentIndex,
            totalQuestions       = _questions.size,
            selectedOptionIndex  = null,          // ← clean slate: no selection
            isAnswerRevealed     = false,         // ← clean slate: no reveal
            score                = score,
            timeRemainingSeconds = timeRemaining,
            totalTimeSeconds     = totalSeconds,
            progressFraction     = (currentIndex + 1).toFloat() / _questions.size.toFloat(),
            timerFraction        = timeRemaining.toFloat() / totalSeconds.toFloat(),
        )
    }

    private fun finaliseQuiz() {
        timerJob?.cancel()
        advanceJob?.cancel()
        _uiState.value = QuizUiState.Finished(score = score, total = _questions.size)
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TIMER_TICK_MS      = 1_000L
        private const val SECONDS_PER_MINUTE = 60

        // ── FIX 2B: REVEAL_DISPLAY_MS replaces AUTO_ADVANCE_DELAY_MS ─────────
        //
        // Renamed from AUTO_ADVANCE_DELAY_MS (which lived in the Screen file)
        // to make it clear this constant belongs to the ViewModel, not the UI.
        // Value kept at 1 000 ms — long enough for the user to register the
        // correct/incorrect feedback, short enough to maintain quiz pace.
        private const val REVEAL_DISPLAY_MS  = 1_000L
    }
}