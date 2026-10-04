package dev.local.weatherstudy.data.source.remote.api.forecast.providera.sub

import dev.local.weatherstudy.domain.entity.content.StudyInsightCard
import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.weather.StudyAlert
import dev.local.weatherstudy.domain.entity.weather.StudyBriefWeather
import dev.local.weatherstudy.domain.entity.weather.StudyCondition
import dev.local.weatherstudy.domain.entity.weather.StudyCurrentObservation
import dev.local.weatherstudy.domain.entity.weather.StudyDailyObservation
import dev.local.weatherstudy.domain.entity.weather.StudyForecastTime
import dev.local.weatherstudy.domain.entity.weather.StudyHourlyObservation
import dev.local.weatherstudy.domain.entity.weather.StudyIndex
import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.type.StudyContentType
import dev.local.weatherstudy.domain.type.StudyIndexCategory
import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.domain.type.StudyIndexType
import dev.local.weatherstudy.domain.type.StudyPrecipitationType
import dev.local.weatherstudy.network.models.forecast.common.StudyAirQualityDto
import dev.local.weatherstudy.network.models.forecast.common.StudyAlertDto
import dev.local.weatherstudy.network.models.forecast.common.StudyCurrentConditionsDto
import dev.local.weatherstudy.network.models.forecast.common.StudyDailyForecastDto
import dev.local.weatherstudy.network.models.forecast.common.StudyHourlyForecastDto
import dev.local.weatherstudy.network.models.forecast.common.StudyInsightDto
import dev.local.weatherstudy.network.models.forecast.common.StudyLifeIndexDto
import dev.local.weatherstudy.network.models.forecast.common.StudyLocalWeatherDto
import dev.local.weatherstudy.network.models.forecast.common.StudyLocationDto
import dev.local.weatherstudy.network.models.forecast.common.StudySearchDto
import dev.local.weatherstudy.network.models.forecast.common.StudyTimeZoneDto
import dev.local.weatherstudy.network.models.forecast.common.StudyUnitValueDto
import dev.local.weatherstudy.network.models.forecast.common.skipNulls
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 17 classes in
 * com.samsung.android.weather.data.source.remote.api.forecast.twc.sub
 * plus `twc/TwcConverter`, `TwcCodeConverter`, `TwcExpansionCodeConverter`,
 * `TwcAQIScale` and `TwcAlertColor`
 *
 * ### One converter per payload section — and why
 *
 * The original's `sub/` package, recovered from the APK:
 *
 * ```
 * TwcForecastConverter            TwcHourlyForecastConverter   TwcDailyForecastConverter
 * TwcCurrentObservationConverter  TwcBriefForecastConverter    TwcForecastChangeConverter
 * TwcIndexConverter               TwcInsightConverter          TwcLifeStyleConverter
 * TwcLocationConverter            TwcSearchConverter           TwcRadarConverter
 * TwcVideoConverter               TwcUnitConverter             TwcReviseDisputedArea
 * TwcIndex                        TwcPrecipitation
 * ```
 *
 * ×5 providers ≈ 85 converter classes in the shipping app. The split is not arbitrary:
 * a backend may serve the daily section in a new shape while leaving hourly alone, and
 * only one converter changes. It also means a provider that lacks a section simply has
 * no converter for it.
 *
 * Three members are worth singling out, because they are not obvious:
 *
 * - **`TwcCodeConverter`** maps the backend's condition code onto Samsung's internal
 *   code. Every provider has one, and it is why nothing downstream ever sees a
 *   provider-specific code — see [StudyProviderACodeConverter].
 * - **`TwcUnitConverter`** exists because the feeds send every measurement in *both*
 *   metric and imperial with a unit tag; picking one is a parsing decision here, not a
 *   formatting decision in the UI.
 * - **`TwcReviseDisputedArea`** suppresses the country name for territories under
 *   dispute, feeding `StudyLocation.isDisputedArea`. A per-provider policy class with a
 *   single purpose — exactly the kind of thing a simplified reconstruction would lose.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyProviderAConverter @Inject constructor(
    private val locationConverter: StudyProviderALocationConverter,
    private val currentObservationConverter: StudyProviderACurrentObservationConverter,
    private val hourlyConverter: StudyProviderAHourlyForecastConverter,
    private val dailyConverter: StudyProviderADailyForecastConverter,
    private val indexConverter: StudyProviderAIndexConverter,
    private val insightConverter: StudyProviderAInsightConverter,
    private val videoConverter: StudyProviderAVideoConverter,
    private val searchConverter: StudyProviderASearchConverter,
) {
    /** Reconstruction of `TwcForecastConverter` — assembles the whole aggregate. */
    fun toWeather(dto: StudyLocalWeatherDto, requested: StudyLocation): StudyWeather {
        val location = locationConverter.convert(dto.location, requested)
        val indices = indexConverter.convert(dto.lifeIndex, dto.airQuality)
        return StudyWeather(
            location = location,
            currentObservation = currentObservationConverter.convert(dto.currentConditions, indices)
                .withTimeZone(dto.location?.timeZone),
            providerName = dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider.PROVIDER_A,
            hourlyObservations = hourlyConverter.convert(dto.hourlyForecast),
            dailyObservations = dailyConverter.convert(dto.dailyForecast),
            alerts = dto.alerts.skipNulls().map(::toAlert),
            insightContent = insightConverter.convert(dto.insight),
        ).also { weather -> dto.links?.let { weather.links.putAll(it) } }
    }

    /** Reconstruction of `TwcBriefForecastConverter`. */
    fun toBriefWeather(dto: StudyLocalWeatherDto, requested: StudyLocation): StudyBriefWeather {
        val location = locationConverter.convert(dto.location, requested)
        val current = currentObservationConverter.convert(dto.currentConditions, emptyList())
        return StudyBriefWeather(
            key = location.key,
            cityName = location.cityName,
            temperature = current.condition.temperature,
            maxTemp = current.condition.maxTemp,
            minTemp = current.condition.minTemp,
            iconNum = current.condition.iconNum,
            weatherText = current.condition.weatherText,
            isDayOrNight = current.time.isDayOrNight,
            updateTime = current.time.updateTime,
        )
    }

    fun toCurrentObservation(dto: StudyLocalWeatherDto): StudyCurrentObservation =
        currentObservationConverter
            .convert(dto.currentConditions, indexConverter.convert(dto.lifeIndex, dto.airQuality))
            .withTimeZone(dto.location?.timeZone)

    fun toInsights(dto: StudyLocalWeatherDto): List<StudyInsightContent> =
        insightConverter.convert(dto.insight)

    fun toVideos(dto: StudyLocalWeatherDto): List<StudyWebContent> = videoConverter.convert(dto)

    fun toLocations(dto: StudySearchDto): List<StudyLocation> = searchConverter.convert(dto)

    /** the zone arrives on the location block; the domain model keeps it on the observation */
    private fun StudyCurrentObservation.withTimeZone(zone: StudyTimeZoneDto?): StudyCurrentObservation =
        if (zone == null) {
            this
        } else {
            copy(
                time = time.copy(
                    ianaTimeZone = zone.name.orEmpty(),
                    timeZone = zone.offset ?: 0f,
                    isDST = zone.isDaylightSaving ?: false,
                ),
            )
        }

    private fun toAlert(dto: StudyAlertDto) = StudyAlert(
        detailKey = dto.detailKey.orEmpty(),
        eventDescription = dto.eventDescription.orEmpty(),
        severityCode = dto.severity ?: StudyAlert.SEVERITY_UNKNOWN,
        significance = dto.significance.orEmpty(),
        issueTimeZone = dto.issueTimeZone.orEmpty(),
        expireTime = dto.expireTime ?: 0L,
        linkURL = dto.link.orEmpty(),
    )
}

