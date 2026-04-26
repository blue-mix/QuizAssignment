package com.bluemix.quizassignment.core.mock

import com.bluemix.quizassignment.data.local.dao.QuizDao
import com.bluemix.quizassignment.data.local.entity.QuestionEntity
import com.bluemix.quizassignment.data.local.entity.QuizEntity
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room [RoomDatabase.Callback] responsible for pre-populating the GrammarFlow
 * database with curated grammar quiz content on first install.
 *
 * ── Lifecycle ─────────────────────────────────────────────────────────────────
 * [onCreate] fires exactly once — when Room creates the SQLite file for the
 * first time (fresh install or after the user clears app data).
 *
 * ── Threading contract ────────────────────────────────────────────────────────
 *
 * WHAT THREAD DOES ROOM CALL `onCreate` ON?
 *
 * Room calls [onCreate] (and [onOpen]) on the thread that is executing the
 * very first database operation — specifically, on the thread running inside
 * the executor that was given to `setQueryExecutor()` on the builder.
 *
 * In [DatabaseModule] we set `.setQueryExecutor(Dispatchers.IO.asExecutor())`,
 * so [onCreate] is always called on a [Dispatchers.IO] worker thread.
 *
 * HOWEVER — the threading guarantee changes between Room versions and between
 * "first open" scenarios:
 *
 *   • Standard first open  : Room calls `onCreate` on the query executor thread
 *                             (IO) before returning the connection to the caller.
 *   • Pre-packaged DB      : Room may call `onOpen` synchronously.
 *   • `allowMainThreadQueries()` builds: `onCreate` can fire on the main thread.
 *
 * Because we cannot guarantee the calling context across all Room versions and
 * configurations, the callback applies a DEFENSIVE `withContext(Dispatchers.IO)`
 * around every DAO write operation. This is belt-and-suspenders safety:
 *
 *   - If Room calls `onCreate` on IO (normal case) → `withContext(IO)` is a
 *     no-op context switch (the dispatcher sees the thread is already on IO and
 *     continues inline). Zero overhead.
 *   - If Room calls `onCreate` on an unexpected thread → `withContext(IO)`
 *     reschedules the work onto a proper IO thread, preventing any SQLite
 *     main-thread violation.
 *
 * ── Why `seedScope.launch` + `withContext(IO)` and not just `seedScope.launch(IO)` ─
 *
 * `seedScope.launch(Dispatchers.IO)` would set IO as the *initial* dispatcher
 * for the launched coroutine. That is sufficient for the direct DAO calls, but
 * it does not express the *intent* clearly — a future developer might remove
 * the dispatcher parameter thinking it's redundant because the scope already
 * uses IO. Using `withContext(Dispatchers.IO)` *inside* the seed functions
 * makes the IO guarantee explicit and local to the code that performs I/O:
 *
 *   seedScope.launch {           ← scope provides the lifetime (SupervisorJob)
 *       withContext(IO) {        ← this block explicitly requires IO
 *           dao.insertQuizzes()  ← SQLite write, must be on IO
 *       }
 *   }
 *
 * This also means if a future developer adds a CPU-only preprocessing step
 * before the DAO calls (e.g., JSON parsing), it can live outside `withContext`
 * and run on the scope's default dispatcher without unintentionally blocking IO.
 *
 * ── Idempotency ───────────────────────────────────────────────────────────────
 * All DAO inserts use [androidx.room.OnConflictStrategy.IGNORE]. Running the
 * seeder twice (e.g., in instrumented tests that don't clear the DB between
 * runs) produces no duplicate rows and throws no exceptions.
 *
 * ── Circular dependency prevention ────────────────────────────────────────────
 * [daoProvider] is a lambda rather than a direct DAO reference. It is only
 * evaluated inside [onCreate], at which point `AppDatabase` is fully constructed
 * and present in the Koin graph. Direct injection would cause:
 *   AppDatabase → Callback → DAO → AppDatabase  ← circular, Koin would throw.
 *
 * @param daoProvider  Lambda that calls `get<AppDatabase>().quizDao()` lazily.
 * @param seedScope    Application-scoped [CoroutineScope] from [DatabaseModule].
 *                     Uses [SupervisorJob] so one failing seed task does not
 *                     cancel the others.
 */
