package com.bluemix.quizassignment.presentation.navigation

import kotlinx.serialization.Serializable


/**
 * Type-safe navigation route definitions for GrammarFlow.
 *
 * ── Design: sealed interface + @Serializable ──────────────────────────────────
 * Navigation 2.8+ encodes each route object directly into the back stack via
 * kotlinx.serialization. No string routes, no argument bundles, no
 * `arguments?.getString("key")` anywhere in the codebase.
 *
 * `data object`  → zero-argument destinations (no heap allocation per navigate call).
 * `data class`   → argument-carrying destinations; the compiler enforces that every
 *                  required arg is supplied before `navController.navigate()` compiles.
 *
 * ── Primitive argument types ──────────────────────────────────────────────────
 * Navigation's type-safe serializer maps Kotlin primitives to NavType counterparts.
 * We use [Int] for IDs in routes (not [Long]) to stay inside the supported NavType
 * set; the explicit `.toLong()` cast happens at the ViewModel boundary.
 */
sealed interface Screen {

    /** Splash / init screen. Shown once; auto-navigates to [Home]. */
    @Serializable
    data object Splash : Screen

    /** Home dashboard — lists all available quizzes. */
    @Serializable
    data object Home : Screen

    /**
     * Quiz metadata / detail view.
     * Hosts the [PreQuizDialog] as a floating dialog destination above it.
     *
     * @param quizId The quiz's database primary key.
     */
    @Serializable
    data class QuizDetail(val quizId: Int) : Screen

    /**
     * Pre-quiz confirmation dialog, rendered as a `dialog<>` destination
     * floating above [QuizDetail]. Using a dedicated route lets the system
     * back-button dismiss it correctly without bespoke dialog state management.
     *
     * @param quizId      Forwarded so the dialog can show the quiz title / time.
     * @param quizTitle   Display name shown in the sheet header.
     * @param timeMinutes Allotted time shown in the timer warning.
     */
    @Serializable
    data class PreQuizDialog(
        val quizId: Int,
        val quizTitle: String,
        val timeMinutes: Int,
    ) : Screen

    /**
     * Core quiz engine.
     *
     * @param quizId           Identifies which questions to load.
     * @param timeInMinutes    Forwarded from [QuizDetail] so the engine ViewModel
     *                         receives the correct budget without an extra DB read.
     */
    @Serializable
    data class QuizEngine(
        val quizId: Int,
        val timeInMinutes: Int,
    ) : Screen

    /**
     * Results screen. All data is baked into the route — no DB read required.
     *
     * @param score Number of correct answers.
     * @param total Total questions in the quiz.
     */
    @Serializable
    data class Results(val score: Int, val total: Int) : Screen
}