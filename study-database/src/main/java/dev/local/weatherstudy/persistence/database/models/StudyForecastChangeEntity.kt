package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.forecast.ForecastChangeEntity
 *
 * Table `TABLE_FORECAST_CHANGE_INFO`, 6 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: queued "the forecast changed" notices, composite key
 * (weather key + uuid) so several can be pending at once.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_FORECAST_CHANGE_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_FORECAST_CHANGE_UUID],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyForecastChangeEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_FORECAST_CHANGE_UUID)
    val uuid: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_FORECAST_CHANGE_CODE)
    val code: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_FORECAST_CHANGE_TITLE)
    val title: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_FORECAST_CHANGE_DESCRIPTION)
    val description: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_FORECAST_CHANGE_EXPIRE_TIME)
    val expireTime: Long = 0L,
)
