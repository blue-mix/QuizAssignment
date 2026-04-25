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

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The full renderable state of the Quiz Engine screen.
 *
 * Sealed class wrapping an inner [Active] data class lets us express
 * three distinct lifecycle phases without a nullable field soup.
 *
 * [Loading]   → Questions are being fetched from Room.
 * [Active]    → Quiz is live. All engine state lives here.
 * [Finished]  → Timer hit zero or user submitted. Carries final score.
 * [Error]     → Questions failed to load (e.g., invalid quizId).
 */
sealed class QuizUiState {

    object Loading : QuizUiState()

    data class Error(val message: String) : QuizUiState()

    /**
     * Emitted once time runs out or the user explicitly submits.
     * The NavHost observes this to navigate to the Results screen.
     *
     * @param score  Number of correct answers.
     * @param total  Total question count.
     */
    data class Finished(
        val score: Int,
        val total: Int,
    ) : QuizUiState()

    /**
     * Live state while the quiz is in progress.
     *
     * Every field is derived from immutable sources and is safe to
     * read from any thread — [StateFlow] guarantees visibility.
     *
     * @param currentQuestion   The [Question] currently displayed.
     * @param currentIndex      Zero-based index of the current question.
     * @param totalQuestions    Total number of questions in this quiz.
     * @param selectedOptionIndex The option the user has tapped, or null if none.
     * @param isAnswerRevealed  True after the user locks in an answer;
     *                          triggers correct/incorrect colour feedback.
     * @param score             Running count of correct answers so far.
     * @param timeRemainingSeconds Countdown value driven by the internal ticker.
     * @param totalTimeSeconds  The quiz's full time budget (for progress bar denominator).
     * @param progressFraction  [0f..1f] representing question progress through the quiz.
     * @param timerFraction     [0f..1f] representing remaining time (for the timer arc).
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

/**
 * All user-triggered actions on the Quiz Engine screen.
 *
 * Events flow in one direction: Composable → ViewModel.
 * The ViewModel processes them and updates [QuizUiState] accordingly.
 *
 * [OptionSelected]  → User tapped an MCQ answer option.
 * [NextQuestion]    → User confirmed their answer and wants to advance.
 * [SubmitQuiz]      → User is on the last question and wants to see results.
 */
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
 * ── UDF contract ──────────────────────────────────────────────────────────────
 *   Events in  : [onEvent]  — single entry point for all user interactions.
 *   State out  : [uiState]  — single source of truth for the quiz composable.
 *
 * ── Timer design ──────────────────────────────────────────────────────────────
 * The countdown runs as a coroutine launched on [viewModelScope]. Because
 * [viewModelScope] survives configuration changes (rotation, fold/unfold),
 * the timer continues ticking accurately — no restart, no drift.
 *
 * The timer [Job] is stored explicitly so it can be cancelled cleanly when
 * the user submits early, preventing a ghost tick after [QuizUiState.Finished].
 *
 * ── Zero Android dependencies ─────────────────────────────────────────────────
 * No `android.*`, no `Context`, no `Resources`. The [quizId] is a plain [Long].
 * [GetQuizQuestionsUseCase] is pure Kotlin. All state emitted is pure Kotlin
 * data classes — Lumo UI binds to [uiState] without any platform bridging.
 *
 * ── Configuration-change safety ───────────────────────────────────────────────
 * State is held in [MutableStateFlow] inside the ViewModel — it survives
 * rotation. Questions are fetched once via [Flow.first] and stored in
 * [_questions] — no redundant DB queries on re-composition.
 *
 * @param quizId              The quiz to load, sourced from the navigation route.
 * @param totalTimeInMinutes  The quiz's time budget, sourced from the Quiz domain
 *                            model and passed in at construction from the Detail screen.
 *                            Defaults to 10 minutes if not supplied.
 * @param getQuizQuestions    Use case injected by Koin.
 */
class QuizEngineViewModel(
    private val quizId: Long,
    private val totalTimeInMinutes: Int,
    private val getQuizQuestions: GetQuizQuestionsUseCase,
) : ViewModel() {

    // ── Internal state ────────────────────────────────────────────────────────

    /** Loaded once; never re-fetched. Survives configuration changes in the VM. */
    private var _questions: List<Question> = emptyList()

    /** Mutable backing field. [update] is atomic for concurrent coroutine safety. */
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)

    /** Handle to the countdown coroutine — stored for explicit cancellation. */
    private var timerJob: Job? = null

    // ── Running totals tracked outside of state (avoids recomposition pollution) ─

    private var currentIndex  = 0
    private var score         = 0
    private var timeRemaining = totalTimeInMinutes * SECONDS_PER_MINUTE
    private val totalSeconds  = totalTimeInMinutes * SECONDS_PER_MINUTE

