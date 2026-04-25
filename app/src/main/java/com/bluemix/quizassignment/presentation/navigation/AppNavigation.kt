package com.bluemix.quizassignment.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/**
 * Root Navigation Host for GrammarFlow.
 *
 * ── Architecture contract ─────────────────────────────────────────────────────
 * This composable has exactly ONE job: declare the navigation graph.
 * It must not contain any layout code, business logic, or state that doesn't
 * belong to navigation. All UI rendering is delegated entirely to the
 * named screen composables (e.g., [SplashScreen], [HomeScreen]).
 *
 * ── What belongs here ─────────────────────────────────────────────────────────
 *   ✔  Route declaration (`composable<>`, `dialog<>`)
 *   ✔  Type-safe route argument extraction (`toRoute<>`)
 *   ✔  ViewModel injection via `koinViewModel()`
 *   ✔  Navigation callbacks passed down as lambdas
 *   ✔  Back-stack manipulation (`popUpTo`, `inclusive`)
 *
 * ── What does NOT belong here ─────────────────────────────────────────────────
 *   ✗  Column / Row / Box layout
 *   ✗  State collection (use screen composables for that)
 *   ✗  Direct UI rendering
 *
 * ── Type-safe routing ─────────────────────────────────────────────────────────
 * Every destination is typed on a `@Serializable` [Screen] variant.
 * `navController.navigate(Screen.QuizDetail(quizId = 3))` is verified by the
 * compiler. `backStackEntry.toRoute<Screen.QuizDetail>()` deserialises the
 * stored route object without any string key lookups.
 *
 * ── Dialog destination ────────────────────────────────────────────────────────
 * [Screen.PreQuizDialog] is declared with `dialog<>` rather than `composable<>`.
 * This renders the destination as a true floating dialog above [QuizDetailScreen],
 * with the host screen remaining visible and interactive beneath the scrim.
 * The system Back button dismisses the dialog by popping it off the back stack —
 * no manual `isVisible` state management required.
 *
 * ── ViewModel scoping ─────────────────────────────────────────────────────────
 * `koinViewModel()` scopes each ViewModel to its back-stack entry. The ViewModel
 * is created when the destination is first composed and cleared when the entry
 * is permanently removed. Rotation and other configuration changes do NOT clear it.
 *
 * @param navController Provided by [com.grammarflow.GrammarFlowApp].
 *                      [rememberNavController] in production; [TestNavHostController]
 *                      in Compose UI tests.
 */
