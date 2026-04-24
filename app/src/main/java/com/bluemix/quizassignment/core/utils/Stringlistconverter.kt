package com.bluemix.quizassignment.core.utils

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter] that serialises a [List]<[String]> to a compact JSON
 * string and deserialises it back, using **kotlinx.serialization**.
 *
 * ── Why kotlinx.serialization over Gson? ─────────────────────────────────────
 *  - It is Kotlin-first and fully null-safe by design.
 *  - No [TypeToken] reflection hacks needed for generic types — the reified
 *    inline `decodeFromString<List<String>>()` call resolves generics at
 *    compile time via the serialization plugin.
 *  - Already on the classpath for any project using the Kotlin serialization
 *    Gradle plugin; no extra dependency required.
 *
 * ── Registration ─────────────────────────────────────────────────────────────
 * Reference this class in `@TypeConverters([StringListConverter::class])` on
 * [com.grammarflow.data.local.AppDatabase].
 *
 * ── Thread safety ────────────────────────────────────────────────────────────
 * The [Json] instance is stateless after construction and safe for concurrent
 * use from multiple threads — Room may call converters on a thread pool.
 */
class StringListConverter {

    /**
     * Shared [Json] instance configured for minimal, predictable output.
     *
     * - `encodeDefaults = false`  → omits default values, keeping JSON compact.
     * - `ignoreUnknownKeys = true` → tolerates schema evolution; adding a new
     *   field to the serialised format won't break existing stored data.
     */
    private val json = Json {
        encodeDefaults    = false
        ignoreUnknownKeys = true
    }

    /**
     * Encodes a [List]<[String]> to a compact JSON array string for storage.
     *
     * Example output: `["Subject","Predicate","Object","Modifier"]`
     *
     * The `inline reified` machinery in kotlinx.serialization resolves
     * `List<String>` generics at compile time — no runtime reflection.
     *
     * @param options The MCQ answer options to serialise. May be empty.
     * @return        A JSON-array string; `"[]"` for an empty list.
     */
    @TypeConverter
    fun fromList(options: List<String>): String = json.encodeToString(options)

    /**
     * Decodes a JSON-array string back to a [List]<[String]>.
     *
     * Defensive: returns an empty list for blank or malformed input rather
     * than propagating a crash to the UI layer.
     *
     * @param value A JSON-array string produced by [fromList].
     * @return      The deserialised list; empty list if [value] is blank.
     */
    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<String>>(value) }
            .getOrElse { emptyList() }  // swallow malformed JSON gracefully
}