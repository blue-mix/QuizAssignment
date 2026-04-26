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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
//
///**
// * Displays detailed metadata for a selected quiz.
// *
// * ── FIX 1: Double bottom-sheet / double-navigate guard ────────────────────────
// *
// * ROOT CAUSE (two independent trigger paths):
// *
// *   Path A — local boolean: The previous version owned `showPreQuizDialog` as a
// *   local `remember { mutableStateOf(false) }` and rendered `PreQuizDialog`
// *   inline when it was true.
// *
// *   Path B — navigation: `onStartQuiz` was wired in the NavHost to call
// *   `navController.navigate(Screen.PreQuizDialog(...))`.
// *
// *   Both paths were active simultaneously. A fast double-tap (or a recomposition
// *   racing with the click handler) could push two dialog destinations onto the
// *   back stack — showing two overlapping sheets where dismissing the top one
// *   revealed a second underneath.
// *
// * SOLUTION — single authoritative trigger path + coroutine debounce:
// *
// *   1. Remove the local `showPreQuizDialog` boolean entirely. The dialog is
// *      now a navigation destination only (declared as `dialog<>` in the NavHost).
// *      There is exactly ONE trigger path: `onStartQuiz()` → NavHost →
// *      `navController.navigate(Screen.PreQuizDialog(...)) { launchSingleTop = true }`.
// *
// *   2. The "View Quiz" button is guarded by a coroutine-based debouncer.
// *      `isNavigating` is a local boolean captured in a `rememberCoroutineScope`
// *      lambda. Once set to true it blocks all subsequent taps until the
// *      composable leaves the composition (i.e., the dialog is on screen).
// *      This is the Compose-idiomatic alternative to a raw `System.currentTimeMillis`
// *      timestamp check — it integrates with the coroutine scope and is
// *      automatically reset if the screen reappears (e.g., user presses Back).
// *
// *   `launchSingleTop = true` in the NavHost is the backstop: even if two
// *   navigate() calls somehow race, Navigation will silently discard the second
// *   one if the destination is already at the top of the back stack.
// *
// * @param quiz            The domain model to display.
// * @param onStartQuiz     Wired in the NavHost to `navController.navigate(Screen.PreQuizDialog(...))`.
// * @param onNavigateBack  Invoked on back-press.
// */
//@Composable
//fun QuizDetailScreen(
//    quiz: Quiz,
//    onStartQuiz: () -> Unit,
//    onNavigateBack: () -> Unit,
//) {
//    // ── FIX 1A: Single-flight debounce guard ──────────────────────────────────
//    //
//    // `isNavigating` starts false. The first tap sets it to true and calls
//    // onStartQuiz(). Every subsequent tap while isNavigating == true is a no-op.
//    //
//    // Why a coroutine scope and not just a raw boolean?
//    // rememberCoroutineScope() is tied to the composition lifecycle. If the user
//    // presses Back from the dialog and returns to this screen, the scope is
//    // re-created and isNavigating is reset to false automatically — no manual
//    // cleanup needed. A raw `var` retained across recompositions would require
//    // explicit reset logic.
//    val scope = rememberCoroutineScope()
//    var isNavigating by remember { mutableStateOf(false) }
//
//    // ── REMOVED: local `showPreQuizDialog` boolean and inline PreQuizDialog ───
//    //
//    // The old code rendered PreQuizDialog here inside an `if (showPreQuizDialog)`
//    // block. That created a duplicate trigger path alongside the NavHost's
//    // `dialog<Screen.PreQuizDialog>` destination. Both are now gone from this
//    // composable. The dialog lives exclusively in the NavHost.
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(ColorBackground),
//    ) {
//        // ── Scrollable body ───────────────────────────────────────────────────
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//                .statusBarsPadding()
//                .padding(bottom = 96.dp),
//        ) {
//            DetailTopBar(onNavigateBack = onNavigateBack)
//            HeroSection(quiz = quiz)
//            Spacer(Modifier.height(24.dp))
//            SyllabusSection(quiz = quiz)
//        }
//
//        // ── Pinned bottom CTA ─────────────────────────────────────────────────
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .background(ColorBackground)
//                .navigationBarsPadding()
//                .padding(horizontal = 24.dp, vertical = 16.dp),
//        ) {
//            Button(
//                modifier = Modifier.fillMaxWidth(),
//                text     = "View Quiz",
//                variant  = ButtonVariant.Primary,
//                // ── FIX 1B: Debounced onClick ─────────────────────────────────
//                //
//                // The guard `if (isNavigating) return@launch` ensures that no
//                // matter how rapidly the user taps — or how many recompositions
//                // race — only one navigate() call is ever dispatched per screen visit.
//                onClick  = {
//                    scope.launch {
//                        if (isNavigating) return@launch  // ← debounce gate
//                        isNavigating = true
//                        onStartQuiz()
//                        // isNavigating remains true until this composable leaves
//                        // the composition when the dialog destination mounts.
//                        // If the user returns via Back, the scope re-creates and
//                        // isNavigating resets to false for the next attempt.
//                    }
//                },
//            )
//        }
//    }
//}
//
//// ── Top bar with back arrow ───────────────────────────────────────────────────
//
//@Composable
//private fun DetailTopBar(onNavigateBack: () -> Unit) {
//    Row(
//        modifier          = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 12.dp),
//        verticalAlignment = Alignment.CenterVertically,
//    ) {
//        Button(
//            variant = ButtonVariant.Ghost,
//            onClick = onNavigateBack,
//            content = {
//                Text(
//                    text  = "← Back",
//                    color = ColorTextSecondary,
//                    style = AppTheme.typography.body1.copy(fontSize = 15.sp),
//                )
//            },
//        )
//    }
//}
//
//// ── Hero section ──────────────────────────────────────────────────────────────
//
//@Composable
//private fun HeroSection(quiz: Quiz) {
//    Column(
//        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
//    ) {
//        val (diffLabel, diffColor) = when (quiz.difficulty) {
//            Difficulty.Easy   -> "Easy"   to ColorCorrect
//            Difficulty.Medium -> "Medium" to ColorAccent
//            Difficulty.Hard   -> "Hard"   to ColorWrong
//        }
//        Box(
//            modifier = Modifier
//                .clip(RoundedCornerShape(8.dp))
//                .background(diffColor.copy(alpha = 0.14f))
//                .padding(horizontal = 10.dp, vertical = 4.dp),
//        ) {
//            Text(
//                text  = diffLabel.uppercase(),
//                color = diffColor,
//                style = AppTheme.typography.body2.copy(
//                    fontSize      = 10.sp,
//                    fontWeight    = FontWeight.Bold,
//                    letterSpacing = 2.sp,
//                ),
//            )
//        }
//
//        Spacer(Modifier.height(16.dp))
//
//        Text(
//            text  = quiz.title,
//            color = ColorTextPrimary,
//            style = AppTheme.typography.h1.copy(
//                fontSize      = 38.sp,
//                fontWeight    = FontWeight.Black,
//                fontStyle     = FontStyle.Italic,
//                letterSpacing = (-1.5).sp,
//                lineHeight    = 42.sp,
//            ),
//        )
//
//        Spacer(Modifier.height(12.dp))
//
//        Text(
//            text  = quiz.description,
//            color = ColorTextSecondary,
//            style = AppTheme.typography.body1.copy(lineHeight = 24.sp),
//        )
//
//        Spacer(Modifier.height(28.dp))
//
//        MetadataRow(quiz = quiz)
//    }
//}
//
//@Composable
//private fun MetadataRow(quiz: Quiz) {
//    Row(
//        modifier              = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(16.dp))
//            .background(ColorSurface)
//            .padding(20.dp),
//        horizontalArrangement = Arrangement.SpaceEvenly,
//    ) {
//        MetadataItem(label = "TIME", value = "${quiz.totalTimeInMinutes} min")
//        VerticalDivider()
//        MetadataItem(label = "QUESTIONS", value = "4 – 16")
//        VerticalDivider()
//        MetadataItem(
//            label = "LEVEL",
//            value = when (quiz.difficulty) {
//                Difficulty.Easy   -> "Easy"
//                Difficulty.Medium -> "Medium"
//                Difficulty.Hard   -> "Hard"
//            },
//        )
//    }
//}
//
//@Composable
//private fun MetadataItem(label: String, value: String) {
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(
//            text  = label,
//            color = ColorTextSecondary,
//            style = AppTheme.typography.body2.copy(
//                fontSize      = 9.sp,
//                fontWeight    = FontWeight.Bold,
//                letterSpacing = 1.5.sp,
//            ),
//        )
//        Spacer(Modifier.height(4.dp))
//        Text(
//            text  = value,
//            color = ColorTextPrimary,
//            style = AppTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
//        )
//    }
//}
//
//@Composable
//private fun VerticalDivider() {
//    Box(
//        modifier = Modifier
//            .size(width = 1.dp, height = 32.dp)
//            .background(ColorBorder),
//    )
//}
//
//// ── Syllabus section ──────────────────────────────────────────────────────────
//
//@Composable
//private fun SyllabusSection(quiz: Quiz) {
//    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
//        Text(
//            text  = "What you'll practice",
//            color = ColorTextPrimary,
//            style = AppTheme.typography.h3.copy(
//                fontSize      = 16.sp,
//                fontWeight    = FontWeight.Bold,
//                letterSpacing = (-0.3).sp,
//            ),
//        )
//        Spacer(Modifier.height(12.dp))
//
//        val syllabusItems = listOf(
//            "Identifying grammatical categories accurately",
//            "Applying rules in context-driven sentences",
//            "Avoiding common structural errors",
//            "Timed recall under quiz pressure",
//        )
//        syllabusItems.forEach { item ->
//            Row(
//                modifier          = Modifier.padding(vertical = 6.dp),
//                verticalAlignment = Alignment.Top,
//            ) {
//                Box(
//                    modifier = Modifier
//                        .padding(top = 7.dp)
//                        .size(5.dp)
//                        .clip(RoundedCornerShape(50))
//                        .background(ColorAccent),
//                )
//                Spacer(Modifier.width(10.dp))
//                Text(
//                    text  = item,
//                    color = ColorTextSecondary,
//                    style = AppTheme.typography.body2.copy(lineHeight = 20.sp),
//                )
//            }
//        }
//
//        Spacer(Modifier.height(20.dp))
//
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(12.dp))
//                .background(ColorSurfaceHigh)
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            Text(text = "⚠️", style = AppTheme.typography.body1)
//            Spacer(Modifier.width(10.dp))
//            Text(
//                text  = "Once started, the timer cannot be paused.",
//                color = ColorTextSecondary,
//                style = AppTheme.typography.body2.copy(lineHeight = 18.sp),
//            )
//        }
//    }
//}

