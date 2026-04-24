package com.bluemix.quizassignment.data.repository

import com.bluemix.quizassignment.data.local.dao.QuizDao
import com.bluemix.quizassignment.data.local.ext.toDomain
import com.bluemix.quizassignment.domain.model.Question
import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [QuizRepository].
 *
 * Lives in the Data layer. The Domain layer knows nothing about this class —
 * only about the [QuizRepository] interface it satisfies.
 *
 * Dependency injection (Hilt / Koin / manual) should provide a single instance
 * of this class bound to the [QuizRepository] interface.
 *
 * @param quizDao The Room DAO used to perform all database operations.
 *                Injected rather than created internally to keep this class
 *                testable: swap [QuizDao] for a fake in unit tests.
 */
class QuizRepositoryImpl(
    private val quizDao: QuizDao,
) : QuizRepository {

    /**
     * Fetches all quizzes from the local database and maps them to domain models.
     *
     * The `suspend` modifier delegates the threading decision to the caller.
     * A ViewModel would typically collect this inside `viewModelScope` on the
     * default dispatcher, while a test can call it directly inside
     * `runTest { }` without any special setup.
     *
     * @return An immutable list of [Quiz] domain objects, ordered by ID.
     *         Returns an empty list (not null) if the table is empty.
     */
    override suspend fun getAllQuizzes(): List<Quiz> =
        quizDao
            .getAllQuizzes()   // List<QuizEntity>
            .toDomain()        // List<Quiz>  — via QuizMapper.kt

    /**
     * Returns a cold [Flow] of questions for the given [quizId].
     *
     * The Flow is backed by Room's invalidation tracker:
     *  - A new list is emitted immediately on first collection (initial query).
     *  - Subsequent emissions happen automatically whenever the `question` table
     *    changes, with no polling or manual refresh needed.
     *
     * The [Flow.map] operator transforms [QuestionEntity] → [Question] lazily,
     * only when a downstream collector is active — no wasted work.
     *
     * @param quizId The [Quiz.id] whose questions should be observed.
     * @return       A [Flow] that emits a fresh [List]<[Question]> on every
     *               relevant DB change. Never emits null; emits an empty list
     *               if no questions exist for [quizId].
     */
    override fun getQuestionsForQuiz(quizId: Long): Flow<List<Question>> =
        quizDao
            .getQuestionsForQuiz(quizId)          // Flow<List<QuestionEntity>>
            .map { entities -> entities.toDomain() } // Flow<List<Question>>
}