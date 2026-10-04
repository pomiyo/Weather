package dev.local.weatherstudy.persistence.mapper

import dev.local.weatherstudy.domain.entity.content.StudyInsightCard
import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.weather.StudyAlert
import dev.local.weatherstudy.domain.entity.weather.StudyCondition
import dev.local.weatherstudy.domain.entity.weather.StudyCurrentObservation
import dev.local.weatherstudy.domain.entity.weather.StudyDailyObservation
import dev.local.weatherstudy.domain.entity.weather.StudyForecastTime
import dev.local.weatherstudy.domain.entity.weather.StudyHourlyObservation
import dev.local.weatherstudy.domain.entity.weather.StudyIndex
import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.type.StudyIndexCategory
import dev.local.weatherstudy.domain.type.StudyIndexType
import dev.local.weatherstudy.persistence.database.models.StudyAlertEntity
import dev.local.weatherstudy.persistence.database.models.StudyDailyEntity
import dev.local.weatherstudy.persistence.database.models.StudyHourlyEntity
import dev.local.weatherstudy.persistence.database.models.StudyIndexEntity
import dev.local.weatherstudy.persistence.database.models.StudyInsightContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyWeatherEntity
import dev.local.weatherstudy.persistence.database.relation.StudyWeatherWithChildren
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the entity↔domain conversion the original performs
 * inside `WeatherRoomDao`'s generated implementation and its `models.forecast`
 * package (the standalone mapper classes are not separately named in the decompiled
 * output, but the conversion is unmistakable from the column↔field divergence).
 *
 * ### Why a mapper is unavoidable here
 *
 * The Room row and the domain aggregate are **shaped differently on purpose**:
 *
 * ```
 * StudyWeatherEntity (65 flat columns)        StudyWeather (a graph)
 * ─────────────────────────────────────       ───────────────────────────────
 * COL_WEATHER_NAME, _STATE, _COUNTRY,    →    location: StudyLocation
 *   _COUNTRY_CODE, _LATITUDE, …
 * COL_WEATHER_CURRENT_TEMP, _ICON_NUM,   →    currentObservation.condition
 *   _WEATHER_TEXT, _FEELSLIKE_TEMP, …
 * COL_WEATHER_SUNRISE_TIME, _TIMEZONE,   →    currentObservation.time
 *   _EXPIRE_TIME, _IS_DAY_OR_NIGHT, …
 * COL_WEATHER_DAY_RAIN_AMOUNT … (16 cols) →   indexList entries
 * ```
 *
 * Three further things the mapper has to reconcile:
 *
 * 1. **latitude/longitude are TEXT in the schema** but `Double` in the domain — the
 *    original stores them as strings, so parsing (and failing safely) happens here.
 * 2. **`locationLabelType` is TEXT** in the schema but `Int` in the domain.
 * 3. **three icon columns** per slot persist the whole icon pipeline; the domain keeps
 *    one `iconNum`, so the mapper picks the converted one and falls back.
 * 4. **per-hour and per-day measurements are columns** (`COL_HOURLY_RAIN_PROBABILITY`,
 *    `_WIND_SPEED`, `_HUMIDITY`, `COL_DAILY_PROBABILITY`, …) but index-list entries in
 *    the domain, so each slot is folded into and unfolded from `indexList`.
 * 5. **child rows come back unordered** from a Room relation; the time order the UI
 *    depends on is restored here.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyWeatherEntityMapper @Inject constructor() {

    // ---------------- entity -> domain ----------------

    fun toDomain(row: StudyWeatherWithChildren): StudyWeather {
        val e = row.weather
        return StudyWeather(
            location = toLocation(e),
            currentObservation = toCurrentObservation(e, row.indices),
            hasIdx = e.hasidx.orEmpty(),
            providerName = e.providerName.orEmpty(),
            hourlyObservations = row.hourly.sortedBy { it.time }.map(::toHourly),
            dailyObservations = row.daily.sortedBy { it.time }.map(::toDaily),
            alerts = row.alerts.map(::toAlert),
            insightContent = row.insights.sortedBy { it.order }.map(::toInsight),
        )
    }

    private fun toLocation(e: StudyWeatherEntity) = StudyLocation(
        key = e.key,
        id = e.location,
        priority = e.order ?: StudyLocation.DEFAULT_PRIORITY,
        latitude = e.latitude.toCoordinate(),
        longitude = e.longitude.toCoordinate(),
        cityName = e.name.orEmpty(),
        stateName = e.state.orEmpty(),
        countryName = e.country.orEmpty(),
        countryCode = e.countryCode.orEmpty(),
        postalCode = e.postalCode.orEmpty(),
        shortAddress = e.locationShortAddress.orEmpty(),
        updateTime = e.locationUpdateTime ?: 0L,
        label = e.locationLabel.orEmpty(),
        labelType = e.locationLabelType?.toIntOrNull() ?: 0,
    )

    private fun toCurrentObservation(e: StudyWeatherEntity, indices: List<StudyIndexEntity>) =
        StudyCurrentObservation(
            condition = StudyCondition(
                iconNum = e.pickIconNum(),
                weatherText = e.weatherText.orEmpty(),
                narrative = e.forecastText.orEmpty(),
                temperature = e.currentTemp ?: StudyCondition.INVALID_TEMPERATURE,
                feelsLikeTemp = e.feelsLikeTemp ?: StudyCondition.INVALID_TEMPERATURE,
                maxTemp = e.highTemp ?: StudyCondition.INVALID_TEMPERATURE,
                minTemp = e.lowTemp ?: StudyCondition.INVALID_TEMPERATURE,
                yesterdayMaxTemp = e.yesterdayHighTemp ?: StudyCondition.INVALID_TEMPERATURE,
                yesterdayMinTemp = e.yesterdayLowTemp ?: StudyCondition.INVALID_TEMPERATURE,
                indexList = indices.map(::toIndex),
            ),
            time = StudyForecastTime(
                epochTime = e.time ?: 0L,
                updateTime = e.updateTime ?: 0L,
                publishTime = e.publishTime ?: 0L,
                expireTime = e.expireTime ?: 0L,
                ianaTimeZone = e.ianaTimeZone.orEmpty(),
                timeZone = e.timeZone?.toFloatOrNull() ?: 0f,
                isDST = (e.isDaylightSaving ?: 0) != 0,
                isDayOrNight = e.isDayOrNight ?: StudyForecastTime.DAY,
                sunRiseTime = e.sunRiseTime ?: StudyForecastTime.INVALID_TIME,
                sunSetTime = e.sunSetTime ?: StudyForecastTime.INVALID_TIME,
                moonRiseTime = e.moonRiseTime ?: StudyForecastTime.INVALID_TIME,
                moonSetTime = e.moonSetTime ?: StudyForecastTime.INVALID_TIME,
                arcticNightType = e.arcticNightType ?: StudyForecastTime.ARCTIC_NONE,
            ),
            webUrl = e.url.orEmpty(),
        )

    private fun toHourly(e: StudyHourlyEntity) = StudyHourlyObservation(
        condition = StudyCondition(
            iconNum = e.convertedIconNum ?: e.iconNum ?: StudyCondition.INVALID_CODE,
            weatherText = e.weatherText.orEmpty(),
            temperature = e.currentTemp ?: StudyCondition.INVALID_TEMPERATURE,
            maxTemp = e.highTemp ?: StudyCondition.INVALID_TEMPERATURE,
            minTemp = e.lowTemp ?: StudyCondition.INVALID_TEMPERATURE,
            // the per-hour measurement columns, unfolded back into index entries
            indexList = buildList {
                e.rainProbability?.let {
                    add(detailIndex(StudyIndexType.PRECIPITATION_PROBABILITY, it.toDouble()))
                }
                e.rainPrecipitation?.let {
                    add(
                        detailIndex(StudyIndexType.PRECIPITATION_AMOUNT, it)
                            .copy(level = e.precipitationType ?: 0),
                    )
                }
                e.humidity?.let { add(detailIndex(StudyIndexType.HUMIDITY, it.toDouble())) }
                e.windSpeed?.let {
                    add(
                        detailIndex(StudyIndexType.WIND, it.toDouble())
                            .copy(levelText = e.windDirection.orEmpty()),
                    )
                }
            },
        ),
        time = StudyForecastTime(
            epochTime = e.time,
            expireTime = e.expireTime ?: 0L,
            isDayOrNight = e.isDayOrNight ?: StudyForecastTime.DAY,
        ),
        webUrl = e.url.orEmpty(),
    )

    private fun toDaily(e: StudyDailyEntity) = StudyDailyObservation(
        dayCondition = StudyCondition(
            iconNum = e.convertedIconDayNum ?: e.iconDayNum ?: StudyCondition.INVALID_CODE,
            weatherText = e.weatherText.orEmpty(),
            narrative = e.narrativeText.orEmpty(),
            maxTemp = e.highTemp ?: StudyCondition.INVALID_TEMPERATURE,
            minTemp = e.lowTemp ?: StudyCondition.INVALID_TEMPERATURE,
            indexList = listOfNotNull(
                e.probability?.let { detailIndex(StudyIndexType.PRECIPITATION_PROBABILITY, it.toDouble()) },
            ),
        ),
        nightCondition = StudyCondition(
            iconNum = e.convertedIconNightNum ?: e.iconNightNum ?: StudyCondition.INVALID_CODE,
            weatherText = e.weatherTextNight.orEmpty(),
            narrative = e.narrativeTextNight.orEmpty(),
            maxTemp = e.highTemp ?: StudyCondition.INVALID_TEMPERATURE,
            minTemp = e.lowTemp ?: StudyCondition.INVALID_TEMPERATURE,
            indexList = listOfNotNull(
                e.probabilityNight?.let {
                    detailIndex(StudyIndexType.PRECIPITATION_PROBABILITY_NIGHT, it.toDouble())
                },
            ),
        ),
        time = StudyForecastTime(
            epochTime = e.time,
            expireTime = e.expireTime ?: 0L,
            sunRiseTime = e.sunriseTime,
            sunSetTime = e.sunsetTime,
        ),
        webUrl = e.url.orEmpty(),
    )

    private fun toIndex(e: StudyIndexEntity) = StudyIndex(
        type = e.type,
        category = e.category,
        value = e.value ?: StudyIndex.INVALID_VALUE,
        level = e.level ?: 0,
        levelText = e.text.orEmpty(),
        description = e.description.orEmpty(),
        extra = e.extra?.toString().orEmpty(),
        priority = e.priority ?: 0,
        webUrl = e.url.orEmpty(),
    )

    private fun toAlert(e: StudyAlertEntity) = StudyAlert(
        detailKey = e.detailKey,
        eventDescription = e.description.orEmpty(),
        severityCode = e.severityCode ?: StudyAlert.SEVERITY_UNKNOWN,
        issueTimeZone = e.issueTimeZone.orEmpty(),
        expireTime = e.expireTime ?: 0L,
        linkURL = e.linkURL.orEmpty(),
    )

    private fun toInsight(e: StudyInsightContentEntity) = StudyInsightContent(
        insightType = e.insightType,
        order = e.order,
        card = StudyInsightCard(
            title = e.title.orEmpty(),
            content = e.text.orEmpty(),
            shortContent = e.shortText.orEmpty(),
            defaultContent = e.defaultText.orEmpty(),
            timeDescription = e.timeDescription.orEmpty(),
            url = e.url.orEmpty(),
        ),
        showDefault = e.showDefault != 0,
        showDetail = e.showDetail != 0,
        showWidget = e.showWidget != 0,
        showNotification = e.showNotification != 0,
    )

    // ---------------- domain -> entity ----------------

    fun toEntity(weather: StudyWeather): StudyWeatherWithChildren {
        val key = weather.location.key
        val condition = weather.currentObservation.condition
        val time = weather.currentObservation.time
        return StudyWeatherWithChildren(
            weather = StudyWeatherEntity(
                key = key,
                location = weather.location.id,
                order = weather.location.priority,
                name = weather.location.cityName,
                state = weather.location.stateName,
                country = weather.location.countryName,
                countryCode = weather.location.countryCode,
                postalCode = weather.location.postalCode,
                latitude = weather.location.latitude.toString(),
                longitude = weather.location.longitude.toString(),
                locationShortAddress = weather.location.shortAddress,
                locationUpdateTime = weather.location.updateTime,
                locationLabel = weather.location.label,
                locationLabelType = weather.location.labelType.toString(),
                iconNum = condition.iconNum,
                convertedIconNum = condition.iconNum,
                weatherText = condition.weatherText,
                forecastText = condition.narrative,
                currentTemp = condition.temperature,
                feelsLikeTemp = condition.feelsLikeTemp,
                highTemp = condition.maxTemp,
                lowTemp = condition.minTemp,
                yesterdayHighTemp = condition.yesterdayMaxTemp,
                yesterdayLowTemp = condition.yesterdayMinTemp,
                time = time.epochTime,
                updateTime = time.updateTime,
                publishTime = time.publishTime,
                expireTime = time.expireTime,
                timeZone = time.timeZone.toString(),
                ianaTimeZone = time.ianaTimeZone,
                isDaylightSaving = if (time.isDST) 1 else 0,
                isDayOrNight = time.isDayOrNight,
                sunRiseTime = time.sunRiseTime,
                sunSetTime = time.sunSetTime,
                moonRiseTime = time.moonRiseTime,
                moonSetTime = time.moonSetTime,
                arcticNightType = time.arcticNightType,
                url = weather.currentObservation.webUrl,
                hasidx = weather.hasIdx,
                providerName = weather.providerName,
            ),
            hourly = weather.hourlyObservations.map { h ->
                val precipitation = h.condition.find(StudyIndexType.PRECIPITATION_AMOUNT)
                val wind = h.condition.find(StudyIndexType.WIND)
                StudyHourlyEntity(
                    key = key,
                    time = h.time.epochTime,
                    isDayOrNight = h.time.isDayOrNight,
                    currentTemp = h.condition.temperature,
                    highTemp = h.condition.maxTemp,
                    lowTemp = h.condition.minTemp,
                    iconNum = h.condition.iconNum,
                    convertedIconNum = h.condition.iconNum,
                    rainProbability = h.condition.find(StudyIndexType.PRECIPITATION_PROBABILITY)
                        ?.value?.roundToInt(),
                    windDirection = wind?.levelText,
                    windSpeed = wind?.value?.roundToInt(),
                    humidity = h.condition.find(StudyIndexType.HUMIDITY)?.value?.roundToInt(),
                    weatherText = h.condition.weatherText,
                    url = h.webUrl,
                    rainPrecipitation = precipitation?.value,
                    precipitationType = precipitation?.level,
                    expireTime = h.time.expireTime,
                )
            },
            daily = weather.dailyObservations.map { d ->
                StudyDailyEntity(
                    key = key,
                    time = d.time.epochTime,
                    highTemp = d.dayCondition.maxTemp,
                    lowTemp = d.nightCondition.minTemp,
                    iconDayNum = d.dayCondition.iconNum,
                    convertedIconDayNum = d.dayCondition.iconNum,
                    iconNightNum = d.nightCondition.iconNum,
                    convertedIconNightNum = d.nightCondition.iconNum,
                    weatherText = d.dayCondition.weatherText,
                    weatherTextNight = d.nightCondition.weatherText,
                    narrativeText = d.dayCondition.narrative,
                    narrativeTextNight = d.nightCondition.narrative,
                    sunriseTime = d.time.sunRiseTime,
                    sunsetTime = d.time.sunSetTime,
                    url = d.webUrl,
                    probability = d.dayCondition.find(StudyIndexType.PRECIPITATION_PROBABILITY)
                        ?.value?.roundToInt(),
                    probabilityNight = d.nightCondition
                        .find(StudyIndexType.PRECIPITATION_PROBABILITY_NIGHT)?.value?.roundToInt(),
                    expireTime = d.time.expireTime,
                )
            },
            // (key, type, category) is the primary key, so a repeated type keeps its last entry
            indices = condition.indexList.associateBy { it.type to it.category }.values.map { i ->
                StudyIndexEntity(
                    key = key,
                    type = i.type,
                    category = i.category,
                    text = i.levelText,
                    value = i.value,
                    priority = i.priority,
                    level = i.level,
                    url = i.webUrl,
                    extra = i.extra.toIntOrNull(),
                    description = i.description,
                )
            },
            alerts = weather.alerts.map { a ->
                StudyAlertEntity(
                    key = key,
                    detailKey = a.detailKey,
                    description = a.eventDescription,
                    severityCode = a.severityCode,
                    expireTime = a.expireTime,
                    issueTimeZone = a.issueTimeZone,
                    linkURL = a.linkURL,
                )
            },
            insights = weather.insightContent.associateBy { it.order }.values.map { c ->
                StudyInsightContentEntity(
                    key = key,
                    insightType = c.insightType,
                    order = c.order,
                    showNotification = if (c.showNotification) 1 else 0,
                    showWidget = if (c.showWidget) 1 else 0,
                    showDetail = if (c.showDetail) 1 else 0,
                    showDefault = if (c.showDefault) 1 else 0,
                    title = c.card.title,
                    text = c.card.content,
                    shortText = c.card.shortContent,
                    defaultText = c.card.defaultContent,
                    url = c.card.url,
                    timeDescription = c.card.timeDescription,
                )
            },
        )
    }

    private fun detailIndex(type: Int, value: Double) =
        StudyIndex(type = type, category = StudyIndexCategory.DETAIL, value = value)

    private fun StudyCondition.find(type: Int): StudyIndex? =
        indexList.firstOrNull { it.type == type && it.value != StudyIndex.INVALID_VALUE }

    private fun String?.toCoordinate(): Double =
        this?.toDoubleOrNull() ?: StudyLocation.INVALID_COORDINATE

    private fun StudyWeatherEntity.pickIconNum(): Int =
        convertedIconNum ?: iconNum ?: StudyCondition.INVALID_CODE
}
