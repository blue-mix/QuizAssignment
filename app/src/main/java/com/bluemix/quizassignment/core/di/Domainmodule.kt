package com.bluemix.quizassignment.core.di

import com.bluemix.quizassignment.domain.usecase.GetAvailableQuizzesUseCase
import com.bluemix.quizassignment.domain.usecase.GetQuizByIdUseCase
import com.bluemix.quizassignment.domain.usecase.GetQuizQuestionsUseCase
import org.koin.dsl.module

/**
 * Koin module: provides all Domain layer Use Cases.
 *
 * ── `factory` vs `single` ─────────────────────────────────────────────────────
 * Use Cases are stateless function objects — `factory` is correct.
 * A new instance per injection site costs negligible memory and prevents
 * any accidental shared-state between ViewModels during integration tests.
 *
 * ── Isolation guarantee ───────────────────────────────────────────────────────
 * This module imports only from `com.grammarflow.domain.*`. It has zero
 * knowledge of `com.grammarflow.data.*`. The [QuizRepository] resolved via
 * `get()` is the domain interface — Koin provides the impl from repositoryModule.
 */
val domainModule = module {

    /**
     * Returns all available quizzes as a one-shot list.
     * Consumed by [HomeViewModel] and internally by [GetQuizByIdUseCase].
     */
    factory {
        GetAvailableQuizzesUseCase(repository = get())
    }

    /**
     * Resolves a single quiz by primary key.
     * Consumed by [com.grammarflow.presentation.detail.QuizDetailViewModel].
     */
    factory {
        GetQuizByIdUseCase(repository = get())
    }

    /**
     * Returns a reactive Flow of questions for a given quizId.
     * Consumed by [com.grammarflow.presentation.quiz.QuizEngineViewModel].
     */
    factory {
        GetQuizQuestionsUseCase(repository = get())
    }
}