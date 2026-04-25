package com.bluemix.quizassignment.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text

/**
 * Celebratory results screen shown after a quiz is submitted or the timer ends.
 *
 * Receives [score] and [total] from the navigation route — no ViewModel or
 * database read needed. The screen is fully renderable from back-stack args.
 *
 * @param score           Number of correct answers.
 * @param total           Total number of questions.
 * @param onReturnHome    Navigates back to the Home dashboard.
 */
@Composable
fun ResultsScreen(
    score: Int,
    total: Int,
    onReturnHome: () -> Unit,
) {
    val accuracy       = if (total > 0) score.toFloat() / total.toFloat() else 0f
    val performanceTag = when {
        accuracy >= 0.9f -> "Excellent! 🎉"
        accuracy >= 0.7f -> "Well done! 👍"
        accuracy >= 0.5f -> "Keep going! 💪"
        else             -> "Room to grow! 📚"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        // ── Section label ─────────────────────────────────────────────────────
        Text(
            text  = "QUIZ COMPLETE",
            color = ColorAccent,
            style = AppTheme.typography.body2.copy(
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 3.sp,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text  = performanceTag,
            color = ColorTextPrimary,
            style = AppTheme.typography.h1.copy(
                fontSize   = 30.sp,
                fontWeight = FontWeight.Black,
                fontStyle  = FontStyle.Italic,
            ),
        )

        Spacer(Modifier.height(40.dp))

        // ── Animated score ring ───────────────────────────────────────────────
        ScoreRing(
            score    = score,
            total    = total,
            accuracy = accuracy,
        )

        Spacer(Modifier.height(40.dp))

        // ── Stats row ─────────────────────────────────────────────────────────
        StatsRow(score = score, total = total, accuracy = accuracy)

        Spacer(Modifier.weight(1f))

        // ── CTA ───────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                text     = "Return to Dashboard",
                variant  = ButtonVariant.Primary,
                onClick  = onReturnHome,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                text     = "Try Another Quiz",
                variant  = ButtonVariant.SecondaryOutlined,
                onClick  = onReturnHome,
            )
        }
    }
}

// ── Score ring ────────────────────────────────────────────────────────────────

/**
 * Animated arc drawn on a [Canvas] — no external library dependency.
 * The sweep angle animates from 0° to the final value over 1.2 s using
 * [FastOutSlowInEasing], giving a satisfying "filling" feel.
 */
@Composable
private fun ScoreRing(
    score: Int,
    total: Int,
    accuracy: Float,
) {
    val sweepAngle = remember { Animatable(0f) }
    val targetSweep = 360f * accuracy

    LaunchedEffect(accuracy) {
        sweepAngle.animateTo(
            targetValue   = targetSweep,
            animationSpec = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
        )
    }

    val ringColor = when {
        accuracy >= 0.7f -> ColorCorrect
        accuracy >= 0.4f -> ColorAccent
        else             -> ColorWrong
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcInset    = strokeWidth / 2f
            val arcSize     = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft     = Offset(arcInset, arcInset)

            // Track (background ring)
            drawArc(
                color       = Color(0xFF2A2A2A),
                startAngle  = -90f,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc
            drawArc(
                color       = ringColor,
                startAngle  = -90f,
                sweepAngle  = sweepAngle.value,
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        // Score text centred inside the ring
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "$score",
                color = ColorTextPrimary,
                style = AppTheme.typography.h1.copy(
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                ),
            )
            Text(
                text  = "out of $total",
                color = ColorTextSecondary,
                style = AppTheme.typography.body2.copy(fontSize = 13.sp),
            )
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(score: Int, total: Int, accuracy: Float) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell(
            label = "CORRECT",
            value = "$score",
            color = ColorCorrect,
        )
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 40.dp)
                .background(ColorBorder),
        )
        StatCell(
            label = "WRONG",
            value = "${total - score}",
            color = ColorWrong,
        )
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 40.dp)
                .background(ColorBorder),
        )
        StatCell(
            label = "ACCURACY",
            value = "${(accuracy * 100).toInt()}%",
            color = ColorAccent,
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = label,
            color = ColorTextSecondary,
            style = AppTheme.typography.body2.copy(
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = value,
            color = color,
            style = AppTheme.typography.h2.copy(
                fontSize   = 24.sp,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}