class GrammarDatabaseCallback(
    private val daoProvider: () -> QuizDao,
    private val seedScope: CoroutineScope,
) : RoomDatabase.Callback() {

    /**
     * Called by Room the first time the database file is created.
     *
     * Returns immediately after launching the seed coroutine — Room's internal
     * open sequence is never blocked waiting for seed data. The quiz list screen
     * will show a loading state while the seed coroutine runs in the background.
     *
     * @param db The raw [SupportSQLiteDatabase] handle. We do NOT use this
     *           directly — all writes go through the type-safe [QuizDao].
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        seedScope.launch {
            // ── PERFORMANCE FIX: withContext(Dispatchers.IO) ──────────────────
            //
            // Even though DatabaseModule sets setQueryExecutor(IO.asExecutor()),
            // which means Room will call this callback from an IO thread in
            // standard usage, we enforce IO explicitly here as a defensive
            // measure. Reasons:
            //
            //   1. Room's callback threading is an implementation detail, not
            //      a documented API contract. A future Room version could change
            //      the calling thread.
            //
            //   2. Tests using `allowMainThreadQueries()` or in-memory builders
            //      without a custom executor will call onCreate on the test's
            //      calling thread — often the main thread.
            //
            //   3. The `withContext` switch is free when already on IO (the
            //      dispatcher detects the current thread is in the IO pool and
            //      does not reschedule). Cost is zero in production, safety is
            //      guaranteed everywhere.
            //
            // Structure: seedQuizzes runs first and completes before seedQuestions
            // starts. This is required — QuestionEntity has a FK constraint on
            // quiz_id that Room enforces at the SQLite level when FK pragmas are
            // active. Inserting questions before quizzes would violate the constraint.
            withContext(Dispatchers.IO) {
                val dao = daoProvider()  // Koin resolves AppDatabase singleton here

                // ── Step 1: Parent rows first (FK constraint requirement) ──────
                seedQuizzes(dao)

                // ── Step 2: Child rows after parent rows are committed ─────────
                seedQuestions(dao)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Seed functions
    //
    // Both are private suspend functions called from within withContext(IO),
    // so SQLite access is always on a background thread.
    //
    // They are NOT individually wrapped in withContext — the caller's
    // withContext block in onCreate already establishes the IO context for the
    // entire seed sequence. Adding per-function withContext would create
    // unnecessary context-switch overhead (each withContext is a coroutine
    // dispatch point even when the context does not change).
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun seedQuizzes(dao: QuizDao) {
        dao.insertQuizzes(
            listOf(
                QuizEntity(
                    id                 = 1L,
                    title              = "Nouns & Pronouns",
                    description        = "Master the building blocks of every sentence — " +
                            "common, proper, abstract nouns and the pronouns that replace them.",
                    totalTimeInMinutes = 8,
                    difficulty         = "EASY",
                ),
                QuizEntity(
                    id                 = 2L,
                    title              = "Verb Tenses",
                    description        = "From simple present to past perfect continuous — " +
                            "test your command of English tense forms.",
                    totalTimeInMinutes = 12,
                    difficulty         = "MEDIUM",
                ),
                QuizEntity(
                    id                 = 3L,
                    title              = "Articles & Determiners",
                    description        = "Crack the rules behind 'a', 'an', and 'the' — " +
                            "one of the trickiest areas of English grammar.",
                    totalTimeInMinutes = 10,
                    difficulty         = "MEDIUM",
                ),
                QuizEntity(
                    id                 = 4L,
                    title              = "Clauses & Sentence Structure",
                    description        = "Identify independent clauses, subordinate clauses, " +
                            "and complex sentence structures under time pressure.",
                    totalTimeInMinutes = 18,
                    difficulty         = "HARD",
                ),
            )
        )
    }

    private suspend fun seedQuestions(dao: QuizDao) {
        dao.insertQuestions(
            listOf(

                // ── Quiz 1: Nouns & Pronouns ──────────────────────────────

                QuestionEntity(
                    id                 = 1L,
                    quizId             = 1L,
                    questionText       = "Which of the following is an abstract noun?",
                    options            = listOf(
                        "Mountain",
                        "Freedom",
                        "Elephant",
                        "Notebook",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 2L,
                    quizId             = 1L,
                    questionText       = "In the sentence \"Neither the manager nor the employees " +
                            "submitted their reports,\" the pronoun \"their\" refers to:",
                    options            = listOf(
                        "The manager only",
                        "The employees only",
                        "Both the manager and the employees",
                        "An unspecified third party",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 3L,
                    quizId             = 1L,
                    questionText       = "Which sentence uses a collective noun correctly?",
                    options            = listOf(
                        "The flock of birds were flying south.",
                        "A flock of birds was flying south.",
                        "Flock birds is flying south.",
                        "The birds flock fly south.",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 4L,
                    quizId             = 1L,
                    questionText       = "Identify the reflexive pronoun in: " +
                            "\"She taught herself to play the piano.\"",
                    options            = listOf(
                        "She",
                        "taught",
                        "herself",
                        "piano",
                    ),
                    correctAnswerIndex = 2,
                ),

                // ── Quiz 2: Verb Tenses ───────────────────────────────────

                QuestionEntity(
                    id                 = 5L,
                    quizId             = 2L,
                    questionText       = "Which sentence is written in the Past Perfect tense?",
                    options            = listOf(
                        "She was reading the report.",
                        "She read the report yesterday.",
                        "She had read the report before the meeting started.",
                        "She has been reading the report all morning.",
                    ),
                    correctAnswerIndex = 2,
                ),
                QuestionEntity(
                    id                 = 6L,
                    quizId             = 2L,
                    questionText       = "Choose the correct verb form: " +
                            "\"By the time we arrive, the concert ___ already ___.\"",
                    options            = listOf(
                        "will / start",
                        "will have / started",
                        "has / started",
                        "would / start",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 7L,
                    quizId             = 2L,
                    questionText       = "Which of these is a stative verb that should NOT " +
                            "be used in the continuous form?",
                    options            = listOf(
                        "run",
                        "write",
                        "belong",
                        "jump",
                    ),
                    correctAnswerIndex = 2,
                ),
                QuestionEntity(
                    id                 = 8L,
                    quizId             = 2L,
                    questionText       = "Correct the error: \"I am living in London since 2019.\"",
                    options            = listOf(
                        "I was living in London since 2019.",
                        "I have been living in London since 2019.",
                        "I am living in London from 2019.",
                        "No error — the sentence is correct.",
                    ),
                    correctAnswerIndex = 1,
                ),

                // ── Quiz 3: Articles & Determiners ────────────────────────

                QuestionEntity(
                    id                 = 9L,
                    quizId             = 3L,
                    questionText       = "Choose the correct article: " +
                            "\"She is ___ honest woman.\"",
                    options            = listOf(
                        "a",
                        "an",
                        "the",
                        "No article needed",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 10L,
                    quizId             = 3L,
                    questionText       = "Which sentence uses \"the\" correctly?",
                    options            = listOf(
                        "She loves the music in general.",
                        "The gold is a precious metal.",
                        "The Nile is the longest river in Africa.",
                        "He goes to the school every day.",
                    ),
                    correctAnswerIndex = 2,
                ),
                QuestionEntity(
                    id                 = 11L,
                    quizId             = 3L,
                    questionText       = "Fill in the blank: " +
                            "\"He played ___ guitar at ___ concert last night.\"",
                    options            = listOf(
                        "a / the",
                        "the / a",
                        "the / the",
                        "a / a",
                    ),
                    correctAnswerIndex = 2,
                ),
                QuestionEntity(
                    id                 = 12L,
                    quizId             = 3L,
                    questionText       = "In which sentence is the article omitted correctly?",
                    options            = listOf(
                        "She went to the hospital to visit a friend.",
                        "Life is full of the surprises.",
                        "The honesty is the best policy.",
                        "I drink the coffee every morning.",
                    ),
                    correctAnswerIndex = 0,
                ),

                // ── Quiz 4: Clauses & Sentence Structure ──────────────────

                QuestionEntity(
                    id                 = 13L,
                    quizId             = 4L,
                    questionText       = "Identify the dependent clause in: " +
                            "\"Although it was raining, we decided to go hiking.\"",
                    options            = listOf(
                        "we decided to go hiking",
                        "Although it was raining",
                        "to go hiking",
                        "we decided",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 14L,
                    quizId             = 4L,
                    questionText       = "Which of the following is a compound-complex sentence?",
                    options            = listOf(
                        "She sings beautifully.",
                        "She sings and he plays the guitar.",
                        "Because she practised daily, she sings beautifully, " +
                                "and he accompanies her on guitar.",
                        "She sings beautifully because she practised daily.",
                    ),
                    correctAnswerIndex = 2,
                ),
                QuestionEntity(
                    id                 = 15L,
                    quizId             = 4L,
                    questionText       = "Which sentence contains a dangling modifier?",
                    options            = listOf(
                        "Running to catch the bus, Maria dropped her keys.",
                        "Running to catch the bus, the keys were dropped.",
                        "Maria, while running, dropped her keys.",
                        "As Maria ran, her keys fell to the ground.",
                    ),
                    correctAnswerIndex = 1,
                ),
                QuestionEntity(
                    id                 = 16L,
                    quizId             = 4L,
                    questionText       = "What is the function of the relative clause in: " +
                            "\"The book that you recommended changed my life\"?",
                    options            = listOf(
                        "It acts as the subject of the sentence.",
                        "It modifies the noun 'book' by identifying which book is meant.",
                        "It provides additional, non-essential information about the book.",
                        "It functions as an adverb modifying 'changed'.",
                    ),
                    correctAnswerIndex = 1,
                ),
            )
        )
    }
}