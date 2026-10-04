package dev.local.weatherstudy.domain.type

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.type.ContentType
 *
 * Observed responsibility: tags a `WebContent` row so one persisted table and one
 * entity can feed the Radar, Video, TodayStories, News and banner detail cards.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyContentType {
    const val NONE = 0
    const val RADAR = 1
    const val VIDEO = 2
    const val TODAY_STORIES = 3
    const val LIFE_BANNER = 4
    const val BANNER = 5
    const val ADVERTISEMENT = 6
    const val NEWS = 7
}

/**
 * Corresponds conceptually to `…type.InsightType`.
 *
 * Observed responsibility: which insight an `InsightContent` row is. Note the two
 * families: the "event" insights (1–20) the provider pushes, and the "condition"
 * insights (21–26) derived locally from the current observation, plus
 * `LIFE_STYLE` (27) and `AI_INSIGHT` (28) added later.
 */
object StudyInsightType {
    const val NONE = 0
    const val THUNDERSTORM_IMPACT = 1
    const val SNOW_FALL = 2
    const val SHORT_TERM_PRECIP = 3
    const val PRECIPITATION = 4
    const val RECORD_TEMPERATURE = 5
    const val SUNNYDAY = 6
    const val FEELS_LIKE_TEMPERATURE = 7
    const val UV = 8
    const val WIND = 9
    const val FLU = 10
    const val POLLEN = 11
    const val TEMPERATURE_CHANGE = 12
    const val AIR_QUALITY = 13
    const val BREAKING_NEWS = 14
    const val TRENDING_VIDEO = 15
    const val SUNRISE_SUNSET = 16
    const val FINE_DUST = 17
    const val ULTRA_FINE_DUST = 18
    const val SEASON_INFO = 19
    const val TOMORROW_COMMENT = 20

    // derived locally from the current observation
    const val WIND_CONDITION = 21
    const val DEW_POINT_CONDITION = 22
    const val PRESSURE_CONDITION = 23
    const val UV_CONDITION = 24
    const val HUMIDITY_CONDITION = 25
    const val VISIBILITY_CONDITION = 26

    const val LIFE_STYLE = 27
    const val AI_INSIGHT = 28
}

/** Corresponds conceptually to `…type.PrecipitationType`. */
object StudyPrecipitationType {
    const val NONE = 0
    const val RAIN = 1
    const val SNOW = 2
    const val MIXED = 3
    const val STORMS = 4
}

/** Corresponds conceptually to `…type.SunRiseSunsetType`. */
object StudySunRiseSunsetType {
    const val SUNRISE = 1
    const val SUNSET = 2
}