/**
 * Displays detailed metadata for a selected quiz.
 *
 * ── Bug 3 Fix: Unresponsive dialog trigger after "Not Yet" dismissal ──────────
 *
 * ROOT CAUSE — `dialog<>` keeps the host composable alive:
 *
 *   The previous fix (Bug 1) used a `var isNavigating by remember { mutableStateOf(false) }`
 *   combined with `rememberCoroutineScope()`. The comment stated:
 *     "If the user returns via Back, the scope re-creates and isNavigating resets."
 *
 *   This assumption is CORRECT for `composable<>` destinations, where pressing
 *   Back destroys the dialog's entry AND causes the host entry to leave and
 *   re-enter Compose's slot table — resetting all `remember {}` state.
 *
 *   It is WRONG for `dialog<>` destinations.
 *
 *   A `dialog<>` destination is a floating overlay. The Navigation library renders
 *   it ON TOP of the host screen's composable, which remains in the Compose slot
 *   table continuously. The host's composition is never interrupted:
 *
 *     Back stack state changes:
 *       QuizDetail → [navigate] → QuizDetail | PreQuizDialog ← top
 *       QuizDetail | PreQuizDialog → [popBackStack] → QuizDetail ← top
 *
 *     Composition tree changes:
 *       QuizDetailScreen [composed] → stays composed → stays composed
 *                                     PreQuizDialog [enters]  [leaves]
 *
 *   Because `QuizDetailScreen` never leaves composition, `remember {}` retains
 *   its value across the entire open→dismiss cycle. `isNavigating` was set to
 *   `true` on the first tap and was NEVER reset — permanently latching the gate.
 *
 * FIX — derive the enabled state from navigation truth, not from local mutable state:
 *
 *   The back stack is the single source of truth for whether the dialog is open.
 *   Duplicating that truth into a local `isNavigating` boolean created the stale-
 *   state problem. The fix eliminates the local boolean entirely and instead
 *   accepts `isActionEnabled: Boolean` as a parameter derived by the NavHost from
 *   `navController.currentBackStackEntry`.
 *
 *   In `AppNavigation.kt`:
 *     val currentEntry by navController.currentBackStackEntryAsState()
 *     val isDialogOpen = currentEntry?.destination?.hasRoute<Screen.PreQuizDialog>() == true
 *     QuizDetailScreen(isActionEnabled = !isDialogOpen, ...)
 *
 *   State machine:
 *     Dialog closed → isDialogOpen = false → isActionEnabled = true  → button enabled
 *     Dialog open   → isDialogOpen = true  → isActionEnabled = false → button disabled
 *     Dialog closed → isDialogOpen = false → isActionEnabled = true  → button enabled ✓
 *
 *   The gate now opens automatically when the dialog is dismissed — with no
 *   reset logic, no LaunchedEffect, no DisposableEffect, and no manual cleanup.
 *
 * RETAINED — within-tap double-fire protection:
 *
 *   A single-cycle `var isTapInFlight` boolean (reset by a `LaunchedEffect`
 *   keyed on `isActionEnabled`) still prevents the race where two recompositions
 *   fire the onClick lambda in the same frame before `isDialogOpen` has updated.
 *   This is a within-tap guard (milliseconds), not a cross-dismiss guard (seconds).
 *   `launchSingleTop = true` in the NavHost remains as the navigation-layer backstop.
 *
 * @param quiz            The domain model to render.
 * @param isActionEnabled Derived from navigation state in [AppNavigation]:
 *                        `true` when [Screen.PreQuizDialog] is NOT the current
 *                        back-stack top. Controls button enabled state and the
 *                        within-tap debounce reset.
 * @param onStartQuiz     Navigates to [Screen.PreQuizDialog]. Wired in NavHost.
 * @param onNavigateBack  Pops back to [Screen.Home].
 */
