package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.LifeStyleEntity
 *
 * Table `TABLE_LIFESTYLE_INFO`, 9 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: life-style suitability rows. `COL_LIFESTYLE_STATES_BY_TIME`
 * is a serialised list — the same JSON-in-a-column choice as the insight table.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_LIFESTYLE_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_LIFESTYLE_TYPE],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyLifeStyleEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_TYPE)
    val type: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_INTERVAL_TYPE)
    val intervalType: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_STATE_TYPE)
    val stateType: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_TITLE_TEXT)
    val titleText: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_DESCRIPTION_TEXT)
    val descriptionText: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_STATE_TEXT)
    val stateText: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_URL)
    val url: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_STATES_BY_TIME)
    val statesByTime: String = "",
)
