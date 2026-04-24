package com.bluemix.quizassignment.domain.repository

import com.bluemix.quizassignment.domain.model.Question
import com.bluemix.quizassignment.domain.model.Quiz
import kotlinx.coroutines.flow.Flow

/**
 * Contract for all quiz-related data access.
 *
 * The Domain layer owns this interface; the Data layer provides the
 * concrete implementation. This inversion of dependency means:
 *  - ViewModels / UseCases depend only on this interface.
 *  - Room, Retrofit, or any other storage concern can be swapped freely.
 *
 * Naming conventions:
 *  - `suspend fun` → one-shot, caller drives the lifecycle via coroutine scope.
 *  - `Flow<…>`     → reactive stream; Room emits a new list on every DB write.
 */
interface QuizRepository {

    /**
     * Returns a snapshot of every available quiz.
     *
     * Marked `suspend` because fetching from Room (even a simple SELECT *) is a
     * blocking I/O operation that must run off the main thread.
     */
    suspend fun getAllQuizzes(): List<Quiz>

    /**
     * Returns a reactive stream of questions for the given quiz.
     *
     * Using [Flow] here gives the UI automatic updates if questions are ever
     * modified (e.g., admin seeding during development). Room's
     * `@Query` + `Flow` combination handles this for free.
     *
     * @param quizId The [Quiz.id] whose questions should be observed.
     */
    fun getQuestionsForQuiz(quizId: Long): Flow<List<Question>>
}