@Composable
fun QuizDetailScreen(
    quiz: Quiz,
    // ── FIX 3: isActionEnabled replaces the local isNavigating boolean ─────────
    //
    // Passed in from the NavHost, derived from `navController.currentBackStackEntry`.
    // True  → dialog is NOT open; button is enabled and ready to be tapped.
    // False → dialog IS open; button is visually and functionally disabled.
    //
    // This value is the single source of truth for the button's enabled state.
    // It reacts to the actual navigation back-stack, so dismissing the dialog
    // automatically restores `isActionEnabled = true` without any local reset.
    isActionEnabled: Boolean,
    onStartQuiz: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    // ── Within-tap double-fire protection ─────────────────────────────────────
    //
    // `isTapInFlight` guards against the specific race where two recompositions
    // in the same frame both execute the onClick lambda before the Navigation
    // back-stack update (which would flip `isActionEnabled` to false) has been
    // reflected in the composition. This window is typically < 16ms.
    //
    // This is intentionally separate from `isActionEnabled`:
    //   isActionEnabled — cross-dismiss gate; driven by navigation back stack.
    //   isTapInFlight   — within-tap gate;   driven by a single-frame boolean.
    //
    // The LaunchedEffect below resets `isTapInFlight` when `isActionEnabled`
    // becomes true — i.e., when the dialog is dismissed and the button is ready
    // again. This is safe because:
    //   - LaunchedEffect(key) re-runs when the key changes.
    //   - isActionEnabled flips false→true only after a real dialog dismissal.
    //   - Resetting isTapInFlight at that point is always correct.
    var isTapInFlight by remember { mutableStateOf(false) }

    // Reset the within-tap guard whenever the button becomes re-enabled.
    // This covers the case where a tap fires and the dialog is then dismissed —
    // without this reset, isTapInFlight would remain true across the dismissal.
    LaunchedEffect(isActionEnabled) {
        if (isActionEnabled) {
            isTapInFlight = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 96.dp),
        ) {
            DetailTopBar(onNavigateBack = onNavigateBack)
            HeroSection(quiz = quiz)
            Spacer(Modifier.height(24.dp))
            SyllabusSection(quiz = quiz)
        }

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
                text     = "View Quiz",
                variant  = ButtonVariant.Primary,
                // ── FIX 3: enabled driven by navigation truth ─────────────────
                //
                // The button is disabled whenever:
                //   a) isActionEnabled = false → dialog is currently open
                //   b) isTapInFlight = true    → a tap just fired this frame
                //
                // Compose's Button with `enabled = false` is both visually
                // dimmed AND ignores all click events — no onClick guard needed
                // inside the lambda for the cross-dismiss case.
                enabled  = isActionEnabled && !isTapInFlight,
                onClick  = {
                    // Within-tap guard: set before the navigation call so that
                    // any recomposition triggered by navigate() sees it as true.
                    isTapInFlight = true
                    onStartQuiz()
                    // isActionEnabled will flip to false once the back-stack
                    // update propagates through currentBackStackEntryAsState(),
                    // which also disables the button for the dialog-open duration.
                },
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            variant = ButtonVariant.Ghost,
            onClick = onNavigateBack,
            content = {
                Text(
                    text  = "← Back",
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
        val (diffLabel, diffColor) = when (quiz.difficulty) {
            Difficulty.Easy   -> "Easy"   to ColorCorrect
            Difficulty.Medium -> "Medium" to ColorAccent
            Difficulty.Hard   -> "Hard"   to ColorWrong
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(diffColor.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text  = diffLabel.uppercase(),
                color = diffColor,
                style = AppTheme.typography.body2.copy(
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text  = quiz.title,
            color = ColorTextPrimary,
            style = AppTheme.typography.h1.copy(
                fontSize      = 38.sp,
                fontWeight    = FontWeight.Black,
                fontStyle     = FontStyle.Italic,
                letterSpacing = (-1.5).sp,
                lineHeight    = 42.sp,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text  = quiz.description,
            color = ColorTextSecondary,
            style = AppTheme.typography.body1.copy(lineHeight = 24.sp),
        )

        Spacer(Modifier.height(28.dp))

        MetadataRow(quiz = quiz)
    }
}

@Composable
private fun MetadataRow(quiz: Quiz) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MetadataItem(label = "TIME", value = "${quiz.totalTimeInMinutes} min")
        VerticalDivider()
        MetadataItem(label = "QUESTIONS", value = "4 – 16")
        VerticalDivider()
        MetadataItem(
            label = "LEVEL",
            value = when (quiz.difficulty) {
                Difficulty.Easy   -> "Easy"
                Difficulty.Medium -> "Medium"
                Difficulty.Hard   -> "Hard"
            },
        )
    }
}

@Composable
private fun MetadataItem(label: String, value: String) {
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
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text  = "What you'll practice",
            color = ColorTextPrimary,
            style = AppTheme.typography.h3.copy(
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
        )
        Spacer(Modifier.height(12.dp))

        val syllabusItems = listOf(
            "Identifying grammatical categories accurately",
            "Applying rules in context-driven sentences",
            "Avoiding common structural errors",
            "Timed recall under quiz pressure",
        )
        syllabusItems.forEach { item ->
            Row(
                modifier          = Modifier.padding(vertical = 6.dp),
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
                    text  = item,
                    color = ColorTextSecondary,
                    style = AppTheme.typography.body2.copy(lineHeight = 20.sp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

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
                text  = "Once started, the timer cannot be paused.",
                color = ColorTextSecondary,
                style = AppTheme.typography.body2.copy(lineHeight = 18.sp),
            )
        }
    }
}