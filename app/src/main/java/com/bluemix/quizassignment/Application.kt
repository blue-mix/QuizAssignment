package com.bluemix.quizassignment

import android.app.Application
import com.bluemix.quizassignment.core.di.databaseModule
import com.bluemix.quizassignment.core.di.domainModule
import com.bluemix.quizassignment.core.di.repositoryModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point that bootstraps the Koin dependency graph.
 *
 * ── Registration order matters ────────────────────────────────────────────────
 * Koin resolves dependencies lazily, so declaration order in [loadModules]
 * does not strictly matter — but we list them bottom-up (data → domain → di)
 * to mirror the dependency hierarchy and make the graph readable.
 *
 * ── AndroidLogger level ──────────────────────────────────────────────────────
 * [Level.DEBUG] logs every resolution to Logcat during development.
 * Change to [Level.ERROR] (or omit [androidLogger]) in release builds to
 * eliminate the overhead.
 *
 */
class GrammarFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Ties Koin's logger to Android Logcat.
            androidLogger(Level.DEBUG)

            // Provides `androidContext()` and `androidApplication()` throughout
            // all modules — required for Room's database builder.
            androidContext(this@GrammarFlowApp)

            // Load all modules in a single call for clarity.
            modules(
                databaseModule,    // CoroutineScope, GrammarDatabaseCallback, AppDatabase, QuizDao
                repositoryModule,  // QuizRepositoryImpl bound to QuizRepository
                domainModule,      // GetAvailableQuizzesUseCase, GetQuizQuestionsUseCase
            )
        }
    }
}