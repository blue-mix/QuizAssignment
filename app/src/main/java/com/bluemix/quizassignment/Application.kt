package com.bluemix.quizassignment

import android.app.Application
import com.bluemix.quizassignment.core.di.databaseModule
import com.bluemix.quizassignment.core.di.domainModule
import com.bluemix.quizassignment.core.di.repositoryModule
import com.bluemix.quizassignment.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point for GrammarFlow.
 *
 * ── Responsibilities ──────────────────────────────────────────────────────────
 * 1. Boot the Koin dependency graph exactly once, before any Activity starts.
 * 2. Provide [android.content.Context] to modules that need it (Room builder,
 *    Koin-Android ViewModel integration).
 * 3. Register every Koin module in clean-architecture dependency order.
 *
 * ── Module loading order ──────────────────────────────────────────────────────
 * Koin resolves the graph lazily — order inside `modules()` is irrelevant for
 * correctness. We list them bottom-up to mirror the Clean Architecture layering
 * visually and make the graph self-documenting:
 *
 *   databaseModule    →  Room DB, DAOs, seeding scope
 *   repositoryModule  →  QuizRepositoryImpl bound to QuizRepository interface
 *   domainModule      →  Use Cases (depend on QuizRepository interface only)
 *   viewModelModule   →  ViewModels (depend on Use Cases only)
 */
class GrammarFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
    /**
     * Bootstraps the Koin container.
     *
     * Extracted to a named function so it can be overridden in test subclasses
     * (e.g., to swap [databaseModule] for an in-memory Room configuration):
     *
     *   class TestGrammarFlowApplication : GrammarFlowApplication() {
     *       override fun initKoin() {
     *           startKoin {
     *               androidContext(this@TestGrammarFlowApplication)
     *               modules(testDatabaseModule, repositoryModule, domainModule, viewModelModule)
     *           }
     *       }
     *   }
     */
    protected open fun initKoin() {
        startKoin {
            // Ties Koin's logger output to Android Logcat.
            androidLogger( Level.DEBUG )

            // Provides androidContext() and androidApplication() in all modules.
            // Must be the real ApplicationContext — never an Activity context.
            androidContext(this@GrammarFlowApplication)

            modules(
                databaseModule,    // AppDatabase, QuizDao, GrammarDatabaseCallback, seeding scope
                repositoryModule,  // QuizRepositoryImpl  →  QuizRepository interface
                domainModule,      // GetAvailableQuizzesUseCase, GetQuizByIdUseCase, GetQuizQuestionsUseCase
                viewModelModule,   // SplashViewModel, HomeViewModel, QuizDetailViewModel, QuizEngineViewModel
            )
        }
    }
}