package com.bluemix.quizassignment.data.local.ext

import com.bluemix.quizassignment.data.local.entity.QuestionEntity
import com.bluemix.quizassignment.data.local.entity.QuizEntity
import com.bluemix.quizassignment.domain.model.Difficulty
import com.bluemix.quizassignment.domain.model.Question
import com.bluemix.quizassignment.domain.model.Quiz

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * Mapper Extension Functions
 *
 * Design principles applied:
 *  1. Extension functions on Entity types → mappers are discoverable at the
 *     call-site (`entity.toDomain()`) without needing a Mapper object.
 *  2. Bidirectional mapping (`toDomain` + `toEntity`) → the `toEntity`
 *     direction is included for completeness and future use (e.g., user-created
 *     quizzes, remote sync writes). Only the domain→entity direction is used
 *     during seeding today.
 *  3. Zero business logic here — mappers are pure structural transformations.
 *     Derived properties (e.g., `correctAnswerText`) live on the domain model.
 * ─────────────────────────────────────────────────────────────────────────────
 */

// ─────────────────────────────────────────────────────────────────────────────
// Quiz Mappers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a Room [QuizEntity] to the clean domain [Quiz] model.
 *
 * The [difficulty] string label (e.g., "HARD") is parsed into the sealed
 * [Difficulty] hierarchy here — the domain model never sees raw strings.
 */
fun QuizEntity.toDomain(): Quiz = Quiz(
    id                 = id,
    title              = title,
    description        = description,
    totalTimeInMinutes = totalTimeInMinutes,
    difficulty         = Difficulty.fromLabel(difficulty),
)

/**
 * Converts a domain [Quiz] back to a [QuizEntity] for persistence.
 *
 * Useful when the app later supports user-generated quizzes or receives
 * quizzes from a remote API that must be cached locally.
 */
fun Quiz.toEntity(): QuizEntity = QuizEntity(
    id                 = id,
    title              = title,
    description        = description,
    totalTimeInMinutes = totalTimeInMinutes,
    difficulty         = difficulty.toLabel(),
)

/**
 * Convenience extension to map an entire list in one call.
 * Avoids `entities.map { it.toDomain() }` boilerplate at every call-site.
 */
@JvmName("quizListToDomain")
fun List<QuizEntity>.toDomain(): List<Quiz> = map { it.toDomain() }

// ─────────────────────────────────────────────────────────────────────────────
// Question Mappers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a Room [QuestionEntity] to the clean domain [Question] model.
 *
 * [options] arrives as a `List<String>` (already deserialised by
 * [com.grammarflow.data.local.converter.StringListConverter]) — no additional
 * parsing needed here.
 */
fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    quizId = quizId,
    questionText = questionText,
    options = options,
    correctAnswerIndex = correctAnswerIndex,
)

/**
 * Converts a domain [Question] back to a [QuestionEntity].
 *
 * [options] is passed as-is; Room's TypeConverter handles JSON serialisation
 * transparently when the entity is written to the database.
 */
fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id                  = id,
    quizId              = quizId,
    questionText        = questionText,
    options             = options,
    correctAnswerIndex  = correctAnswerIndex,
)

/**
 * Convenience extension to map an entire list in one call.
 */
@JvmName("questionListToDomain")
fun List<QuestionEntity>.toDomain(): List<Question> = map { it.toDomain() }