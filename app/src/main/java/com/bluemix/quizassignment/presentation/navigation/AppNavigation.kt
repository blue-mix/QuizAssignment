package com.bluemix.quizassignment.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.bluemix.quizassignment.domain.model.Difficulty
import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.presentation.screens.HomeScreen
import com.bluemix.quizassignment.presentation.screens.PreQuizDialog
import com.bluemix.quizassignment.presentation.screens.QuizDetailScreen
import com.bluemix.quizassignment.presentation.screens.QuizEngineScreen
import com.bluemix.quizassignment.presentation.screens.ResultsScreen
import com.bluemix.quizassignment.presentation.screens.SplashScreen
import com.bluemix.quizassignment.presentation.viewmodels.QuizDetailUiState
import com.bluemix.quizassignment.presentation.viewmodels.QuizDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Root Navigation Host for GrammarFlow.
 *
 * ── Bug 3 Fix: "Not Yet" dismissal leaves button permanently unresponsive ─────
 *
 * The previous code passed no enabled-state signal to [QuizDetailScreen].
 * The screen owned a local `isNavigating` boolean that was set to `true` on
 * the first button tap and was never reset — because `dialog<>` destinations
 * do not remove the host composable from the Compose slot table, so
 * `remember {}` state survives across the entire open/dismiss cycle.
 *
 * FIX — derive `isDialogOpen` from `navController.currentBackStackEntryAsState()`:
 *
 * `currentBackStackEntryAsState()` is a Compose State<NavBackStackEntry?> that
 * recomposes whenever the top of the back stack changes. We use it to derive
 * a Boolean that answers "is the PreQuizDialog currently the top destination?"
 *
 *   Dialog opens  → PreQuizDialog pushed → currentEntry has route PreQuizDialog
 *                                       → isDialogOpen = true
 *                                       → isActionEnabled = false → button disabled
 *
 *   "Not Yet" tap → popBackStack()       → currentEntry reverts to QuizDetail
 *                                       → isDialogOpen = false
 *                                       → isActionEnabled = true  → button enabled ✓
 *
 *   Tap again     → isActionEnabled = true → button enabled → navigate succeeds ✓
 *
 * WHY `currentBackStackEntryAsState()` IS THE CORRECT API:
 *
 *   It is the only reactive, Compose-aware way to observe the navigation back
 *   stack inside a composable. It returns a `State<NavBackStackEntry?>` backed
 *   by a `SnapshotStateHolder` that Navigation updates atomically when the back
 *   stack changes. Any composable that reads it is automatically scheduled for
 *   recomposition when it changes — no manual subscription or lifecycle observer
 *   needed.
 *
 * WHY NOT `navController.currentBackStackEntry` (non-State version):
 *
 *   `navController.currentBackStackEntry` is a plain property, not a
 *   Compose State. Reading it does not subscribe to changes — the composable
 *   would not recompose when the back stack changes, so `isDialogOpen` would
 *   never update after the dialog is dismissed.
 *
 * SCOPING — why `currentBackStackEntryAsState()` is called at the NavHost level
 * and not inside the `composable<Screen.QuizDetail>` block:
 *
 *   Inside `composable<Screen.QuizDetail> { backStackEntry -> ... }`, the
 *   provided `backStackEntry` is the entry for QuizDetail — it does not change
 *   when PreQuizDialog is pushed on top. We need the CURRENT top entry, which
 *   requires the NavController-level state. The value is passed DOWN as a
 *   parameter to keep [QuizDetailScreen] free of navigation imports.
 */
