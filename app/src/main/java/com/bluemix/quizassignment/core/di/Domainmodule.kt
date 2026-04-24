package com.bluemix.quizassignment.core.di

import com.bluemix.quizassignment.domain.usecase.GetAvailableQuizzesUseCase
import com.bluemix.quizassignment.domain.usecase.GetQuizQuestionsUseCase
import org.koin.dsl.module

/**
 * Koin module: provides all Domain layer Use Cases.
 *
 * ── `factory` vs `single` for Use Cases ──────────────────────────────────────
 * Use Cases are declared as `factory` (a new instance per injection) for two
 * reasons:
 *
 *  1. **Purity** — A Use Case is a stateless function object. Sharing a single
 *     instance across ViewModels offers no benefit over creating a fresh one.
 *
 *  2. **Testability** — `factory` prevents one ViewModel's test from
 *     accidentally sharing state with another's when both are live in the
 *     same Koin context during integration tests.
 *
 * If a Use Case ever accumulates state (e.g., an in-memory cache), promote it
 * to `single` at that point — not preemptively.
 *
 * ── Layer isolation ───────────────────────────────────────────────────────────
 * This module only imports Domain layer classes. It never imports anything from
 * `com.grammarflow.data.*`. The [QuizRepository] it resolves via `get()` is
 * the domain interface — Koin satisfies it with [QuizRepositoryImpl] as wired
 * in [repositoryModule], but this module has zero knowledge of that.
 */
val domainModule = module {

    /**
     * Provides a new [GetAvailableQuizzesUseCase] per injection site.
     *
     * Resolved via: domainModule → repositoryModule → databaseModule (QuizDao)
     */
    factory {
        GetAvailableQuizzesUseCase(
            repository = get(),  // QuizRepository interface
        )
    }

    /**
     * Provides a new [GetQuizQuestionsUseCase] per injection site.
     *
     * Resolved via: domainModule → repositoryModule → databaseModule (QuizDao)
     */
    factory {
        GetQuizQuestionsUseCase(
            repository = get(),  // QuizRepository interface
        )
    }
}