/**
 * Corresponds conceptually to `…twc.TwcCodeConverter` and `TwcExpansionCodeConverter`.
 *
 * Observed responsibility: translate the backend's own condition code into Samsung's
 * internal code. **This is the single most important converter in the provider package**:
 * it is why `StudyAssignIconNum` can be provider-agnostic, why one icon set serves five
 * backends, and why the themed splash activities can key off one code space.
 *
 * The original has two: a base code map and an "expansion" map for finer variants (e.g.
 * distinguishing drizzle from rain). The mapping table itself is provider data and is
 * not reproduced; the table *shape* and its position in the pipeline are.
 */
@Singleton
class StudyProviderACodeConverter @Inject constructor() {

    /** backend code → internal condition code */
    fun toInternalCode(externalCode: String?): Int =
        externalCode?.toIntOrNull()?.let { code -> INTERNAL_CODE_RANGES[code] } ?: UNKNOWN

    /** the finer variant, where the backend distinguishes one */
    fun toExpansionCode(externalCode: String?): String = externalCode.orEmpty()

    private companion object {
        const val UNKNOWN = -1

        /**
         * Illustrative. The original's table is a per-provider mapping of dozens of
         * codes; what matters architecturally is that it exists here and nowhere else.
         */
        val INTERNAL_CODE_RANGES: Map<Int, Int> = buildMap {
            (0..1).forEach { put(it, 0) }
            (2..4).forEach { put(it, 2) }
            (5..8).forEach { put(it, 5) }
            (9..18).forEach { put(it, 9) }
            (19..29).forEach { put(it, 19) }
            (30..35).forEach { put(it, 30) }
            (36..40).forEach { put(it, 36) }
        }
    }
}

