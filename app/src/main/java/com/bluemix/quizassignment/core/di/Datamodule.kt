package com.bluemix.quizassignment.core.di

import androidx.room.Room
import com.bluemix.quizassignment.core.mock.GrammarDatabaseCallback
import com.bluemix.quizassignment.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/**
 * Koin module: provides all database-related dependencies with guaranteed
 * ── Dependency graph ──────────────────────────────────────────────────────────
 *
 *   Dispatchers.IO.asExecutor() ──► Room builder config
 *                                         │
 *   CoroutineScope(SupervisorJob+IO) ─────┤
 *                                         │
 *   GrammarDatabaseCallback ◄─────────────┤
 *   (daoProvider lambda, seedScope)       │
 *                                         ▼
 *                                    AppDatabase (singleton)
 *                                         │
 *                                         ▼
 *                                      QuizDao (singleton)
 */
val databaseModule = module {

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single {
        GrammarDatabaseCallback(
            daoProvider = { get<AppDatabase>().quizDao() },
            seedScope   = get(),
        )
    }

    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "grammar_flow.db",
        )
            .setQueryExecutor(Dispatchers.IO.asExecutor())
            .setTransactionExecutor(Dispatchers.IO.asExecutor())
            .addCallback(get<GrammarDatabaseCallback>())

            .build()
    }
    single { get<AppDatabase>().quizDao() }
}