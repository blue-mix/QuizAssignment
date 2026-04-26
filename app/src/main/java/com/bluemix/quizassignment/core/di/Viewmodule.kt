package com.bluemix.quizassignment.core.di

import com.bluemix.quizassignment.presentation.viewmodels.HomeViewModel
import com.bluemix.quizassignment.presentation.viewmodels.QuizDetailViewModel
import com.bluemix.quizassignment.presentation.viewmodels.QuizEngineViewModel
import com.bluemix.quizassignment.presentation.viewmodels.SplashViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module: provides all Presentation layer ViewModels.
 *
 * ── `viewModelOf` vs `viewModel { params -> }` ────────────────────────────────
 *
 * [viewModelOf] — constructor DSL. Use when every constructor parameter is
 * already in the Koin graph (no runtime arguments from navigation routes).
 * Koin reflects the constructor and calls `get()` for each param automatically.
 *
 * [viewModel { params -> }] — explicit lambda. Use when the ViewModel needs a
 * mix of graph-resolved dependencies AND runtime parameters supplied by the
 * NavHost via `koinViewModel(parameters = { parametersOf(...) })`.
 *
 * ── SavedStateHandle ─────────────────────────────────────────────────────────
 * Koin-Android automatically injects [SavedStateHandle] into any ViewModel
 * that declares it as a constructor parameter — no explicit registration needed.
 * GrammarFlow's ViewModels deliberately avoid [SavedStateHandle] (they take
 * plain Kotlin types) to stay free of Android imports. The NavHost bridges
 * route arguments via [parametersOf] instead.
 *
 * ── Scoping ───────────────────────────────────────────────────────────────────
 * `koinViewModel()` in Compose scopes each instance to its back-stack entry.
 * Instances are created lazily on first access and cleared when the entry
 * is permanently removed — matching Jetpack's standard ViewModel lifecycle.
 */
val viewModelModule = module {

    viewModelOf(::SplashViewModel)
    viewModelOf(::HomeViewModel)

    viewModel { params ->
        QuizDetailViewModel(
            quizId = params[0],
            getQuizById = get(),
        )
    }
    viewModel { params ->
        QuizEngineViewModel(
            quizId = params[0],
            totalTimeInMinutes = params[1],
            getQuizQuestions = get(),
        )
    }
}