/**
 * Corresponds conceptually to `…twc.sub.TwcUnitConverter`.
 *
 * Observed responsibility: the feeds send `{metric, imperial, unit}` for every
 * measurement (28 of the original's 298 DTOs are unit wrappers). Choosing the canonical
 * value happens here, so the domain holds one number and the UI only formats.
 */
@Singleton
class StudyProviderAUnitConverter @Inject constructor() {
    /** the domain stores metric; display conversion is `:study-ui-common`'s notation layer */
    fun toCanonical(dto: StudyUnitValueDto?): Double =
        dto?.metric ?: dto?.imperial?.let { fahrenheitToCelsius(it) } ?: INVALID

    private fun fahrenheitToCelsius(f: Double) = (f - FREEZING_F) * 5.0 / 9.0

    private companion object {
        const val INVALID = StudyCondition.INVALID_TEMPERATURE
        const val FREEZING_F = 32.0
    }
}

/**
 * Corresponds conceptually to `…twc.sub.TwcReviseDisputedArea`.
 *
 * Observed responsibility: a single-purpose per-provider policy class — suppress the
 * country name where a territory is disputed, so the UI shows the city and state only.
 * It sets `StudyLocation.isDisputedArea`, which the location row and the detail header
 * both read.
 *
 * The original's list of affected territories is provider/region data and is not
 * reproduced; the mechanism is.
 */
@Singleton
class StudyProviderAReviseDisputedArea @Inject constructor() {
    fun revise(location: StudyLocation): StudyLocation =
        if (isDisputed(location.countryCode)) {
            location.copy(isDisputedArea = true, countryName = "")
        } else {
            location
        }

    private fun isDisputed(countryCode: String): Boolean = countryCode in DISPUTED_CODES

    private companion object {
        /** empty by design — see the class note */
        val DISPUTED_CODES = emptySet<String>()
    }
}

/** Corresponds conceptually to `…twc.sub.TwcLocationConverter`. */
@Singleton
class StudyProviderALocationConverter @Inject constructor(
    private val reviseDisputedArea: StudyProviderAReviseDisputedArea,
) {
    fun convert(dto: StudyLocationDto?, requested: StudyLocation): StudyLocation {
        if (dto == null) return requested
        return reviseDisputedArea.revise(
            requested.copy(
                key = dto.placeId ?: requested.key,
                cityName = dto.city.orEmpty(),
                stateName = dto.state.orEmpty(),
                countryName = dto.country.orEmpty(),
                countryCode = dto.countryCode.orEmpty(),
                postalCode = dto.postalCode.orEmpty(),
                latitude = dto.latitude ?: requested.latitude,
                longitude = dto.longitude ?: requested.longitude,
                updateTime = System.currentTimeMillis(),
            ),
        )
    }
}

