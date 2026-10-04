package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.ForecastChange
 *
 * Observed responsibilities:
 * - a "the forecast has changed since you last looked" record, surfaced by
 *   `CheckForecastChange` and `UpdateForecastChangeNotification`
 * - `uuid` is part of the Room composite key so several changes can queue up
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyForecastChange(
    val uuid: String,
    val changeType: Int = 0,
    val title: String = "",
    val description: String = "",
    val createTime: Long = 0L,
    val expireTime: Long = 0L,
)

/**
 * Corresponds conceptually to `…entity.weather.BriefWeather`.
 *
 * Observed responsibility: the trimmed projection the widgets, complications and
 * content providers read, so they never pull the whole aggregate.
 */
@JsonClass(generateAdapter = true)
data class StudyBriefWeather(
    val key: String,
    val cityName: String = "",
    val temperature: Double = StudyCondition.INVALID_TEMPERATURE,
    val maxTemp: Double = StudyCondition.INVALID_TEMPERATURE,
    val minTemp: Double = StudyCondition.INVALID_TEMPERATURE,
    val iconNum: Int = StudyCondition.INVALID_CODE,
    val weatherText: String = "",
    val isDayOrNight: Int = StudyForecastTime.DAY,
    val updateTime: Long = 0L,
)

/**
 * Corresponds conceptually to `…entity.weather.AwayModeLocation`.
 *
 * Observed responsibility: the geofence "home vs away" feature — when the device
 * leaves its home geofence, `HomeToAwayModeWorker` records the away location here.
 */
@JsonClass(generateAdapter = true)
data class StudyAwayModeLocation(
    val awayLocation: String,
    val homeLocation: String = "",
    val enterTime: Long = 0L,
    val isActive: Boolean = false,
)

/** Corresponds conceptually to `…entity.weather.Theme` (map-search themed places). */
@JsonClass(generateAdapter = true)
data class StudyTheme(
    val id: String,
    val name: String = "",
    val categoryId: String = "",
    val places: List<StudyThemePlace> = emptyList(),
)

/** Corresponds conceptually to `…entity.weather.ThemePlace`. */
@JsonClass(generateAdapter = true)
data class StudyThemePlace(
    val key: String,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val iconNum: Int = StudyCondition.INVALID_CODE,
    val temperature: Double = StudyCondition.INVALID_TEMPERATURE,
)
