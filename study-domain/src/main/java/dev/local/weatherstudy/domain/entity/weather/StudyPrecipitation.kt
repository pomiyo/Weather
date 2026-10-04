package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.type.StudyPrecipitationType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Precipitation
 *
 * Observed responsibilities:
 * - the per-type precipitation breakdown (rain / snow / hail), each with an
 *   amount and a probability
 * - built by the per-provider index converters (`TwcIndexConverter`,
 *   `WjpIndexConverter`, …), which is why it is not a field on Condition
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyPrecipitation(
    val precipitationAmount: Double = 0.0,
    val precipitationProbability: Int = 0,
    val rainAmount: Double = 0.0,
    val rainProbability: Int = 0,
    val snowAmount: Double = 0.0,
    val snowProbability: Int = 0,
    val hailAmount: Double = 0.0,
    val hailProbability: Int = 0,
)

/**
 * Corresponds conceptually to `…entity.weather.HourlyPrecipitation` —
 * the compact per-hour row the precipitation card's chart consumes.
 */
@JsonClass(generateAdapter = true)
data class StudyHourlyPrecipitation(
    val precipitationType: Int = StudyPrecipitationType.NONE,
    val precipitationAmount: Double = 0.0,
    val precipitationProbability: Int = 0,
)

/** Reconstruction of `PrecipitationKt`. */
fun StudyPrecipitation.hasAny(): Boolean =
    precipitationAmount > 0.0 || rainAmount > 0.0 || snowAmount > 0.0 || hailAmount > 0.0