/** Corresponds conceptually to `…twc.sub.TwcCurrentObservationConverter`. */
@Singleton
class StudyProviderACurrentObservationConverter @Inject constructor(
    private val codeConverter: StudyProviderACodeConverter,
    private val unitConverter: StudyProviderAUnitConverter,
) {
    fun convert(dto: StudyCurrentConditionsDto?, indices: List<StudyIndex>): StudyCurrentObservation {
        if (dto == null) return StudyCurrentObservation()
        return StudyCurrentObservation(
            condition = StudyCondition(
                internalCode = codeConverter.toInternalCode(dto.iconCode),
                externalCode = dto.iconCode.orEmpty(),
                expansionCode = codeConverter.toExpansionCode(dto.iconCode),
                weatherText = dto.phrase.orEmpty(),
                narrative = dto.narrative.orEmpty(),
                temperature = unitConverter.toCanonical(dto.temperature),
                feelsLikeTemp = unitConverter.toCanonical(dto.feelsLike),
                maxTemp = unitConverter.toCanonical(dto.temperatureMax),
                minTemp = unitConverter.toCanonical(dto.temperatureMin),
                indexList = indices,
            ),
            time = StudyForecastTime(
                epochTime = dto.observationTime ?: 0L,
                updateTime = System.currentTimeMillis(),
                expireTime = dto.expireTime ?: 0L,
                isDayOrNight = if (dto.dayOrNight == NIGHT) {
                    StudyForecastTime.NIGHT
                } else {
                    StudyForecastTime.DAY
                },
                sunRiseTime = dto.sunrise ?: StudyForecastTime.INVALID_TIME,
                sunSetTime = dto.sunset ?: StudyForecastTime.INVALID_TIME,
                moonRiseTime = dto.moonrise ?: StudyForecastTime.INVALID_TIME,
                moonSetTime = dto.moonset ?: StudyForecastTime.INVALID_TIME,
            ),
        )
    }

    private companion object {
        const val NIGHT = "N"
    }
}

/** Corresponds conceptually to `…twc.sub.TwcHourlyForecastConverter`. */
@Singleton
class StudyProviderAHourlyForecastConverter @Inject constructor(
    private val codeConverter: StudyProviderACodeConverter,
    private val unitConverter: StudyProviderAUnitConverter,
) {
    fun convert(dtos: List<StudyHourlyForecastDto>?): List<StudyHourlyObservation> =
        dtos.skipNulls().map { dto ->
            StudyHourlyObservation(
                condition = StudyCondition(
                    internalCode = codeConverter.toInternalCode(dto.iconCode),
                    externalCode = dto.iconCode.orEmpty(),
                    weatherText = dto.phrase.orEmpty(),
                    temperature = unitConverter.toCanonical(dto.temperature),
                    indexList = buildList {
                        dto.precipChance?.let {
                            add(
                                StudyIndex(
                                    type = StudyIndexType.PRECIPITATION_PROBABILITY,
                                    category = StudyIndexCategory.DETAIL,
                                    value = it.toDouble(),
                                ),
                            )
                        }
                        dto.precipAmount?.let {
                            add(
                                StudyIndex(
                                    type = StudyIndexType.PRECIPITATION_AMOUNT,
                                    category = StudyIndexCategory.DETAIL,
                                    value = unitConverter.toCanonical(it),
                                    // the kind of precipitation rides on the level of the amount
                                    level = precipitationTypeOf(dto.precipType),
                                ),
                            )
                        }
                        dto.humidity?.let {
                            add(
                                StudyIndex(
                                    type = StudyIndexType.HUMIDITY,
                                    category = StudyIndexCategory.DETAIL,
                                    value = it.toDouble(),
                                ),
                            )
                        }
                        dto.windSpeed?.let {
                            add(
                                StudyIndex(
                                    type = StudyIndexType.WIND,
                                    category = StudyIndexCategory.DETAIL,
                                    value = unitConverter.toCanonical(it),
                                    levelText = dto.windDirection.orEmpty(),
                                ),
                            )
                        }
                    },
                ),
                time = StudyForecastTime(
                    epochTime = dto.validTime ?: 0L,
                    expireTime = dto.expireTime ?: 0L,
                    isDayOrNight = if (dto.dayOrNight == "N") {
                        StudyForecastTime.NIGHT
                    } else {
                        StudyForecastTime.DAY
                    },
                ),
            )
        }

    private fun precipitationTypeOf(type: String?): Int = when (type?.lowercase()) {
        "rain" -> StudyPrecipitationType.RAIN
        "snow" -> StudyPrecipitationType.SNOW
        "mixed" -> StudyPrecipitationType.MIXED
        "storm" -> StudyPrecipitationType.STORMS
        else -> StudyPrecipitationType.NONE
    }
}

