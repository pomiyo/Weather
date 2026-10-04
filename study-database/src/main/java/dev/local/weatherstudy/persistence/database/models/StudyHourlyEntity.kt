package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.HourlyEntity
 *
 * Table `TABLE_HOURLY_INFO`, 22 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibilities:
 * - one row per forecast hour, composite key (weather key + epoch time)
 * - carries its own `EXPIRE_TIME`, so staleness is per-hour, not per-location
 * - note `COL_HOURLY_WIND_DESCRIPTION` is the only NOT NULL text column with a
 *   `''` default — added by a later migration against existing rows
 * - CASCADE on the parent key
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_HOURLY_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_HOURLY_TIME],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyHourlyEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_TIME)
    val time: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_IS_DAY_OR_NIGHT)
    val isDayOrNight: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_CURRENT_TEMP)
    val currentTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_HIGH_TEMP)
    val highTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_LOW_TEMP)
    val lowTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_ICON_NUM)
    val iconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_CONVERTED_ICON_NUM)
    val convertedIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_EXPANSION_ICON_NUM)
    val expansionIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_RAIN_PROBABILITY)
    val rainProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_WIND_DIRECTION)
    val windDirection: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_WIND_SPEED)
    val windSpeed: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_WIND_DESCRIPTION)
    val windDescription: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_HUMIDITY)
    val humidity: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_WEATHER_TEXT)
    val weatherText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_URL)
    val url: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_PM25F)
    val pm25f: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_PM25FLEVEL)
    val pm25fLevel: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_AQI)
    val aqi: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_RAIN_PRECIPITATION)
    val rainPrecipitation: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_PRECIPITATION_TYPE)
    val precipitationType: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_HOURLY_EXPIRE_TIME)
    val expireTime: Long? = null,
)
