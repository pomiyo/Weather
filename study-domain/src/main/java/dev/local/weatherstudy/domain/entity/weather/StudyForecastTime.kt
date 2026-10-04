package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.ForecastTime
 *
 * Observed responsibilities:
 * - all time/expiry data for one observation slot
 * - `expireTime` is what the refresh use cases (`ReachToForecastRefreshTime`,
 *   `ReachToObservationRefreshTime`, `ReachToContentRefreshTime`) compare against,
 *   so staleness is a property of the data, not of a global timer
 * - `isDayOrNight` and the sun/moon times drive the themed splash choice, the
 *   SunCurvedPathView arc and the moon phase view
 * - `arcticNightType` handles polar day/night where there is no sunrise or sunset
 *
 * 13 constructor components, matching the original's @Metadata.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyForecastTime(
    val epochTime: Long = 0L,
    val updateTime: Long = 0L,
    val publishTime: Long = 0L,
    val expireTime: Long = 0L,
    val timeZone: Float = 0f,
    val ianaTimeZone: String = "",
    val isDST: Boolean = false,
    val isDayOrNight: Int = DAY,
    val sunRiseTime: Long = INVALID_TIME,
    val sunSetTime: Long = INVALID_TIME,
    val moonRiseTime: Long = INVALID_TIME,
    val moonSetTime: Long = INVALID_TIME,
    val arcticNightType: Int = ARCTIC_NONE,
) {
    companion object {
        const val INVALID_TIME = -1L

        const val DAY = 1
        const val NIGHT = 0

        const val ARCTIC_NONE = 0
        const val ARCTIC_POLAR_DAY = 1
        const val ARCTIC_POLAR_NIGHT = 2
    }
}

/** Reconstruction of `ForecastTimeKt`. */
val StudyForecastTime.isDay: Boolean get() = isDayOrNight == StudyForecastTime.DAY

fun StudyForecastTime.isExpired(now: Long): Boolean = expireTime in 1..<now