/** Corresponds conceptually to `…twc.sub.TwcDailyForecastConverter`. */
@Singleton
class StudyProviderADailyForecastConverter @Inject constructor(
    private val codeConverter: StudyProviderACodeConverter,
    private val unitConverter: StudyProviderAUnitConverter,
) {
    fun convert(dtos: List<StudyDailyForecastDto>?): List<StudyDailyObservation> =
        dtos.skipNulls().map { dto ->
            StudyDailyObservation(
                dayCondition = StudyCondition(
                    internalCode = codeConverter.toInternalCode(dto.day?.iconCode),
                    externalCode = dto.day?.iconCode.orEmpty(),
                    weatherText = dto.day?.phrase.orEmpty(),
                    narrative = dto.day?.narrative.orEmpty(),
                    maxTemp = unitConverter.toCanonical(dto.temperatureMax),
                    minTemp = unitConverter.toCanonical(dto.temperatureMin),
                    indexList = chanceIndex(dto.day?.precipChance, StudyIndexType.PRECIPITATION_PROBABILITY),
                ),
                nightCondition = StudyCondition(
                    internalCode = codeConverter.toInternalCode(dto.night?.iconCode),
                    externalCode = dto.night?.iconCode.orEmpty(),
                    weatherText = dto.night?.phrase.orEmpty(),
                    narrative = dto.night?.narrative.orEmpty(),
                    maxTemp = unitConverter.toCanonical(dto.temperatureMax),
                    minTemp = unitConverter.toCanonical(dto.temperatureMin),
                    indexList = chanceIndex(
                        dto.night?.precipChance,
                        StudyIndexType.PRECIPITATION_PROBABILITY_NIGHT,
                    ),
                ),
                time = StudyForecastTime(
                    epochTime = dto.validTime ?: 0L,
                    expireTime = dto.expireTime ?: 0L,
                    sunRiseTime = dto.sunrise ?: StudyForecastTime.INVALID_TIME,
                    sunSetTime = dto.sunset ?: StudyForecastTime.INVALID_TIME,
                ),
            )
        }

    private fun chanceIndex(chance: Int?, type: Int): List<StudyIndex> =
        if (chance == null) emptyList()
        else listOf(StudyIndex(type = type, category = StudyIndexCategory.DETAIL, value = chance.toDouble()))
}

/**
 * Corresponds conceptually to `…twc.sub.TwcIndexConverter`, `TwcIndex`,
 * `TwcPrecipitation` and `…twc.TwcAQIScale`.
 *
 * Observed responsibility: fold the life-index list and the air-quality payload into one
 * `StudyIndex` list. `TwcAQIScale` is folded in here because which national AQI scale is
 * in force changes the index's level banding — see [StudyIndexLevel.AqiScale].
 */
@Singleton
class StudyProviderAIndexConverter @Inject constructor() {

    fun convert(
        lifeIndex: List<StudyLifeIndexDto>?,
        airQuality: StudyAirQualityDto?,
    ): List<StudyIndex> = buildList {
        addAll(lifeIndex.skipNulls().mapNotNull(::toIndex))
        airQuality?.let { addAll(toAirIndices(it)) }
    }

    private fun toIndex(dto: StudyLifeIndexDto): StudyIndex? {
        val type = INDEX_TYPES[dto.type] ?: return null
        return StudyIndex(
            type = type,
            category = StudyIndexCategory.DETAIL,
            value = dto.value ?: StudyIndex.INVALID_VALUE,
            level = dto.level ?: StudyIndexLevel.NONE,
            levelText = dto.levelText.orEmpty(),
            unit = dto.unit.orEmpty(),
            extra = dto.extra.orEmpty(),
            description = dto.description.orEmpty(),
            webUrl = dto.link.orEmpty(),
        )
    }

