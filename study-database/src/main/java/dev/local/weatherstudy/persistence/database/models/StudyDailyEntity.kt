package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.DailyEntity
 *
 * Table `TABLE_DAILY_INFO`, 28 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibilities:
 * - one row per forecast day, composite key (weather key + epoch time)
 * - **six icon columns**: a day set and a night set, each in all three pipeline
 *   stages. The daily card renders one or the other from the device's day/night state.
 * - text is also doubled (`WEATHER_TEXT` / `WEATHER_TEXT_NIGHT`,
 *   `NARRATIVE_TEXT` / `NARRATIVE_TEXT_NIGHT`) for the same reason
 * - `SUNRISE_TIME` / `SUNSET_TIME` are NOT NULL with a `0` default, unlike the
 *   nullable pair on the parent row — they were added later as non-null
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_DAILY_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_DAILY_TIME],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyDailyEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_HIGH_TEMP)
    val highTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_LOW_TEMP)
    val lowTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_CONVERTED_ICON_NUM)
    val convertedIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_EXPANSION_ICON_NUM)
    val expansionIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_TIME)
    val time: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_CURRENT_TEMP)
    val currentTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_ICON_NUM)
    val iconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_ICON_DAY_NUM)
    val iconDayNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_CONVERTED_ICON_DAY_NUM)
    val convertedIconDayNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_EXPANSION_DAY_ICON_NUM)
    val expansionDayIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_ICON_NIGHT_NUM)
    val iconNightNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_CONVERTED_ICON_NIGHT_NUM)
    val convertedIconNightNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_EXPANSION_NIGHT_ICON_NUM)
    val expansionNightIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_SUNRISE_TIME)
    val sunriseTime: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_SUNSET_TIME)
    val sunsetTime: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_PM10)
    val pm10: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_PM10LEVEL)
    val pm10Level: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_PM25)
    val pm25: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_PM25LEVEL)
    val pm25Level: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_WEATHER_TEXT)
    val weatherText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_WEATHER_TEXT_NIGHT)
    val weatherTextNight: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_NARRATIVE_TEXT)
    val narrativeText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_NARRATIVE_TEXT_NIGHT)
    val narrativeTextNight: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_URL)
    val url: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_PROBABILITY)
    val probability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_PROBABILITY_NIGHT)
    val probabilityNight: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_DAILY_EXPIRE_TIME)
    val expireTime: Long? = null,
)
