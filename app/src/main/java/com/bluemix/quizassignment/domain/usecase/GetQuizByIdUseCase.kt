package com.bluemix.quizassignment.domain.usecase

import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.domain.repository.QuizRepository

/**
 * Use Case: Fetch a single [Quiz] by its primary key.
 *
 * ── Why fetch-all and filter instead of a dedicated DAO query? ────────────────
 * The [QuizRepository] contract intentionally exposes only [getAllQuizzes] and
 * [getQuestionsForQuiz] — a deliberate YAGNI decision made in the data layer.
 * Rather than adding a `getQuizById` method to the repository (which would
 * require a new DAO query, entity mapping, and interface change), we reuse the
 * already-cached list from Room's query and filter in memory.
 *
 * For GrammarFlow's dataset size (< 20 quizzes), in-memory filtering is
 * indistinguishable from a direct indexed lookup. When the dataset grows large
 * enough to warrant a dedicated `getQuizById(id: Long): Quiz?` repository
 * method, the change is confined to:
 *   1. [QuizRepository] interface
 *   2. [QuizRepositoryImpl] implementation
 *   3. [QuizDao] with a new `@Query`
 *   4. This use case body (three lines)
 *
 * ── Null vs exception ─────────────────────────────────────────────────────────
 * Returns `null` instead of throwing if the ID is not found. The caller
 * ([QuizDetailViewModel]) maps `null` to an error UI state — no try/catch
 * needed at the call site.
 *
 * @param repository The [QuizRepository] contract injected by Koin.
 */
class GetQuizByIdUseCase(
    private val repository: QuizRepository,
) {
    /**
     * Fetches the [Quiz] matching [quizId], or `null` if not found.
     *
     * @param quizId The primary key to look up. Must be > 0.
     * @return       The matching [Quiz], or `null` if [quizId] is invalid or absent.
     */
    suspend operator fun invoke(quizId: Long): Quiz? =
        repository.getAllQuizzes().firstOrNull { it.id == quizId }
}