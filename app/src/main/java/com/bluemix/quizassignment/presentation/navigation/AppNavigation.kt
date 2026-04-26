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

@SuppressLint("RestrictedApi")
@Composable
fun GrammarFlowNavHost(navController: NavHostController) {

    val currentEntry by navController.currentBackStackEntryAsState()
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