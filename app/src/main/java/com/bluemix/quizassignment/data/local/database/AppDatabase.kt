package com.bluemix.quizassignment.data.local.database

import com.bluemix.quizassignment.core.utils.StringListConverter
import com.bluemix.quizassignment.data.local.dao.QuizDao
import com.bluemix.quizassignment.data.local.entity.QuestionEntity
import com.bluemix.quizassignment.data.local.entity.QuizEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The single Room database for the GrammarFlow application.
 *
 * ── Responsibilities ──────────────────────────────────────────────────────────
 * This class is intentionally minimal — it only:
 *  1. Declares the entity set and schema version.
 *  2. Exposes DAO accessors.
 *  3. Registers [TypeConverter] classes.
 *
 * Construction, lifecycle, and the seeding [GrammarDatabaseCallback] are all
 * managed by [com.grammarflow.di.DatabaseModule] via Koin. The @Database class
 * itself knows nothing about DI, coroutines, or seeding data.
 *
 * ── Schema export ─────────────────────────────────────────────────────────────
 * `exportSchema = true` instructs Room's annotation processor to write a JSON
 * snapshot of the schema to `app/schemas/`. Commit these files to version
 * control — they are your migration audit trail and allow Room to validate
 * migrations in integration tests via [MigrationTestHelper].
 *
 * ── Version history ──────────────────────────────────────────────────────────
 * Version 1 — Initial schema: `quiz` and `question` tables.
 *             Add a [androidx.room.migration.Migration] object to
 *             [com.grammarflow.di.DatabaseModule] before bumping this number.
 */
@Database(
    entities      = [QuizEntity::class, QuestionEntity::class],
    version       = 1,
    exportSchema  = true,
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to all quiz and question persistence operations.
     *
     * Room generates the concrete implementation at compile time via KSP.
     * Callers should never instantiate [QuizDao] directly.
     */
    abstract fun quizDao(): QuizDao
}