package com.bluemix.quizassignment.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity that maps 1-to-1 with the `question` table.
 *
 * Design decisions:
 *  1. [ForeignKey] with [ForeignKey.CASCADE] on delete — removing a quiz
 *     automatically removes all its questions. No orphan cleanup needed.
 *  2. [Index] on [quizId] — Room queries `WHERE quiz_id = ?` frequently;
 *     the index makes those scans O(log n) instead of full table scans.
 *  3. [options] is a `List<String>` serialised via [StringListConverter] so
 *     we avoid a separate join-table for a simple ordered list.
 */
@Entity(
    tableName = "question",
    foreignKeys = [
        ForeignKey(
            entity        = QuizEntity::class,
            parentColumns = ["id"],
            childColumns  = ["quiz_id"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["quiz_id"])
    ],
)
data class QuestionEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Maps to the parent QuizEntity's primary key. */
    @ColumnInfo(name = "quiz_id")
    val quizId: Long,

    @ColumnInfo(name = "question_text")
    val questionText: String,

    /**
     * Serialised JSON array, e.g. ["They run","He run","She runs","It runned"].
     * Conversion is handled by [StringListConverter].
     */
    @ColumnInfo(name = "options")
    val options: List<String>,

    /** Zero-based index into [options] pointing to the correct answer. */
    @ColumnInfo(name = "correct_answer_index")
    val correctAnswerIndex: Int,
)