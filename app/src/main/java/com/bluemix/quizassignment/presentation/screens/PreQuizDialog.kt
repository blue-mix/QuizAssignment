package com.bluemix.quizassignment.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text

/**
 * Modal bottom sheet that confirms the user's intent to start the quiz.
 *
 * ── Why a BottomSheet and not an AlertDialog? ─────────────────────────────────
 * The Lumo [ModalBottomSheet] gives us more vertical real estate for the warning
 * content, a native feel on Android, and a gesture-dismissable surface —
 * all without custom dialog scaffolding.
 *
 * ── State ownership ───────────────────────────────────────────────────────────
 * Visibility state (`showPreQuizDialog`) is owned by [QuizDetailScreen].
 * This composable is purely presentational — it calls [onConfirm] or [onDismiss]
 * and the parent decides what to do.
 *
 * @param quizTitle   Title of the selected quiz, shown in the sheet header.
 * @param timeMinutes Allotted time, shown in the warning copy.
 * @param onConfirm   Called when the user taps "Start Quiz."
 * @param onDismiss   Called on cancel tap or swipe-to-dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreQuizDialog(
    quizTitle: String,
    timeMinutes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
//        isVisible = true,
        onDismissRequest = onDismiss,
       // sheetGesturesEnabled = true,
        dragHandle = { SheetDragHandle() },
    ) {
        PreQuizSheetContent(
            quizTitle = quizTitle,
            timeMinutes = timeMinutes,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sheet content — stateless, independently testable/previewable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PreQuizSheetContent(
    quizTitle: String,
    timeMinutes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface)
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Quiz title ────────────────────────────────────────────────────────
        Text(
            text = quizTitle,
            color = ColorAccent,
            style = AppTheme.typography.body2.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Ready to begin?",
            color = ColorTextPrimary,
            style = AppTheme.typography.h2.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            ),
        )

        Spacer(Modifier.height(20.dp))

        // ── Warning card ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ColorSurfaceHigh)
                .padding(16.dp),
        ) {
            WarningRow(
                icon = "⏱",
                label = "Timer",
                body = "You have $timeMinutes minutes. The clock starts the moment you tap Start.",
            )
            Spacer(Modifier.height(12.dp))
            WarningRow(
                icon = "🚫",
                label = "No pausing",
                body = "The timer cannot be paused or reset once the quiz begins.",
            )
            Spacer(Modifier.height(12.dp))
            WarningRow(
                icon = "✓",
                label = "One attempt",
                body = "Each question allows one answer. Choose carefully before tapping Next.",
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Action buttons ────────────────────────────────────────────────────
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = "Start Quiz",
            variant = ButtonVariant.Primary,
            onClick = onConfirm,
        )
        Spacer(Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = "Not yet",
            variant = ButtonVariant.SecondaryOutlined,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun WarningRow(icon: String, label: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = icon, style = AppTheme.typography.body1.copy(fontSize = 16.sp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                color = ColorTextPrimary,
                style = AppTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                color = ColorTextSecondary,
                style = AppTheme.typography.body2.copy(lineHeight = 18.sp, fontSize = 13.sp),
            )
        }
    }
}

// ── Drag handle ───────────────────────────────────────────────────────────────

@Composable
private fun SheetDragHandle() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface)
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(ColorBorder),
        )
    }
}