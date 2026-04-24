package com.bluemix.quizassignment.core.di

import androidx.room.Room
import com.bluemix.quizassignment.core.mock.GrammarDatabaseCallback
import com.bluemix.quizassignment.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/**
 * Koin module: provides all database-related dependencies.
 *
 * ── Dependency graph for this module ─────────────────────────────────────────
 *
 *   CoroutineScope (singleton)
 *        │
 *   GrammarDatabaseCallback (singleton)
 *        │
 *   AppDatabase (singleton) ──► QuizDao (singleton)
 *
 * ── Why the seeder scope is declared here ────────────────────────────────────
 * [GrammarDatabaseCallback] needs a [CoroutineScope] to launch the seeding
 * coroutine. By providing the scope from Koin we:
 *  1. Keep the callback itself free of hard-coded [kotlinx.coroutines.GlobalScope].
 *  2. Allow tests to inject a [kotlinx.coroutines.test.TestScope] to control
 *     timing precisely.
 *
 * [SupervisorJob] ensures that a failure seeding questions does NOT cancel the
 * sibling quiz-insert job; both run independently.
 *
 * ── Why `daoProvider` is a lambda ────────────────────────────────────────────
 * The [GrammarDatabaseCallback] is constructed *before* [AppDatabase] is fully
 * initialised (it is passed into the builder). If the callback held a direct
 * reference to the DAO, we would have a circular dependency:
 *
 *   AppDatabase → Callback → DAO → AppDatabase  ← circular
 *
 * Passing `{ get<AppDatabase>().quizDao() }` as a lazy lambda defers DAO
 * resolution until [GrammarDatabaseCallback.onCreate] is actually called by
 * Room — at which point [AppDatabase] is fully available in the Koin graph.
 */
val databaseModule = module {

    // ── Application-scoped CoroutineScope for the database seeder ─────────────
    //
    // `single` → Koin creates exactly one instance for the app's lifetime.
    // The scope survives configuration changes (it is not tied to any Activity).
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // ── Database seeding callback ─────────────────────────────────────────────
    //
    // `single` → one callback instance, shared with the Room builder below.
    // The `daoProvider` lambda closes over `get<AppDatabase>()`, which Koin
    // resolves lazily at call-time — breaking the circular dependency.
    single {
        GrammarDatabaseCallback(
            daoProvider = { get<AppDatabase>().quizDao() },
            seedScope = get(),                             // CoroutineScope above
        )
    }

    // ── Room AppDatabase ──────────────────────────────────────────────────────
    //
    // `single` → Room is a heavyweight object; one instance per process.
    // `androidApplication()` is Koin's helper for a safe ApplicationContext.
    //
    // `.addCallback(get())` attaches the [GrammarDatabaseCallback] so Room
    // calls it exactly once on first database creation.
    //
    // `.setForeignKeyConstraintsEnabled(true)` is NOT set here because Room
    // handles FK pragma via `addCallback`'s `onOpen` if configured;
    // alternatively, add a separate `onOpen` callback if needed.
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "grammar_flow.db",
        )
            .addCallback(get<GrammarDatabaseCallback>())
            .build()
    }

    // ── QuizDao ───────────────────────────────────────────────────────────────
    //
    // Scoped as `single` because Room's generated DAO implementation is
    // stateless and thread-safe; creating it repeatedly wastes allocation.
    single { get<AppDatabase>().quizDao() }
}