    private fun toAirIndices(dto: StudyAirQualityDto): List<StudyIndex> = buildList {
        dto.index?.let {
            add(
                StudyIndex(
                    type = StudyIndexType.AQI,
                    category = StudyIndexCategory.AIR,
                    value = it.toDouble(),
                    level = dto.level ?: StudyIndexLevel.NONE,
                    levelText = dto.category.orEmpty(),
                    extra = scaleOf(dto.scale).toString(),
                ),
            )
        }
        dto.pollutants.skipNulls().forEach { pollutant ->
            POLLUTANT_TYPES[pollutant.name?.uppercase()]?.let { type ->
                add(
                    StudyIndex(
                        type = type,
                        category = StudyIndexCategory.AIR,
                        value = pollutant.amount ?: StudyIndex.INVALID_VALUE,
                        level = pollutant.level ?: StudyIndexLevel.NONE,
                        unit = pollutant.unit.orEmpty(),
                    ),
                )
            }
        }
    }

    /** Reconstruction of `TwcAQIScale`: which national index the backend reported in. */
    private fun scaleOf(scale: String?): Int = when (scale?.uppercase()) {
        "EPA" -> StudyIndexLevel.AqiScale.EPA
        "NAQI" -> StudyIndexLevel.AqiScale.NAQI
        "ATMO" -> StudyIndexLevel.AqiScale.ATMO
        "DAQI" -> StudyIndexLevel.AqiScale.DAQI
        "UBA" -> StudyIndexLevel.AqiScale.UBA
        "IMECA" -> StudyIndexLevel.AqiScale.IMECA
        "CAQI" -> StudyIndexLevel.AqiScale.CAQI
        else -> StudyIndexLevel.AqiScale.EPA
    }

    private companion object {
        val INDEX_TYPES = mapOf(
            "uv" to StudyIndexType.UV,
            "humidity" to StudyIndexType.HUMIDITY,
            "pressure" to StudyIndexType.PRESSURE,
            "wind" to StudyIndexType.WIND,
            "visibility" to StudyIndexType.VISIBILITY,
            "dewPoint" to StudyIndexType.DEW_POINT,
            "pollen" to StudyIndexType.POLLEN,
            "precipAmount" to StudyIndexType.PRECIPITATION_AMOUNT,
            "precipChance" to StudyIndexType.PRECIPITATION_PROBABILITY,
            "moonPhase" to StudyIndexType.MOON_PHASE,
        )
        val POLLUTANT_TYPES = mapOf(
            "PM10" to StudyIndexType.PM10,
            "PM2.5" to StudyIndexType.PM2_5,
            "PM25" to StudyIndexType.PM2_5,
        )
    }
}

/** Corresponds conceptually to `…twc.sub.TwcInsightConverter`. */
@Singleton
class StudyProviderAInsightConverter @Inject constructor() {
    fun convert(dtos: List<StudyInsightDto>?): List<StudyInsightContent> =
        dtos.skipNulls().mapIndexed { index, dto ->
            StudyInsightContent(
                insightType = dto.type ?: 0,
                order = dto.order ?: index,
                card = StudyInsightCard(
                    title = dto.title.orEmpty(),
                    content = dto.text.orEmpty(),
                    shortContent = dto.shortText.orEmpty(),
                    defaultContent = dto.defaultText.orEmpty(),
                    timeDescription = dto.timeDescription.orEmpty(),
                    url = dto.link.orEmpty(),
                ),
                showDefault = dto.defaultText?.isNotEmpty() == true,
                showDetail = dto.showDetail ?: true,
                showWidget = dto.showWidget ?: false,
                showNotification = dto.showNotification ?: false,
                expireTime = dto.expireTime ?: Long.MAX_VALUE,
            )
        }
}

/** Corresponds conceptually to `…twc.sub.TwcVideoConverter` and `TwcRadarConverter`. */
@Singleton
class StudyProviderAVideoConverter @Inject constructor() {
    fun convert(dto: StudyLocalWeatherDto): List<StudyWebContent> = emptyList<StudyWebContent>()
        .also { /* provider A's video payload arrives on a separate endpoint shape */ }

    fun toRadar(url: String?): StudyWebContent? = url?.takeIf { it.isNotEmpty() }?.let {
        StudyWebContent(id = "radar", type = StudyContentType.RADAR, url = it)
    }
}

/** Corresponds conceptually to `…twc.sub.TwcSearchConverter`. */
@Singleton
class StudyProviderASearchConverter @Inject constructor(
    private val locationConverter: StudyProviderALocationConverter,
) {
    fun convert(dto: StudySearchDto): List<StudyLocation> =
        dto.results.skipNulls().map { locationConverter.convert(it, StudyLocation(key = "")) }
}
