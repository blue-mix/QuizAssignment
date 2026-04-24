package com.bluemix.quizassignment.domain.model

/**
 * Pure domain model representing a Grammar Quiz.
 *
 * This class lives exclusively in the Domain layer and has
 * ZERO dependency on Room, Android, or any framework.
 *
 * @param id             Unique identifier for the quiz.
 * @param title          Display title of the quiz (e.g., "Parts of Speech").
 * @param description    Short description shown on the quiz card.
 * @param totalTimeInMinutes Allotted time the user has to complete the quiz.
 * @param difficulty     Difficulty level expressed as a sealed type.
 */
data class Quiz(
    val id: Long,
    val title: String,
    val description: String,
    val totalTimeInMinutes: Int,
    val difficulty: Difficulty,
)

/**
 * Strongly-typed difficulty levels. Using a sealed class instead of a raw
 * string/enum keeps the domain expressive and prevents invalid states.
 */
sealed class Difficulty {
    object Easy   : Difficulty()
    object Medium : Difficulty()
    object Hard   : Difficulty()

    /** Serialisation helper used by the mapper when converting to/from the DB. */
    fun toLabel(): String = when (this) {
        Easy   -> "EASY"
        Medium -> "MEDIUM"
        Hard   -> "HARD"
    }

    companion object {
        fun fromLabel(label: String): Difficulty = when (label.uppercase()) {
            "EASY"   -> Easy
            "HARD"   -> Hard
            else     -> Medium   // safe default
        }
    }
}