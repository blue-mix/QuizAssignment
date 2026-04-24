package com.bluemix.quizassignment.domain.model

/**
 * Pure domain model representing a single MCQ Question.
 *
 * No Room annotations. No Android imports. Domain only.
 *
 * @param id                 Unique identifier for the question.
 * @param quizId             Foreign-key reference to the parent [Quiz].
 * @param questionText       The question stem shown to the user.
 * @param options            Ordered list of answer choices (typically 4 items).
 * @param correctAnswerIndex Zero-based index into [options] identifying the right answer.
 */
data class Question(
    val id: Long,
    val quizId: Long,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
) {
    /**
     * Derived convenience property — returns the text of the correct answer.
     * Business logic lives in the domain, not in ViewModels or UI.
     */
    val correctAnswerText: String
        get() = options[correctAnswerIndex]

    /**
     * Validates that the given answer index is correct.
     * Keeps quiz-evaluation logic close to the model it operates on.
     */
    fun isCorrect(selectedIndex: Int): Boolean = selectedIndex == correctAnswerIndex
}