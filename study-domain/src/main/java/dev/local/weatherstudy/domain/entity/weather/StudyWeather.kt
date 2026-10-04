package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyLifeStyleContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.content.StudyWebMenu

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Weather
 *
 * Observed responsibilities:
 * - the single aggregate root for one saved location's forecast
 * - carries the location, the current observation, and the hourly/daily series
 * - carries the content blocks that feed the detail cards (alerts, insights,
 *   life style, radar, videos, today stories, web menus)
 * - `links` is a free-form map the providers fill with deep links
 *
 * Field set matches the 14 constructor components recovered from the original's
 * Kotlin @Metadata, in the same order.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyWeather(
    val location: StudyLocation,
    val currentObservation: StudyCurrentObservation,
    val hasIdx: String = "",
    val providerName: String = "",
    val hourlyObservations: List<StudyHourlyObservation> = emptyList(),
    val dailyObservations: List<StudyDailyObservation> = emptyList(),
    val webMenus: List<StudyWebMenu> = emptyList(),
    val alerts: List<StudyAlert> = emptyList(),
    val radar: StudyWebContent? = null,
    val videos: List<StudyWebContent> = emptyList(),
    val todayStories: List<StudyWebContent> = emptyList(),
    val insightContent: List<StudyInsightContent> = emptyList(),
    val lifeStyleContent: List<StudyLifeStyleContent> = emptyList(),
    val forecastChange: StudyForecastChange? = null,
) {
    val links: MutableMap<String, String> = mutableMapOf()
}

/**
 * Educational reconstruction of the file-facade helpers the original keeps in
 * `WeatherKt` — expiry arithmetic over the whole aggregate.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.WeatherKt
 */
fun StudyWeather.minUpdateTime(): Long =
    (listOf(currentObservation.time.updateTime) +
        hourlyObservations.map { it.time.updateTime } +
        dailyObservations.map { it.time.updateTime }).minOrNull() ?: 0L

fun StudyWeather.minForecastExpireTime(): Long =
    (hourlyObservations.map { it.time.expireTime } +
        dailyObservations.map { it.time.expireTime }).minOrNull()
        ?: currentObservation.time.expireTime

fun StudyWeather.minContentExpireTime(): Long =
    insightContent.map { it.expireTime }.minOrNull() ?: Long.MAX_VALUE
