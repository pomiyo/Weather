package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.IndexEntity
 *
 * Table `TABLE_LIFE_INDEX_INFO`, 10 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibilities:
 * - one row per measurement, composite key (weather key + type + **category**)
 * - the category in the key is the interesting part: the SAME index type can be
 *   stored more than once under different categories, because
 *   `StudyIndexCategory` is a bitmask of which surfaces want it
 * - `COL_LIFE_INDEX_EXTRA` is INTEGER here although the domain's `extra` is a
 *   string field — a type narrowing the mapper has to handle
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_LIFE_INDEX_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_LIFE_INDEX_TYPE, StudyDbConstants.COL_LIFE_INDEX_CATEGORY],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyIndexEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_TYPE)
    val type: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_TEXT)
    val text: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_VALUE)
    val value: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_PRIORITY)
    val priority: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_LEVEL)
    val level: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_URL)
    val url: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_CATEGORY)
    val category: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_EXTRA)
    val extra: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_LIFE_INDEX_DESCRIPTION)
    val description: String? = null,
)