@Composable
fun GrammarFlowNavHost(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = Screen.Splash,
    ) {

        // ── 1. Splash ─────────────────────────────────────────────────────────
        //
        // SplashScreen owns its own ViewModel injection and navigation
        // side-effect internally — the NavHost only wires the navigation callback.
        // The screen pops itself off the back stack so Back from Home exits the app.

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
        //
        // HomeScreen owns its ViewModel via the default koinViewModel() parameter.
        // The NavHost provides the single navigation callback: onQuizSelected.

        composable<Screen.Home> {
            HomeScreen(
                onQuizSelected = { quiz ->
                    navController.navigate(Screen.QuizDetail(quizId = quiz.id.toInt()))
                },
            )
        }

        // ── 3. Quiz Detail ────────────────────────────────────────────────────
        //
        // Route argument:   quizId: Int
        // Extraction:       backStackEntry.toRoute<Screen.QuizDetail>()
        //
        // QuizDetailViewModel bridges the thin route argument (quizId) to the
        // full Quiz domain model. koinViewModel() receives the quizId via
        // parametersOf so the VM constructor stays free of Android imports.
        //
        // The detail screen navigates to PreQuizDialog (not QuizEngine directly).
        // The dialog carries the quiz title and time so it can render without
        // an additional DB read.

        composable<Screen.QuizDetail> { backStackEntry ->
            val route: Screen.QuizDetail = backStackEntry.toRoute()

            val viewModel: QuizDetailViewModel = koinViewModel(
                // params[0] → quizId: Long in QuizDetailViewModel constructor
                parameters = { parametersOf(route.quizId.toLong()) },
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Derive navigation callbacks based on the loaded quiz data.
            // The lambdas are only exercised when state is Success, but we
            // define them at this level to keep QuizDetailScreen stateless.
            val onStartQuiz: () -> Unit = {
                if (state is QuizDetailUiState.Success) {
                    val quiz = (state as QuizDetailUiState.Success).quiz
                    navController.navigate(
                        Screen.PreQuizDialog(
                            quizId = quiz.id.toInt(),
                            quizTitle = quiz.title,
                            timeMinutes = quiz.totalTimeInMinutes,
                        )
                    )
                }
            }

            when (state) {
                is QuizDetailUiState.Success -> {
                    QuizDetailScreen(
                        quiz = (state as QuizDetailUiState.Success).quiz,
                        onStartQuiz = onStartQuiz,
                        onNavigateBack = { navController.navigateUp() },
                    )
                }

                // Loading and Error are handled inside QuizDetailScreen itself;
                // pass a temporary empty-state placeholder until the VM resolves.
                // In a richer implementation, add Loading/Error branches here
                // to show a full-screen skeleton or error before QuizDetailScreen mounts.
                else -> {
                    QuizDetailScreen(
                        quiz = Quiz(
                            id = 0L,
                            title = "",
                            description = "",
                            totalTimeInMinutes = 0,
                            difficulty = Difficulty.Medium,
                        ),
                        onStartQuiz = {},
                        onNavigateBack = { navController.navigateUp() },
                    )
                }
            }
        }

        // ── 4. Pre-Quiz Dialog ────────────────────────────────────────────────
        //
        // Declared with `dialog<>` — renders as a floating overlay on top of
        // QuizDetailScreen. QuizDetailScreen remains visible behind the scrim.
        //
        // Route arguments:
        //   quizId      : Int     — used to navigate to QuizEngine on confirm
        //   quizTitle   : String  — displayed in the sheet header
        //   timeMinutes : Int     — shown in the timer warning copy
        //
        // No ViewModel needed — the dialog is purely presentational. All data
        // arrives via the route and all actions are navigation callbacks.
        //
        // On confirm  → pop the dialog and navigate to QuizEngine.
        // On dismiss  → popBackStack() removes the dialog, revealing QuizDetail.

        dialog<Screen.PreQuizDialog> { backStackEntry ->
            val route: Screen.PreQuizDialog = backStackEntry.toRoute()

            PreQuizDialog(
                quizTitle = route.quizTitle,
                timeMinutes = route.timeMinutes,
                onConfirm = {
                    // Pop the dialog first so QuizEngine is on top of Detail, not Dialog.
                    navController.popBackStack()
                    navController.navigate(
                        Screen.QuizEngine(
                            quizId = route.quizId,
                            timeInMinutes = route.timeMinutes,
                        )
                    )
                },
                onDismiss = {
                    navController.popBackStack()
                },
            )
        }

        // ── 5. Quiz Engine ────────────────────────────────────────────────────
        //
        // Route arguments:
        //   quizId        : Int  — identifies which questions to load
        //   timeInMinutes : Int  — forwarded from the route so the VM has the
        //                         correct budget without an additional DB read
        //
        // Extraction:  backStackEntry.toRoute<Screen.QuizEngine>()
        //
        // Both args are passed to QuizEngineViewModel via parametersOf.
        // The VM constructor is Long + Int (not Int + Int) because Room PKs are
        // Long — the explicit .toLong() cast is the sole adaptation point.
        //
        // On quiz completion the screen calls onQuizFinished, which navigates to
        // Results and pops QuizEngine so Back from Results returns to QuizDetail.

        composable<Screen.QuizEngine> { backStackEntry ->
            val route: Screen.QuizEngine = backStackEntry.toRoute()

            QuizEngineScreen(
                quizId = route.quizId.toLong(),
                totalTimeInMinutes = route.timeInMinutes,
                onQuizFinished = { score, total ->
                    navController.navigate(Screen.Results(score = score, total = total)) {
                        // Remove QuizEngine from the stack — Back from Results
                        // should return to QuizDetail, not re-enter the quiz.
                        popUpTo<Screen.QuizEngine> { inclusive = true }
                    }
                },
            )
        }

        // ── 6. Results ────────────────────────────────────────────────────────
        //
        // Route arguments (baked in, no DB read required):
        //   score : Int
        //   total : Int
        //
        // Two navigation paths available to the user:
        //   "Return to Dashboard"  → clears the quiz back stack, re-anchors on Home.
        //   "Try Another Quiz"     → same callback for now; can diverge if needed.

        composable<Screen.Results> { backStackEntry ->
            val route: Screen.Results = backStackEntry.toRoute()

            ResultsScreen(
                score = route.score,
                total = route.total,
                onReturnHome = {
                    navController.navigate(Screen.Home) {
                        // Pop everything above Home (Detail, Engine, Results)
                        // so the back stack is clean for the next quiz session.
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }
    }
}