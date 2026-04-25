package com.bluemix.quizassignment.presentation.screens


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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluemix.quizassignment.domain.model.Difficulty
import com.bluemix.quizassignment.domain.model.Quiz
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text

/**
 * Displays detailed metadata for a selected quiz and hosts the [PreQuizDialog].
 *
 * ── State hoisting note ───────────────────────────────────────────────────────
 * This screen owns [showPreQuizDialog] — a purely local UI toggle with no
 * business significance — so it lives here rather than in a ViewModel.
 * The "Start Quiz" callback is only invoked after the dialog confirms.
 *
 * @param quiz            The domain model to display. Sourced from the Home screen's
 *                        selection; passed through the NavHost parameter.
 * @param onStartQuiz     Invoked when the user confirms "Start" in the dialog.
 *                        The NavHost navigates to [Screen.QuizEngine].
 * @param onNavigateBack  Invoked on back-press.
 */
@Composable
fun QuizDetailScreen(
    quiz: Quiz,
    onStartQuiz: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showPreQuizDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        // ── Scrollable body ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                // Reserve space for the pinned CTA at the bottom
                .padding(bottom = 96.dp),
        ) {
            DetailTopBar(onNavigateBack = onNavigateBack)
            HeroSection(quiz = quiz)
            Spacer(Modifier.height(24.dp))
            SyllabusSection(quiz = quiz)
        }

        // ── Pinned bottom CTA ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ColorBackground)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                text = "View Quiz",
                variant = ButtonVariant.Primary,
                onClick = { showPreQuizDialog = true },
            )
        }
    }

    // ── Pre-Quiz confirmation dialog ──────────────────────────────────────────
    if (showPreQuizDialog) {
        PreQuizDialog(
            quizTitle = quiz.title,
            timeMinutes = quiz.totalTimeInMinutes,
            onConfirm = {
                showPreQuizDialog = false
                onStartQuiz()
            },
            onDismiss = { showPreQuizDialog = false },
        )
    }
}

// ── Top bar with back arrow ───────────────────────────────────────────────────

@Composable
private fun DetailTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            variant = ButtonVariant.Ghost,
            onClick = onNavigateBack,
            content = {
                Text(
                    text = "← Back",
                    color = ColorTextSecondary,
                    style = AppTheme.typography.body1.copy(fontSize = 15.sp),
                )
            },
        )
    }
}

// ── Hero section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(quiz: Quiz) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        // Difficulty pill
        val (diffLabel, diffColor) = when (quiz.difficulty) {
            Difficulty.Easy -> "Easy" to ColorCorrect
            Difficulty.Medium -> "Medium" to ColorAccent
            Difficulty.Hard -> "Hard" to ColorWrong
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(diffColor.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = diffLabel.uppercase(),
                color = diffColor,
                style = AppTheme.typography.body2.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = quiz.title,
            color = ColorTextPrimary,
            style = AppTheme.typography.h1.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-1.5).sp,
                lineHeight = 42.sp,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = quiz.description,
            color = ColorTextSecondary,
            style = AppTheme.typography.body1.copy(lineHeight = 24.sp),
        )

        Spacer(Modifier.height(28.dp))

        // Metadata row
        MetadataRow(quiz = quiz)
    }
}

@Composable
private fun MetadataRow(quiz: Quiz) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MetadataItem(label = "TIME", value = "${quiz.totalTimeInMinutes} min")
        VerticalDivider()
        MetadataItem(label = "QUESTIONS", value = "4 – 16")   // seeded range
        VerticalDivider()
        MetadataItem(
            label = "LEVEL", value = when (quiz.difficulty) {
                Difficulty.Easy -> "Easy"
                Difficulty.Medium -> "Medium"
                Difficulty.Hard -> "Hard"
            }
        )
    }
}

@Composable
private fun MetadataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = ColorTextSecondary,
            style = AppTheme.typography.body2.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = ColorTextPrimary,
            style = AppTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 32.dp)
            .background(ColorBorder),
    )
}

// ── Syllabus section ──────────────────────────────────────────────────────────

@Composable
private fun SyllabusSection(quiz: Quiz) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Text(
            text = "What you'll practice",
            color = ColorTextPrimary,
            style = AppTheme.typography.h3.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
        )
        Spacer(Modifier.height(12.dp))

        // Static syllabus bullets — in production, source from domain model
        val syllabusItems = listOf(
            "Identifying grammatical categories accurately",
            "Applying rules in context-driven sentences",
            "Avoiding common structural errors",
            "Timed recall under quiz pressure",
        )
        syllabusItems.forEach { item ->
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ColorAccent),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item,
                    color = ColorTextSecondary,
                    style = AppTheme.typography.body2.copy(lineHeight = 20.sp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Warning banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ColorSurfaceHigh)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⚠️", style = AppTheme.typography.body1)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Once started, the timer cannot be paused.",
                color = ColorTextSecondary,
                style = AppTheme.typography.body2.copy(lineHeight = 18.sp),
            )
        }
    }
}