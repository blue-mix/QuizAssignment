package com.bluemix.quizassignment.domain.usecase

import com.bluemix.quizassignment.domain.model.Question
import com.bluemix.quizassignment.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use Case: Observe the ordered list of questions for a specific quiz.
 *
 * ── Why Flow and not suspend? ─────────────────────────────────────────────────
 * The question list is a reactive data source. Room re-emits it whenever the
 * `question` table changes (e.g., remote sync, dev-time re-seeding). Returning
 * a [Flow] means the Quiz screen updates automatically without a manual refresh.
 *
 * ── Validation responsibility ─────────────────────────────────────────────────
 * Input validation (e.g., rejecting a negative `quizId`) lives here — not in
 * the ViewModel, not in the repository. The domain layer owns business rules.
 * We emit an empty list for invalid IDs rather than throwing, which is a
 * deliberate offline-first decision: bad IDs should never crash the app.
 *
 * ── Future extensibility ──────────────────────────────────────────────────────
 * Shuffling question order, filtering by difficulty, or limiting count to a
 * "practice mode" subset are all single-line additions inside `invoke` below.
 *
 * @param repository The [QuizRepository] contract injected by Koin.
 */
class GetQuizQuestionsUseCase(
    private val repository: QuizRepository,
) {
    /**
     * Returns a cold [Flow] that emits the current question list for [quizId]
     * and re-emits on every subsequent database change.
     *
     * Not `suspend` — [Flow] builders are not blocking. The ViewModel simply
     * calls `invoke(id).collect { … }` inside its own scope.
     *
     * @param quizId  The ID of the quiz whose questions should be observed.
     *                Must be > 0; negative or zero IDs emit an empty list.
     * @return        A [Flow] of [Question] lists. Guaranteed non-null; emits
     *                an empty list rather than throwing on invalid [quizId].
     */
    operator fun invoke(quizId: Long): Flow<List<Question>> {
        // Guard: invalid IDs should not reach the database layer.
        if (quizId <= 0L) return kotlinx.coroutines.flow.flowOf(emptyList())

        return repository
            .getQuestionsForQuiz(quizId)
            // Hook point: add .map { questions -> questions.shuffled() } here
            // to randomise question order for a "practice mode" in the future.
            .map { questions -> questions }   // identity — documents the hook
    }
}