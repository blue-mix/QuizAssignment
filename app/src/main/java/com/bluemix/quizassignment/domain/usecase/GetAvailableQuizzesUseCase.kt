package com.bluemix.quizassignment.domain.usecase

import com.bluemix.quizassignment.domain.model.Quiz
import com.bluemix.quizassignment.domain.repository.QuizRepository

/**
 * Use Case: Retrieve the full list of available grammar quizzes.
 *
 * ── Why a Use Case instead of calling the repository directly? ───────────────
 * Today this is a thin wrapper. Tomorrow it might filter quizzes by the user's
 * subscription tier, sort by last-attempted date, or exclude completed quizzes.
 * Centralising that logic here means ViewModels never change — only this file.
 *
 * ── `operator fun invoke` ─────────────────────────────────────────────────────
 * Overloading `invoke` allows callers to treat the use case instance as a
 * function: `getAvailableQuizzes()` instead of `getAvailableQuizzes.execute()`.
 * This is idiomatic Kotlin and keeps ViewModels extremely readable.
 *
 * @param repository The [QuizRepository] contract injected by Koin.
 *                   The use case never knows it is talking to Room.
 */
class GetAvailableQuizzesUseCase(
    private val repository: QuizRepository,
) {
    /**
     * Fetches all available quizzes as a one-shot operation.
     *
     * `suspend` propagates the coroutine context from the ViewModel's
     * `viewModelScope` all the way down to the Room query — no thread
     * management needed here.
     *
     * @return An immutable, ordered list of [Quiz] domain models.
     *         Returns an empty list (never null) if no quizzes exist.
     */
    suspend operator fun invoke(): List<Quiz> = repository.getAllQuizzes()
}