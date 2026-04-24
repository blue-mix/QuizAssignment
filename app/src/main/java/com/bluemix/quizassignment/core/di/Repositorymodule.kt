package com.bluemix.quizassignment.core.di

import com.bluemix.quizassignment.data.repository.QuizRepositoryImpl
import com.bluemix.quizassignment.domain.repository.QuizRepository
import org.koin.dsl.module

/**
 * Koin module: binds the concrete [QuizRepositoryImpl] to the domain's
 * [QuizRepository] interface.
 *
 * ── Dependency Inversion in practice ─────────────────────────────────────────
 * The `bind` DSL keyword is the Koin idiom for interface binding. When any
 * component requests a [QuizRepository] from the Koin graph, it receives a
 * [QuizRepositoryImpl] — without ever importing the concrete class.
 *
 * ViewModels and Use Cases depend only on [QuizRepository]. Swapping the
 * implementation (e.g., for a remote-backed repo in a future sprint) is a
 * one-line change confined to this file.
 *
 * ── Why `single` and not `factory`? ──────────────────────────────────────────
 * [QuizRepositoryImpl] holds no mutable state of its own (it delegates
 * everything to the DAO). A singleton avoids unnecessary allocation on every
 * ViewModel creation while keeping the semantics correct.
 */
val repositoryModule = module {

    single<QuizRepository> {
        QuizRepositoryImpl(
            quizDao = get(),  // QuizDao — resolved from databaseModule
        )
    }
}