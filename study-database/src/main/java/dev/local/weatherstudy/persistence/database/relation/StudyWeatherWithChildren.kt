package dev.local.weatherstudy.persistence.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import dev.local.weatherstudy.persistence.database.models.StudyAlertEntity
import dev.local.weatherstudy.persistence.database.models.StudyContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyDailyEntity
import dev.local.weatherstudy.persistence.database.models.StudyForecastChangeEntity
import dev.local.weatherstudy.persistence.database.models.StudyHourlyEntity
import dev.local.weatherstudy.persistence.database.models.StudyIndexEntity
import dev.local.weatherstudy.persistence.database.models.StudyInsightContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyLifeStyleEntity
import dev.local.weatherstudy.persistence.database.models.StudyWeatherEntity
import dev.local.weatherstudy.persistence.database.models.StudyWebMenuEntity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the `@Relation` POJOs the original's
 * `WeatherRoomDao` returns (the decompiled DAO implementation shows the
 * multi-table fetch, though the POJO names are lost to R8).
 *
 * Observed responsibility: the bridge between a flat root row and the eight-table
 * aggregate. `StudyWeatherEntity` is deliberately wide and denormalised; everything
 * genuinely one-to-many hangs off it here, joined on `COL_WEATHER_KEY`.
 *
 * Note what is NOT in the root row and therefore needs a relation: hourly, daily,
 * indices, alerts, content, web menus, lifestyle, insight. And note what IS inlined
 * and therefore needs none: the location, the current observation, and the day/night
 * precipitation matrix.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyWeatherWithChildren(
    @Embedded val weather: StudyWeatherEntity,

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val hourly: List<StudyHourlyEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val daily: List<StudyDailyEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val indices: List<StudyIndexEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val alerts: List<StudyAlertEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val contents: List<StudyContentEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val webMenus: List<StudyWebMenuEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val lifeStyles: List<StudyLifeStyleEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val insights: List<StudyInsightContentEntity> = emptyList(),

    @Relation(parentColumn = "COL_WEATHER_KEY", entityColumn = "COL_WEATHER_KEY")
    val forecastChange: StudyForecastChangeEntity? = null,
)
