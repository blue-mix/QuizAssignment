package com.bluemix.quizassignment.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluemix.quizassignment.domain.model.Difficulty
import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.presentation.viewmodels.HomeEvent
import com.bluemix.quizassignment.presentation.viewmodels.HomeUiState
import com.bluemix.quizassignment.presentation.viewmodels.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text
import ui.components.card.CardDefaults
import ui.components.card.OutlinedCard
import ui.components.progressindicators.CircularProgressIndicator

/**
 * Entry-point composable for the Home dashboard.
 *
 * Responsibilities:
 *  - Collect [HomeUiState] from [HomeViewModel].
 *  - Route the state to the correct stateless sub-composable.
 *  - Delegate navigation decisions to [onQuizSelected].
 */
@Composable
fun HomeScreen(
    onQuizSelected: (Quiz) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onEvent = viewModel::onEvent,
        onQuizSelected = onQuizSelected,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless Content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onQuizSelected: (Quiz) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding(),
    ) {
        HomeDashboardHeader()

        when (state) {
            HomeUiState.Loading -> HomeLoadingState()
            HomeUiState.Empty -> HomeEmptyState(onRetry = { onEvent(HomeEvent.Refresh) })
            is HomeUiState.Success -> HomeQuizList(
                quizzes = state.quizzes,
                onQuizSelected = onQuizSelected,
            )

            is HomeUiState.Error -> HomeErrorState(
                message = state.message,
                onRetry = { onEvent(HomeEvent.Refresh) },
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeDashboardHeader() {
    Column(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 8.dp),
    ) {
        Text(
            text = "Your Quizzes",
            color = ColorTextPrimary,
            style = AppTheme.typography.h2.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sharpen your grammar. One quiz at a time.",
            color = ColorTextSecondary,
            style = AppTheme.typography.body2,
        )
    }
}

// ── Quiz List ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeQuizList(
    quizzes: List<Quiz>,
    onQuizSelected: (Quiz) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = quizzes, key = { it.id }) { quiz ->
            QuizCard(quiz = quiz, onClick = { onQuizSelected(quiz) })
        }
        // Bottom breathing room for navigation gestures
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Quiz Card ─────────────────────────────────────────────────────────────────

@Composable
private fun QuizCard(
    quiz: Quiz,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),   // squircle-adjacent geometry
        colors = CardDefaults.cardColors(
            containerColor = ColorSurface,
            contentColor = ColorTextPrimary,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: text content
            Column(modifier = Modifier.weight(1f)) {
                // Difficulty chip
                DifficultyChip(difficulty = quiz.difficulty)
                Spacer(Modifier.height(10.dp))

                Text(
                    text = quiz.title,
                    color = ColorTextPrimary,
                    style = AppTheme.typography.h3.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = quiz.description,
                    color = ColorTextSecondary,
                    style = AppTheme.typography.body2.copy(fontSize = 13.sp),
                    maxLines = 2,
                )
                Spacer(Modifier.height(12.dp))

                // Time indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ColorAccent, shape = RoundedCornerShape(50)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${quiz.totalTimeInMinutes} min",
                        color = ColorAccent,
                        style = AppTheme.typography.body2.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            // Right: arrow indicator
            Spacer(Modifier.width(16.dp))
            Text(
                text = "→",
                color = ColorTextSecondary,
                style = AppTheme.typography.h3.copy(fontSize = 22.sp),
            )
        }
    }
}

// ── Difficulty Chip ───────────────────────────────────────────────────────────

@Composable
private fun DifficultyChip(difficulty: Difficulty) {
    val (label, color) = when (difficulty) {
        Difficulty.Easy -> "Easy" to ColorCorrect
        Difficulty.Medium -> "Medium" to ColorAccent
        Difficulty.Hard -> "Hard" to ColorWrong
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            style = AppTheme.typography.body2.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        )
    }
}

// ── Async States ──────────────────────────────────────────────────────────────

@Composable
private fun HomeLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = ColorAccent,
            trackColor = Color(0xFF2A2A2A),
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun HomeEmptyState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No quizzes yet.",
            color = ColorTextPrimary,
            style = AppTheme.typography.h3.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "The database may still be seeding.",
            color = ColorTextSecondary,
            style = AppTheme.typography.body2,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            text = "Refresh",
            variant = ButtonVariant.PrimaryOutlined,
            onClick = onRetry,
        )
    }
}

@Composable
private fun HomeErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Something went wrong.",
            color = ColorWrong,
            style = AppTheme.typography.h3.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = ColorTextSecondary,
            style = AppTheme.typography.body2,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            text = "Try Again",
            variant = ButtonVariant.Primary,
            onClick = onRetry,
        )
    }
}