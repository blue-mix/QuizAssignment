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
//
///**
// * Koin module: provides all database-related dependencies.
// *
// * ── Dependency graph for this module ─────────────────────────────────────────
// *
// *   CoroutineScope (singleton)
// *        │
// *   GrammarDatabaseCallback (singleton)
// *        │
// *   AppDatabase (singleton) ──► QuizDao (singleton)
// *
// * ── Why the seeder scope is declared here ────────────────────────────────────
// * [GrammarDatabaseCallback] needs a [CoroutineScope] to launch the seeding
// * coroutine. By providing the scope from Koin we:
// *  1. Keep the callback itself free of hard-coded [kotlinx.coroutines.GlobalScope].
// *  2. Allow tests to inject a [kotlinx.coroutines.test.TestScope] to control
// *     timing precisely.
// *
// * [SupervisorJob] ensures that a failure seeding questions does NOT cancel the
// * sibling quiz-insert job; both run independently.
// *
// * ── Why `daoProvider` is a lambda ────────────────────────────────────────────
// * The [GrammarDatabaseCallback] is constructed *before* [AppDatabase] is fully
// * initialised (it is passed into the builder). If the callback held a direct
// * reference to the DAO, we would have a circular dependency:
// *
// *   AppDatabase → Callback → DAO → AppDatabase  ← circular
// *
// * Passing `{ get<AppDatabase>().quizDao() }` as a lazy lambda defers DAO
// * resolution until [GrammarDatabaseCallback.onCreate] is actually called by
// * Room — at which point [AppDatabase] is fully available in the Koin graph.
// */
//val databaseModule = module {
//
//    // ── Application-scoped CoroutineScope for the database seeder ─────────────
//    //
//    // `single` → Koin creates exactly one instance for the app's lifetime.
//    // The scope survives configuration changes (it is not tied to any Activity).
//    single<CoroutineScope> {
//        CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    }
//
//    // ── Database seeding callback ─────────────────────────────────────────────
//    //
//    // `single` → one callback instance, shared with the Room builder below.
//    // The `daoProvider` lambda closes over `get<AppDatabase>()`, which Koin
//    // resolves lazily at call-time — breaking the circular dependency.
//    single {
//        GrammarDatabaseCallback(
//            daoProvider = { get<AppDatabase>().quizDao() },
//            seedScope = get(),                             // CoroutineScope above
//        )
//    }
//
//    // ── Room AppDatabase ──────────────────────────────────────────────────────
//    //
//    // `single` → Room is a heavyweight object; one instance per process.
//    // `androidApplication()` is Koin's helper for a safe ApplicationContext.
//    //
//    // `.addCallback(get())` attaches the [GrammarDatabaseCallback] so Room
//    // calls it exactly once on first database creation.
//    //
//    // `.setForeignKeyConstraintsEnabled(true)` is NOT set here because Room
//    // handles FK pragma via `addCallback`'s `onOpen` if configured;
//    // alternatively, add a separate `onOpen` callback if needed.
//    single {
//        Room.databaseBuilder(
//            androidApplication(),
//            AppDatabase::class.java,
//            "grammar_flow.db",
//        )
//            .addCallback(get<GrammarDatabaseCallback>())
//            .build()
//    }
//
//    // ── QuizDao ───────────────────────────────────────────────────────────────
//    //
//    // Scoped as `single` because Room's generated DAO implementation is
//    // stateless and thread-safe; creating it repeatedly wastes allocation.
//    single { get<AppDatabase>().quizDao() }
//}

