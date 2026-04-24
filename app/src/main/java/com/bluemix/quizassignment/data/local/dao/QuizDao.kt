package com.bluemix.quizassignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bluemix.quizassignment.data.local.entity.QuestionEntity
import com.bluemix.quizassignment.data.local.entity.QuizEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for all quiz and question persistence operations.
 *
 * Design rules applied here:
 *  - Every function that returns a one-shot result is `suspend` → caller
 *    must provide a coroutine scope, keeping threading decisions above this layer.
 *  - [getQuestionsForQuiz] returns [Flow] → Room automatically re-emits
 *    whenever the underlying `question` table changes, enabling reactive UI.
 *  - [OnConflictStrategy.IGNORE] on inserts → safe for idempotent seeding;
 *    re-running the callback after a DB wipe won't throw a constraint violation.
 */
@Dao
interface QuizDao {

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the complete list of quizzes ordered by their primary key.
     *
     * One-shot suspend function — the ViewModel collects this once per screen
     * entry. If real-time quiz-list updates are ever needed, change the return
     * type to `Flow<List<QuizEntity>>` without modifying any callers.
     */
    @Query("SELECT * FROM quiz ORDER BY id ASC")
    suspend fun getAllQuizzes(): List<QuizEntity>

    /**
     * Returns a reactive stream of questions for the given [quizId].
     *
     * Room invalidates and re-emits this Flow on every write to the `question`
     * table — ideal for reflecting live seeding or future remote sync updates.
     *
     * @param quizId The primary key of the parent [QuizEntity].
     */
    @Query("SELECT * FROM question WHERE quiz_id = :quizId ORDER BY id ASC")
    fun getQuestionsForQuiz(quizId: Long): Flow<List<QuestionEntity>>

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE  (used for initial DB seeding via RoomDatabase.Callback)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a batch of quizzes.
     *
     * [OnConflictStrategy.IGNORE] ensures this is idempotent — calling it
     * twice (e.g., during testing) won't duplicate rows or throw exceptions.
     *
     * @param quizzes Non-empty list of [QuizEntity] objects to persist.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuizzes(quizzes: List<QuizEntity>)

    /**
     * Inserts a batch of questions.
     *
     * Must be called *after* [insertQuizzes] because of the FK constraint on
     * `quiz_id`. Room enforces FK constraints at runtime when enabled via
     * [androidx.sqlite.db.SupportSQLiteDatabase.setForeignKeyConstraintsEnabled].
     *
     * @param questions Non-empty list of [QuestionEntity] objects to persist.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)
}