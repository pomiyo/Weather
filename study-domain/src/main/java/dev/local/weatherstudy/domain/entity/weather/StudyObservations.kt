package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Observation
 *
 * Observed responsibilities:
 * - the common shape of every forecast slot: a [StudyCondition] payload,
 *   a [StudyForecastTime] stamp, and a provider web link
 *
 * The original declares Observation, CurrentObservation and HourlyObservation with
 * the identical three-component shape and DailyObservation with a day/night pair.
 * They are kept as distinct types rather than merged, because the DAOs, mappers and
 * card state providers are all typed on them individually.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyObservation {
    val condition: StudyCondition
    val time: StudyForecastTime
    val webUrl: String
}

/** Corresponds conceptually to `…entity.weather.CurrentObservation`. */
@JsonClass(generateAdapter = true)
data class StudyCurrentObservation(
    override val condition: StudyCondition = StudyCondition(),
    override val time: StudyForecastTime = StudyForecastTime(),
    override val webUrl: String = "",
) : StudyObservation

/** Corresponds conceptually to `…entity.weather.HourlyObservation`. */
@JsonClass(generateAdapter = true)
data class StudyHourlyObservation(
    override val condition: StudyCondition = StudyCondition(),
    override val time: StudyForecastTime = StudyForecastTime(),
    override val webUrl: String = "",
) : StudyObservation

/**
 * Corresponds conceptually to `…entity.weather.DailyObservation`.
 *
 * Observed responsibility: a day row carries TWO conditions and an `isDay` flag
 * selecting which one the daily card shows — that is why the original does not
 * reuse Observation here.
 */
@JsonClass(generateAdapter = true)
data class StudyDailyObservation(
    val dayCondition: StudyCondition = StudyCondition(),
    val nightCondition: StudyCondition = StudyCondition(),
    val isDay: Boolean = true,
    val time: StudyForecastTime = StudyForecastTime(),
    val webUrl: String = "",
) {
    val activeCondition: StudyCondition get() = if (isDay) dayCondition else nightCondition
}

/** Corresponds conceptually to `…entity.weather.CurrentObservationWithKey`. */
data class StudyCurrentObservationWithKey(
    val key: String,
    val observation: StudyCurrentObservation,
)
