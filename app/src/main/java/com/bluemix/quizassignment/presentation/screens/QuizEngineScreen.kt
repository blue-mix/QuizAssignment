package com.bluemix.quizassignment.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluemix.quizassignment.domain.model.Question
import com.bluemix.quizassignment.presentation.viewmodels.QuizEngineViewModel
import com.bluemix.quizassignment.presentation.viewmodels.QuizEvent
import com.bluemix.quizassignment.presentation.viewmodels.QuizUiState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text
import ui.components.card.CardDefaults
import ui.components.card.OutlinedCard
import ui.components.progressindicators.CircularProgressIndicator
import ui.components.progressindicators.LinearProgressIndicator

// Auto-advance delay after answer reveal (ms)
private const val AUTO_ADVANCE_DELAY_MS = 1_200L

/**
 * Entry-point composable for the Quiz Engine screen.
 *
 * @param quizId             Sourced from the type-safe navigation route.
 * @param totalTimeInMinutes Sourced from the quiz domain model; passed
 *                           to [QuizEngineViewModel] via Koin [parametersOf].
 * @param onQuizFinished     Called when [QuizUiState.Finished] is emitted.
 *                           The NavHost navigates to [Screen.Results].
 */
@Composable
fun QuizEngineScreen(
    quizId: Long,
    totalTimeInMinutes: Int,
    onQuizFinished: (score: Int, total: Int) -> Unit,
    viewModel: QuizEngineViewModel = koinViewModel(
        parameters = { parametersOf(quizId, totalTimeInMinutes) }
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe Finished state once and navigate
    LaunchedEffect(state) {
        if (state is QuizUiState.Finished) {
            val finished = state as QuizUiState.Finished
            onQuizFinished(finished.score, finished.total)
        }
    }

    QuizEngineContent(
        state   = state,
        onEvent = viewModel::onEvent,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless Content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizEngineContent(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        when (state) {
            QuizUiState.Loading   -> QuizLoadingState()
            is QuizUiState.Error  -> QuizErrorState(state.message)
            is QuizUiState.Active -> QuizActiveState(state = state, onEvent = onEvent)
            // Finished → handled via LaunchedEffect navigation; show nothing here
            is QuizUiState.Finished -> QuizLoadingState()
        }
    }
}

// ── Active quiz state ─────────────────────────────────────────────────────────

@Composable
private fun QuizActiveState(
    state: QuizUiState.Active,
    onEvent: (QuizEvent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isLastQuestion = state.currentIndex == state.totalQuestions - 1

    // Auto-advance to next question after answer is revealed
    LaunchedEffect(state.selectedOptionIndex) {
        if (state.isAnswerRevealed && !isLastQuestion) {
            delay(AUTO_ADVANCE_DELAY_MS)
            onEvent(QuizEvent.NextQuestion)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Top bar: progress + timer ─────────────────────────────────────────
        QuizTopBar(state = state)

        // ── Scrollable question + options ─────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            QuestionSection(
                currentIndex  = state.currentIndex,
                totalQuestions = state.totalQuestions,
                questionText  = state.currentQuestion.questionText,
            )
            Spacer(Modifier.height(28.dp))
            OptionsSection(
                question           = state.currentQuestion,
                selectedOptionIndex = state.selectedOptionIndex,
                isAnswerRevealed   = state.isAnswerRevealed,
                onOptionSelected   = { index ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(QuizEvent.OptionSelected(index))
                },
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Bottom action ─────────────────────────────────────────────────────
        if (state.isAnswerRevealed && isLastQuestion) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorBackground)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    text     = "See Results",
                    variant  = ButtonVariant.Primary,
                    onClick  = { onEvent(QuizEvent.SubmitQuiz) },
                )
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun QuizTopBar(state: QuizUiState.Active) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Question counter
            Text(
                text  = "${state.currentIndex + 1} / ${state.totalQuestions}",
                color = ColorTextSecondary,
                style = AppTheme.typography.body2.copy(
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 13.sp,
                    letterSpacing = 0.5.sp,
                ),
            )

            // Countdown timer
            TimerBadge(
                timeRemainingSeconds = state.timeRemainingSeconds,
                timerFraction        = state.timerFraction,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Lumo LinearProgressIndicator for question progress
        LinearProgressIndicator(
            progress   = state.progressFraction,
            modifier   = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50)),
            color      = ColorAccent,
            trackColor = ColorSurfaceHigh,
        )
    }
}

@Composable
private fun TimerBadge(
    timeRemainingSeconds: Int,
    timerFraction: Float,
) {
    // Color shifts from green → accent → red as time depletes
    val timerColor by animateColorAsState(
        targetValue = when {
            timerFraction > 0.5f -> ColorCorrect
            timerFraction > 0.2f -> ColorAccent
            else                 -> ColorWrong
        },
        animationSpec = tween(durationMillis = 600),
        label         = "timerColor",
    )

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60

    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(timerColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(timerColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = "%d:%02d".format(minutes, seconds),
            color = timerColor,
            style = AppTheme.typography.body1.copy(
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                letterSpacing = 1.sp,
            ),
        )
    }
}

// ── Question section ──────────────────────────────────────────────────────────

@Composable
private fun QuestionSection(
    currentIndex: Int,
    totalQuestions: Int,
    questionText: String,
) {
    Column {
        Text(
            text  = "QUESTION ${currentIndex + 1}",
            color = ColorAccent,
            style = AppTheme.typography.body2.copy(
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text  = questionText,
            color = ColorTextPrimary,
            style = AppTheme.typography.h2.copy(
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                fontStyle  = FontStyle.Italic,
                lineHeight = 30.sp,
                letterSpacing = (-0.5).sp,
            ),
        )
    }
}

// ── Options section ───────────────────────────────────────────────────────────

@Composable
private fun OptionsSection(
    question: Question,
    selectedOptionIndex: Int?,
    isAnswerRevealed: Boolean,
    onOptionSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.forEachIndexed { index, optionText ->
            OptionCard(
                index               = index,
                text                = optionText,
                isSelected          = selectedOptionIndex == index,
                isCorrect           = question.isCorrect(index),
                isAnswerRevealed    = isAnswerRevealed,
                isEnabled           = !isAnswerRevealed,
                onOptionSelected    = onOptionSelected,
            )
        }
    }
}

@Composable
private fun OptionCard(
    index: Int,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswerRevealed: Boolean,
    isEnabled: Boolean,
    onOptionSelected: (Int) -> Unit,
) {
    // Compute the reveal state for this specific card
    val revealAsCorrect = isAnswerRevealed && isCorrect
    val revealAsWrong   = isAnswerRevealed && isSelected && !isCorrect

    // Animate card background color
    val containerColor by animateColorAsState(
        targetValue = when {
            revealAsCorrect -> ColorCorrect.copy(alpha = 0.15f)
            revealAsWrong   -> ColorWrong.copy(alpha = 0.15f)
            isSelected      -> ColorAccent.copy(alpha = 0.10f)
            else            -> ColorSurface
        },
        animationSpec = tween(durationMillis = 350),
        label         = "cardBg_$index",
    )

    // Animate the left-border accent color — the signature visual indicator
    val borderAccentColor by animateColorAsState(
        targetValue = when {
            revealAsCorrect -> ColorCorrect
            revealAsWrong   -> ColorWrong
            isSelected      -> ColorAccent
            else            -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 350),
        label         = "border_$index",
    )

    val textColor by animateColorAsState(
        targetValue = when {
            revealAsCorrect -> ColorCorrect
            revealAsWrong   -> ColorWrong
            isSelected      -> ColorAccent
            else            -> ColorTextPrimary
        },
        animationSpec = tween(durationMillis = 350),
        label         = "textColor_$index",
    )

    OutlinedCard(
        onClick  = { if (isEnabled) onOptionSelected(index) },
        enabled  = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor   = textColor,
        ),
        border   = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Left accent border strip ──────────────────────────────────────
            // The signature reveal element: a thin left strip that lights up
            // green or red when the answer is revealed.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .background(
                        color = borderAccentColor,
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    ),
            )

            // ── Option label (A, B, C, D) ─────────────────────────────────────
            Box(
                modifier          = Modifier
                    .padding(start = 14.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorSurfaceHigh),
                contentAlignment  = Alignment.Center,
            ) {
                Text(
                    text  = ('A' + index).toString(),
                    color = textColor,
                    style = AppTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp,
                    ),
                )
            }

            Spacer(Modifier.width(12.dp))

            // ── Option text ───────────────────────────────────────────────────
            Text(
                text   = text,
                color  = textColor,
                style  = AppTheme.typography.body1.copy(
                    fontSize   = 15.sp,
                    lineHeight = 20.sp,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 14.dp, top = 14.dp, bottom = 14.dp),
            )
        }
    }
}

// ── Async states ──────────────────────────────────────────────────────────────

@Composable
private fun QuizLoadingState() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(36.dp),
            color       = ColorAccent,
            trackColor  = ColorSurfaceHigh,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun QuizErrorState(message: String) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = message,
                color     = ColorWrong,
                textAlign = TextAlign.Center,
                style     = AppTheme.typography.body1.copy(lineHeight = 22.sp),
            )
        }
    }
}