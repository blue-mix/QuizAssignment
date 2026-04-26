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

class QuizEngineViewModel(
    private val quizId: Long,
    private val totalTimeInMinutes: Int,
    private val getQuizQuestions: GetQuizQuestionsUseCase,
) : ViewModel() {

    private var _questions: List<Question> = emptyList()

    /** Backing field. All writes go through `update { }` for atomicity. */
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)

    private var timerJob: Job? = null
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

        val isLastQuestion = currentIndex >= _questions.lastIndex
        if (!isLastQuestion) {
            advanceJob?.cancel()  // safety: cancel any orphaned advance from a previous question
            advanceJob = viewModelScope.launch {

                delay(REVEAL_DISPLAY_MS)
                handleNextQuestion()
            }
        }
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
        private const val REVEAL_DISPLAY_MS  = 1_000L
    }
}