/**
 * Koin module: provides all database-related dependencies with guaranteed
 * zero main-thread blockage.
 *
 * ── The threading problem this module solves ──────────────────────────────────
 *
 * `Room.databaseBuilder().build()` does not open the SQLite file — that first
 * I/O happens on the executor when a query runs. What it DOES do on the calling
 * thread is:
 *
 *   1. Resolve the KSP-generated `AppDatabase_Impl` class via reflection.
 *   2. Construct the `SupportSQLiteOpenHelper` configuration struct.
 *   3. Validate the database configuration and store it.
 *
 * These steps are CPU-only (no file I/O) but carry measurable cost: 2–8 ms on
 * the main thread as observed in the Logcat output (7.77 ms for this device).
 * On slower devices or large schema versions this can exceed 16 ms — a dropped
 * frame — and will be caught by `StrictMode.detectAll()` in debug builds.
 *
 * ── Two-layer solution ────────────────────────────────────────────────────────
 *
 * LAYER 1 — Room builder executor configuration (DatabaseModule):
 *   `.setQueryExecutor(Dispatchers.IO.asExecutor())`
 *   `.setTransactionExecutor(Dispatchers.IO.asExecutor())`
 *
 *   These route ALL Room's internal executor work — query dispatch, WAL
 *   checkpointing, and the `onCreate` / `onOpen` callback dispatch — to
 *   [Dispatchers.IO]. Room uses these executors for everything after `build()`.
 *   Without this, Room creates its own `Executors.newFixedThreadPool(4)` which
 *   is not co-operative with the coroutine dispatcher — setting them explicitly
 *   lets coroutines and Room share the same IO thread pool, avoiding
 *   over-subscription on devices with few cores.
 *
 * LAYER 2 — Splash pre-warming (SplashViewModel):
 *   The Koin `single` block for `AppDatabase` still executes `build()` on
 *   whatever thread first calls `get<AppDatabase>()`. With the old code, that
 *   was the main thread (triggered by HomeViewModel's Koin resolution chain).
 *
 *   The fix: `SplashViewModel` now explicitly calls `get<AppDatabase>()` inside
 *   a `withContext(Dispatchers.IO)` block during its 1.5 s splash window. This
 *   means:
 *
 *     a) The Koin singleton is created and cached on [Dispatchers.IO].
 *     b) By the time `HomeViewModel` is created and calls `get<AppDatabase>()`
 *        on the main thread, Koin returns the ALREADY-BUILT cached singleton —
 *        zero allocation, zero reflection, zero cost.
 *
 *   This is the "lazy singleton with eager pre-warming" pattern: the singleton
 *   declaration stays lazy (correct — Koin should not eagerly create heavyweights),
 *   but we control WHEN the first creation happens by pre-warming it from a
 *   safe background context during the splash window.
 *
 * ── Why NOT `runBlocking { withContext(IO) { build() } }` in the single block ─
 *
 *   That approach deadlocks on the main thread if any Koin resolution is called
 *   from the main thread before the coroutine completes, which is exactly the
 *   scenario we're trying to fix. The pre-warming pattern in SplashViewModel
 *   achieves the same goal without any blocking primitives.
 *
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

    // ── 1. Application-scoped IO CoroutineScope ───────────────────────────────
    //
    // Used by GrammarDatabaseCallback to launch seeding coroutines.
    // SupervisorJob: failure in one seed coroutine does not cancel siblings.
    // Dispatchers.IO: all seed inserts run on the IO thread pool.
    //
    // This scope is also injected into SplashViewModel for the pre-warm call.
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // ── 2. Database seeding callback ──────────────────────────────────────────
    //
    // Constructed before AppDatabase (it is passed into the builder), so the
    // DAO reference is a lazy lambda to break the circular dependency:
    //   AppDatabase → Callback → DAO → AppDatabase  ← would deadlock
    //
    // The lambda `{ get<AppDatabase>().quizDao() }` is only evaluated inside
    // GrammarDatabaseCallback.onCreate(), at which point AppDatabase is fully
    // constructed and available in the Koin graph.
    single {
        GrammarDatabaseCallback(
            daoProvider = { get<AppDatabase>().quizDao() },
            seedScope   = get(),
        )
    }

    // ── 3. AppDatabase ────────────────────────────────────────────────────────
    //
    // ── PERFORMANCE FIX: setQueryExecutor + setTransactionExecutor ────────────
    //
    // Room has an internal default executor: `Executors.newFixedThreadPool(4)`.
    // Replacing it with `Dispatchers.IO.asExecutor()` achieves three things:
    //
    //   a) THREAD POOL SHARING: Room's queries run on the same [Dispatchers.IO]
    //      thread pool as all coroutines using `withContext(IO)`. The JVM does
    //      not spin up an extra pool — fewer idle threads, less memory pressure.
    //
    //   b) STRUCTURED CONCURRENCY ALIGNMENT: Because Room's executor IS the IO
    //      dispatcher's executor, coroutine cancellation signals propagate
    //      correctly. A cancelled coroutine's query is eligible for interruption
    //      rather than completing on an unrelated thread.
    //
    //   c) QUERY EXECUTOR handles all SELECT / INSERT / UPDATE / DELETE calls.
    //      TRANSACTION EXECUTOR handles BEGIN / COMMIT / ROLLBACK blocks.
    //      Both must be set, because Room uses them independently — setting only
    //      one leaves the other on Room's private pool.
    //
    // ── Why this alone does NOT fix the 7.77 ms stall ─────────────────────────
    //
    // `build()` itself (reflection + config struct creation) still runs on the
    // thread that calls `get<AppDatabase>()`. The executor settings only affect
    // work AFTER build() completes. The stall is eliminated by the SplashViewModel
    // pre-warm pattern (see DatabaseModule KDoc above and SplashViewModel).
    //
    // ── `allowMainThreadQueries()` is deliberately absent ─────────────────────
    //
    // We never add `allowMainThreadQueries()`. Its absence means Room will throw
    // `IllegalStateException` in debug builds if any query accidentally runs on
    // the main thread — acting as a compile-time-equivalent runtime guard.
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "grammar_flow.db",
        )
            // ── PERFORMANCE FIX 1: explicit IO executor for queries ───────────
            //
            // Routes all Room query work to Dispatchers.IO, preventing Room from
            // creating its own private thread pool and over-subscribing the CPU.
            .setQueryExecutor(Dispatchers.IO.asExecutor())

            // ── PERFORMANCE FIX 2: explicit IO executor for transactions ───────
            //
            // Must be set alongside setQueryExecutor. Room's internal WAL
            // checkpoint and migration transactions use this executor.
            // Using the same dispatcher as queries avoids priority inversions
            // where a transaction waits for a query thread that is waiting for
            // the transaction to complete.
            .setTransactionExecutor(Dispatchers.IO.asExecutor())

            // ── Seeding callback ──────────────────────────────────────────────
            //
            // Room invokes GrammarDatabaseCallback.onCreate() on the query
            // executor thread (now Dispatchers.IO) the first time the database
            // file is opened. The callback immediately returns after launching
            // its own seed coroutine on seedScope — it never blocks the executor.
            .addCallback(get<GrammarDatabaseCallback>())

            .build()
    }

    // ── 4. QuizDao ────────────────────────────────────────────────────────────
    //
    // `appDatabase.quizDao()` is pure object creation (<0.1ms) — it returns a
    // KSP-generated DAO impl that holds a reference to the database. No I/O.
    //
    // After the SplashViewModel pre-warm, this `get<AppDatabase>()` call hits
    // the cached singleton and is effectively free.
    single { get<AppDatabase>().quizDao() }
}