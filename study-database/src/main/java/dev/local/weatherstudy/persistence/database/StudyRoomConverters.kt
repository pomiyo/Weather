package dev.local.weatherstudy.persistence.database

import androidx.room.TypeConverter

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the Room type converters the original's
 * `WeatherDatabase_Impl` applies (the converter class itself is not separately named
 * in the decompiled output).
 *
 * Observed responsibility: two columns hold serialised structures rather than scalars —
 * `COL_INSIGHT_SERIALIZED_JSON` and `COL_LIFESTYLE_STATES_BY_TIME`. Both are TEXT in
 * the schema, which is how the app adds insight kinds and per-time-slot strips without
 * a migration. These converters are the boundary where that choice is made explicit.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyRoomConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString(SEPARATOR).orEmpty()

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.split(SEPARATOR) ?: emptyList()

    @TypeConverter
    fun fromIntList(value: List<Int>?): String = value?.joinToString(SEPARATOR).orEmpty()

    @TypeConverter
    fun toIntList(value: String?): List<Int> =
        value?.takeIf { it.isNotEmpty() }?.split(SEPARATOR)?.mapNotNull { it.toIntOrNull() }
            ?: emptyList()

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
