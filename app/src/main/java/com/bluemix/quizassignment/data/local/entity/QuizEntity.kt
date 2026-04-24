package com.bluemix.quizassignment.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that maps 1-to-1 with the `quiz` table.
 *
 * Intentionally kept flat (no nested objects) so Room can handle it
 * without extra TypeConverters beyond the ones needed for [QuestionEntity].
 *
 * [difficulty] is stored as a String label ("EASY" / "MEDIUM" / "HARD")
 * and converted to the sealed class [com.grammarflow.domain.model.Difficulty]
 * inside the mapper — keeping serialisation concerns out of the domain.
 */
@Entity(tableName = "quiz")
data class QuizEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "total_time_minutes")
    val totalTimeInMinutes: Int,

    /** Stored as a plain label; the mapper converts it to the sealed Difficulty type. */
    @ColumnInfo(name = "difficulty")
    val difficulty: String,
)