    // ── Public API ────────────────────────────────────────────────────────────

    /** Observed by the Quiz Engine composable. */
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        loadQuestionsAndStartQuiz()
    }

    /**
     * Single entry point for all user interactions.
     * Guards are applied inside each handler — calling [onEvent] with an
     * event that makes no sense for the current state is a silent no-op.
     */
    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.OptionSelected -> handleOptionSelected(event.index)
            QuizEvent.NextQuestion      -> handleNextQuestion()
            QuizEvent.SubmitQuiz        -> handleSubmitQuiz()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Explicit cancellation is technically redundant (viewModelScope is
        // cancelled automatically), but makes the lifecycle contract explicit.
        timerJob?.cancel()
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private fun loadQuestionsAndStartQuiz() {
        viewModelScope.launch {
            runCatching {
                // `.first()` collects exactly one emission from the Room Flow
                // and then cancels the upstream — no ongoing subscription needed
                // for the quiz engine (questions don't change mid-session).
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

    /**
     * Launches a 1-second-resolution countdown on [viewModelScope].
     *
     * Survives configuration changes. Cancels automatically when the ViewModel
     * is cleared. Explicitly cancelled via [timerJob] on early submission.
     */
    private fun startCountdownTimer() {
        timerJob = viewModelScope.launch {
            while (timeRemaining > 0) {
                delay(TIMER_TICK_MS)
                timeRemaining--
                // Only update the timer fraction if the quiz is still active.
                val current = _uiState.value
                if (current is QuizUiState.Active) {
                    _uiState.update {
                        current.copy(
                            timeRemainingSeconds = timeRemaining,
                            timerFraction        = timeRemaining.toFloat() / totalSeconds.toFloat(),
                        )
                    }
                }
            }
            // Time's up — auto-submit with whatever score has been accumulated.
            finaliseQuiz()
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Records the user's selection and reveals the answer feedback.
     * Guard: no-op if the answer has already been locked in for this question.
     */
    private fun handleOptionSelected(index: Int) {
        val current = _uiState.value as? QuizUiState.Active ?: return
        if (current.isAnswerRevealed) return  // prevent double-tap changing answer

        val question = _questions[currentIndex]
        if (question.isCorrect(index)) score++

        _uiState.value = current.copy(
            selectedOptionIndex = index,
            isAnswerRevealed    = true,
            score               = score,
        )
    }

    /**
     * Advances to the next question.
     * Guard: no-op if no answer has been selected yet (forces the user to answer).
     * Guard: no-op on the last question (use [QuizEvent.SubmitQuiz] instead).
     */
    private fun handleNextQuestion() {
        val current = _uiState.value as? QuizUiState.Active ?: return
        if (!current.isAnswerRevealed) return             // must select before advancing
        if (currentIndex >= _questions.lastIndex) return  // use SubmitQuiz on last

        currentIndex++
        emitActiveState()
    }

    /**
     * Ends the quiz and emits [QuizUiState.Finished].
     * Can be triggered by the user on the last question, or automatically
     * by the timer reaching zero.
     * Guard: no-op if state is not [QuizUiState.Active] (prevents double-finish).
     */
    private fun handleSubmitQuiz() {
        if (_uiState.value !is QuizUiState.Active) return
        finaliseQuiz()
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    /**
     * Builds and emits a fresh [QuizUiState.Active] for the current question index.
     * Called on initial load and after every [handleNextQuestion].
     */
    private fun emitActiveState() {
        val question = _questions[currentIndex]
        _uiState.value = QuizUiState.Active(
            currentQuestion      = question,
            currentIndex         = currentIndex,
            totalQuestions       = _questions.size,
            selectedOptionIndex  = null,
            isAnswerRevealed     = false,
            score                = score,
            timeRemainingSeconds = timeRemaining,
            totalTimeSeconds     = totalSeconds,
            // Progress fraction: how far through the question list we are.
            // +1 because the current question counts as "started".
            progressFraction     = (currentIndex + 1).toFloat() / _questions.size.toFloat(),
            timerFraction        = timeRemaining.toFloat() / totalSeconds.toFloat(),
        )
    }

    /**
     * Cancels the timer and transitions to [QuizUiState.Finished].
     * The NavHost observes [Finished] to navigate to the Results screen.
     */
    private fun finaliseQuiz() {
        timerJob?.cancel()
        _uiState.value = QuizUiState.Finished(
            score = score,
            total = _questions.size,
        )
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TIMER_TICK_MS      = 1_000L
        private const val SECONDS_PER_MINUTE = 60
    }
}