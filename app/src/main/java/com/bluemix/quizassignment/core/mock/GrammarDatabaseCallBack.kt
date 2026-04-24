package com.bluemix.quizassignment.core.mock

import com.bluemix.quizassignment.data.local.dao.QuizDao
import com.bluemix.quizassignment.data.local.entity.QuestionEntity
import com.bluemix.quizassignment.data.local.entity.QuizEntity
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Room [RoomDatabase.Callback] responsible for pre-populating the GrammarFlow
 * database with curated grammar quiz content on first install.
 *
 * ── Lifecycle ─────────────────────────────────────────────────────────────────
 * [onCreate] fires exactly once — when Room creates the SQLite file for the
 * first time (fresh install or after the user clears app data). It does NOT
 * fire on subsequent app launches.
 *
 * ── Threading ─────────────────────────────────────────────────────────────────
 * Room calls [onCreate] on a background thread, but we still delegate all
 * suspend DAO calls to [seedScope] (provided by Koin's [DatabaseModule]) so
 * that the coroutine lifetime is controlled at the DI layer — not hard-coded
 * to [kotlinx.coroutines.GlobalScope].
 *
 * Using [SupervisorJob] (set up in [DatabaseModule]) ensures a failure seeding
 * questions does not cancel the quiz-insert job and vice versa.
 *
 * ── Idempotency ──────────────────────────────────────────────────────────────
 * All DAO inserts use [androidx.room.OnConflictStrategy.IGNORE], so running
 * the seeder twice (e.g., in tests) produces no duplicate rows and throws no
 * exceptions.
 *
 * @param daoProvider  Lambda returning the [QuizDao] after the DB is built.
 *                     A lambda is used instead of passing the DAO directly
 *                     to break the circular dependency:
 *                     AppDatabase → Callback → DAO → AppDatabase.
 * @param seedScope    External [CoroutineScope] provided by Koin.
 *                     Survives configuration changes and screen navigation.
 */
class GrammarDatabaseCallback(
    private val daoProvider: () -> QuizDao,
    private val seedScope: CoroutineScope,
) : RoomDatabase.Callback() {

    /**
     * Called by Room the very first time the database file is created.
     * Launches the seeder on [seedScope] and returns immediately so Room's
     * internal initialisation is not blocked.
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedScope.launch {
            val dao = daoProvider()
            seedQuizzes(dao)
            // Questions must be inserted AFTER quizzes — FK constraint on quiz_id.
            seedQuestions(dao)
        }
    }

    private suspend fun seedQuizzes(dao: QuizDao) {
        dao.insertQuizzes(
            listOf(
                QuizEntity(
                    id = 1L,
                    title = "Nouns & Pronouns",
                    description = "Master the building blocks of every sentence — " +
                            "common, proper, abstract nouns and the pronouns that replace them.",
                    totalTimeInMinutes = 8,
                    difficulty = "EASY",
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

    // ─────────────────────────────────────────────────────────────────────────
    // Question seed data
    //
    // Naming convention for IDs:
    //   Quiz 1 → questions  1– 4
    //   Quiz 2 → questions  5– 8
    //   Quiz 3 → questions  9–12
    //   Quiz 4 → questions 13–16
    // ─────────────────────────────────────────────────────────────────────────

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
                    id = 16L,
                    quizId = 4L,
                    questionText = "What is the function of the relative clause in: " +
                            "\"The book that you recommended changed my life\"?",
                    options = listOf(
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