@SuppressLint("RestrictedApi")
@Composable
fun GrammarFlowNavHost(navController: NavHostController) {

    // ── FIX 3: Observe the back stack top reactively ──────────────────────────
    //
    // `currentBackStackEntryAsState()` re-runs whenever the top of the back
    // stack changes (push or pop). This is the Compose State that drives the
    // button's enabled state in QuizDetailScreen.
    val currentEntry by navController.currentBackStackEntryAsState()

    // True only while Screen.PreQuizDialog is the current top destination.
    // `hasRoute<T>()` uses the type-safe route serialization to check the
    // destination — no string-based route comparison, no manual route IDs.
    val isDialogOpen = currentEntry?.destination?.hasRoute<Screen.PreQuizDialog>() == true

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash,
    ) {

        // ── 1. Splash ─────────────────────────────────────────────────────────

        composable<Screen.Splash> {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                },
            )
        }

        // ── 2. Home ───────────────────────────────────────────────────────────

        composable<Screen.Home> {
            HomeScreen(
                onQuizSelected = { quiz ->
                    navController.navigate(Screen.QuizDetail(quizId = quiz.id.toInt()))
                },
            )
        }

        // ── 3. Quiz Detail ────────────────────────────────────────────────────

        composable<Screen.QuizDetail> { backStackEntry ->
            val route: Screen.QuizDetail = backStackEntry.toRoute()

            val viewModel: QuizDetailViewModel = koinViewModel(
                parameters = { parametersOf(route.quizId.toLong()) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val onStartQuiz: () -> Unit = {
                if (state is QuizDetailUiState.Success) {
                    val quiz = (state as QuizDetailUiState.Success).quiz
                    navController.navigate(
                        Screen.PreQuizDialog(
                            quizId      = quiz.id.toInt(),
                            quizTitle   = quiz.title,
                            timeMinutes = quiz.totalTimeInMinutes,
                        )
                    ) {
                        // Navigation-layer backstop against actual double-taps
                        // that slip through within a single frame.
                        launchSingleTop = true
                    }
                }
            }

            when (state) {
                is QuizDetailUiState.Success -> {
                    QuizDetailScreen(
                        quiz            = (state as QuizDetailUiState.Success).quiz,
                        // ── FIX 3: pass derived enabled state ─────────────────
                        //
                        // `!isDialogOpen` is true when PreQuizDialog is NOT the
                        // current back-stack top — i.e., the button should be
                        // active. It flips to false the instant Navigation pushes
                        // Screen.PreQuizDialog, and back to true the instant it
                        // is popped (by "Not Yet", swipe-to-dismiss, or Back).
                        //
                        // This value is derived from the reactive back-stack state
                        // declared above the NavHost builder — it is not a local
                        // boolean and requires no manual reset logic anywhere.
                        isActionEnabled = !isDialogOpen,
                        onStartQuiz     = onStartQuiz,
                        onNavigateBack  = { navController.navigateUp() },
                    )
                }
                else -> {
                    QuizDetailScreen(
                        quiz            =Quiz(
                            id                 = 0L,
                            title              = "",
                            description        = "",
                            totalTimeInMinutes = 0,
                            difficulty         =Difficulty.Medium,
                        ),
                        isActionEnabled = !isDialogOpen,
                        onStartQuiz     = {},
                        onNavigateBack  = { navController.navigateUp() },
                    )
                }
            }
        }

        // ── 4. Pre-Quiz Dialog ────────────────────────────────────────────────
        //
        // `dialog<>` renders as a floating overlay above QuizDetailScreen.
        // QuizDetailScreen stays composed underneath — which is precisely why
        // the old local `isNavigating` boolean was never reset (Bug 3).
        //
        // Navigation automatically manages back-stack pop on system Back gesture.
        // onDismiss calls popBackStack() explicitly to handle the "Not Yet" tap.

        dialog<Screen.PreQuizDialog> { backStackEntry ->
            val route: Screen.PreQuizDialog = backStackEntry.toRoute()

            PreQuizDialog(
                quizTitle   = route.quizTitle,
                timeMinutes = route.timeMinutes,
                onConfirm   = {
                    navController.popBackStack()
                    navController.navigate(
                        Screen.QuizEngine(
                            quizId        = route.quizId,
                            timeInMinutes = route.timeMinutes,
                        )
                    )
                },
                // "Not Yet" tap → pop the dialog → currentBackStackEntryAsState()
                // updates → isDialogOpen becomes false → isActionEnabled becomes
                // true → QuizDetailScreen button re-enables. Automatic. ✓
                onDismiss = {
                    navController.popBackStack()
                },
            )
        }

        // ── 5. Quiz Engine ────────────────────────────────────────────────────

        composable<Screen.QuizEngine> { backStackEntry ->
            val route: Screen.QuizEngine = backStackEntry.toRoute()

            QuizEngineScreen(
                quizId             = route.quizId.toLong(),
                totalTimeInMinutes = route.timeInMinutes,
                onQuizFinished     = { score, total ->
                    navController.navigate(Screen.Results(score = score, total = total)) {
                        popUpTo<Screen.QuizEngine> { inclusive = true }
                    }
                },
            )
        }

        // ── 6. Results ────────────────────────────────────────────────────────

        composable<Screen.Results> { backStackEntry ->
            val route: Screen.Results = backStackEntry.toRoute()

            ResultsScreen(
                score        = route.score,
                total        = route.total,
                onReturnHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }
    }
}
//
///**
// * Root Navigation Host for GrammarFlow.
// *
// * ── FIX 1: launchSingleTop on the PreQuizDialog destination ──────────────────
// *
// * ROOT CAUSE (navigation layer):
// * `navController.navigate(Screen.PreQuizDialog(...))` without `launchSingleTop`
// * pushes a new back-stack entry unconditionally. If the lambda is invoked twice
// * in rapid succession (double-tap, recomposition race, or the old dual-path
// * bug in QuizDetailScreen) two `Screen.PreQuizDialog` entries land on the
// * stack. Dismissing the first `dialog<>` destination reveals the second.
// *
// * FIX:
// * Add `launchSingleTop = true` to the `navOptions` block of the navigate call.
// * Navigation then checks whether the destination is already at the top of the
// * back stack and silently discards the duplicate push if it is. This is the
// * backstop that works at the navigation layer independently of any UI-layer
// * debounce guard.
// *
// * Both defences are necessary:
// *   • UI debounce in QuizDetailScreen  → prevents the *first* duplicate call
// *   • launchSingleTop here              → catches anything that slips through
// *     (e.g., a recomposition or configuration change mid-navigation)
// */
//@Composable
//fun GrammarFlowNavHost(navController: NavHostController) {
//
//    NavHost(
//        navController    = navController,
//        startDestination = Screen.Splash,
//    ) {
//
//        // ── 1. Splash ─────────────────────────────────────────────────────────
//
//        composable<Screen.Splash> {
//            SplashScreen(
//                onNavigateToHome = {
//                    navController.navigate(Screen.Home) {
//                        popUpTo(Screen.Splash) { inclusive = true }
//                    }
//                },
//            )
//        }
//
//        // ── 2. Home ───────────────────────────────────────────────────────────
//
//        composable<Screen.Home> {
//            HomeScreen(
//                onQuizSelected = { quiz ->
//                    navController.navigate(Screen.QuizDetail(quizId = quiz.id.toInt()))
//                },
//            )
//        }
//
//        // ── 3. Quiz Detail ────────────────────────────────────────────────────
//        //
//        // onStartQuiz now navigates to the Screen.PreQuizDialog destination with
//        // launchSingleTop = true. This is the ONLY trigger path for the dialog —
//        // the local boolean + inline PreQuizDialog that previously lived inside
//        // QuizDetailScreen.kt has been removed entirely.
//
//        composable<Screen.QuizDetail> { backStackEntry ->
//            val route: Screen.QuizDetail = backStackEntry.toRoute()
//
//            val viewModel: QuizDetailViewModel = koinViewModel(
//                parameters = { parametersOf(route.quizId.toLong()) },
//            )
//            val state by viewModel.uiState.collectAsStateWithLifecycle()
//
//            val onStartQuiz: () -> Unit = {
//                if (state is QuizDetailUiState.Success) {
//                    val quiz = (state as QuizDetailUiState.Success).quiz
//                    navController.navigate(
//                        Screen.PreQuizDialog(
//                            quizId      = quiz.id.toInt(),
//                            quizTitle   = quiz.title,
//                            timeMinutes = quiz.totalTimeInMinutes,
//                        )
//                    ) {
//                        // ── FIX 1: launchSingleTop backstop ───────────────────
//                        //
//                        // If Screen.PreQuizDialog is already at the top of the
//                        // back stack (from a rapid double-tap or recomposition
//                        // race that slipped past the UI debounce), Navigation
//                        // discards this call instead of pushing a second entry.
//                        //
//                        // Works in tandem with the coroutine debounce gate in
//                        // QuizDetailScreen — the UI guard prevents the first
//                        // duplicate; this setting eliminates any that remain.
//                        launchSingleTop = true
//                    }
//                }
//            }
//
//            when (state) {
//                is QuizDetailUiState.Success -> {
//                    QuizDetailScreen(
//                        quiz           = (state as QuizDetailUiState.Success).quiz,
//                        onStartQuiz    = onStartQuiz,
//                        onNavigateBack = { navController.navigateUp() },
//                    )
//                }
//                else -> {
//                    QuizDetailScreen(
//                        quiz           = Quiz(
//                            id                 = 0L,
//                            title              = "",
//                            description        = "",
//                            totalTimeInMinutes = 0,
//                            difficulty         = Difficulty.Medium,
//                        ),
//                        onStartQuiz    = {},
//                        onNavigateBack = { navController.navigateUp() },
//                    )
//                }
//            }
//        }
//
//        // ── 4. Pre-Quiz Dialog ────────────────────────────────────────────────
//        //
//        // `dialog<>` renders as a true floating overlay. The system Back button
//        // pops it via the back stack — no manual isVisible state required.
//        // launchSingleTop on the navigate() call above ensures this destination
//        // is pushed at most once per user interaction.
//
//        dialog<Screen.PreQuizDialog> { backStackEntry ->
//            val route: Screen.PreQuizDialog = backStackEntry.toRoute()
//
//            PreQuizDialog(
//                quizTitle   = route.quizTitle,
//                timeMinutes = route.timeMinutes,
//                onConfirm   = {
//                    navController.popBackStack()
//                    navController.navigate(
//                        Screen.QuizEngine(
//                            quizId        = route.quizId,
//                            timeInMinutes = route.timeMinutes,
//                        )
//                    )
//                },
//                onDismiss = {
//                    navController.popBackStack()
//                },
//            )
//        }
//
//        // ── 5. Quiz Engine ────────────────────────────────────────────────────
//
//        composable<Screen.QuizEngine> { backStackEntry ->
//            val route: Screen.QuizEngine = backStackEntry.toRoute()
//
//            QuizEngineScreen(
//                quizId             = route.quizId.toLong(),
//                totalTimeInMinutes = route.timeInMinutes,
//                onQuizFinished     = { score, total ->
//                    navController.navigate(Screen.Results(score = score, total = total)) {
//                        popUpTo<Screen.QuizEngine> { inclusive = true }
//                    }
//                },
//            )
//        }
//
//        // ── 6. Results ────────────────────────────────────────────────────────
//
//        composable<Screen.Results> { backStackEntry ->
//            val route: Screen.Results = backStackEntry.toRoute()
//
//            ResultsScreen(
//                score        = route.score,
//                total        = route.total,
//                onReturnHome = {
//                    navController.navigate(Screen.Home) {
//                        popUpTo(Screen.Home) { inclusive = false }
//                    }
//                },
//            )
//        }
//    }
//}