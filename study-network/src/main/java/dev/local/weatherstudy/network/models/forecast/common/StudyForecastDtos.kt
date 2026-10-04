package dev.local.weatherstudy.network.models.forecast.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 298 DTO classes in
 * com.samsung.android.weather.network.models.forecast
 *
 * ### The shape being preserved, and the documented reduction
 *
 * The original has **five parallel DTO families**, one per backend, prefixed by
 * provider. The counts recovered from the APK:
 *
 * ```
 * Twc*  ≈ 45    Wjp*  ≈ 30    Wkr*  ≈ 33    Hua*  ≈ 28    SRC*  ≈ 31
 * ```
 *
 * plus shared `SubModel` and `NullSkipJsonAdapter`. The families are near-identical in
 * purpose and completely separate in code — `TwcLocalWeather`, `WjpLocalWeather`,
 * `WkrLocalWeather`, `HuaLocalWeather`, `SRCLocalWeather` are five distinct types with
 * five distinct converters, because each backend's JSON differs in nesting and units.
 *
 * **That parallel-families-plus-per-provider-converter shape is the architecture, and it
 * is what is reconstructed.** Reproducing all 298 near-duplicate data classes would add
 * volume without adding understanding, so this file carries one representative family
 * and `reports/model-map.md` lists the full original set with the four-stage flow each
 * one participates in. This reduction is declared in `reports/class-mapping.md` §18.
 *
 * Two original mechanisms kept verbatim in spirit:
 * - **`SubModel`** — the providers wrap payloads in an envelope, so the DTOs are
 *   layered rather than flat; [StudyEnvelopeDto] reconstructs that.
 * - **`NullSkipJsonAdapter`** — the feeds send nulls inside arrays, and the original
 *   has a dedicated adapter to drop them. [StudyNullSkip] marks where.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyLocalWeatherDto(
    @Json(name = "location") val location: StudyLocationDto? = null,
    @Json(name = "currentConditions") val currentConditions: StudyCurrentConditionsDto? = null,
    @Json(name = "hourlyForecast") val hourlyForecast: List<StudyHourlyForecastDto>? = null,
    @Json(name = "dailyForecast") val dailyForecast: List<StudyDailyForecastDto>? = null,
    @Json(name = "lifeIndex") val lifeIndex: List<StudyLifeIndexDto>? = null,
    @Json(name = "alerts") val alerts: List<StudyAlertDto>? = null,
    @Json(name = "airQuality") val airQuality: StudyAirQualityDto? = null,
    @Json(name = "insight") val insight: List<StudyInsightDto>? = null,
    @Json(name = "links") val links: Map<String, String>? = null,
    @Json(name = "expireTime") val expireTime: Long? = null,
)

/**
 * Corresponds conceptually to `TwcLocation` / `WjpLocation` / `WkrLocation` /
 * `HuaLocation` / `SRCLocation` and `TwcGeoPosition` / `HuaGeoPosition` / `SRCGeoPosition`.
 */
@JsonClass(generateAdapter = true)
data class StudyLocationDto(
    @Json(name = "placeId") val placeId: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "countryCode") val countryCode: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timeZone") val timeZone: StudyTimeZoneDto? = null,
)

/** Corresponds conceptually to `TwcTimeZone` / `HuaTimeZone` / `SRCTimeZone`. */
@JsonClass(generateAdapter = true)
data class StudyTimeZoneDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "offset") val offset: Float? = null,
    @Json(name = "isDaylightSaving") val isDaylightSaving: Boolean? = null,
)

/** Corresponds conceptually to `TwcCurrentObservation` / `*CurrentConditions`. */
@JsonClass(generateAdapter = true)
data class StudyCurrentConditionsDto(
    @Json(name = "observationTime") val observationTime: Long? = null,
    @Json(name = "iconCode") val iconCode: String? = null,
    @Json(name = "phrase") val phrase: String? = null,
    @Json(name = "narrative") val narrative: String? = null,
    @Json(name = "temperature") val temperature: StudyUnitValueDto? = null,
    @Json(name = "feelsLike") val feelsLike: StudyUnitValueDto? = null,
    @Json(name = "temperatureMax") val temperatureMax: StudyUnitValueDto? = null,
    @Json(name = "temperatureMin") val temperatureMin: StudyUnitValueDto? = null,
    @Json(name = "sunrise") val sunrise: Long? = null,
    @Json(name = "sunset") val sunset: Long? = null,
    @Json(name = "moonrise") val moonrise: Long? = null,
    @Json(name = "moonset") val moonset: Long? = null,
    @Json(name = "dayOrNight") val dayOrNight: String? = null,
    @Json(name = "expireTime") val expireTime: Long? = null,
)

/**
 * Corresponds conceptually to the `*MetricImperial` / `*UnitValue` / `*ValueUnit` /
 * `*TemperatureUnit` / `*WindSpeedUnit` / `*PressureTendencyUnit` family — **28 of the
 * 298 DTOs are unit wrappers**.
 *
 * Observed responsibility and why it matters: the backends send every measurement in
 * both metric and imperial, each tagged with its unit, rather than in one canonical
 * unit. So unit conversion is a *parsing* concern here, not a formatting concern in the
 * UI — which is why `:study-ui-common` has a 27-class `usecase/notation` package doing
 * presentation only.
 */
@JsonClass(generateAdapter = true)
data class StudyUnitValueDto(
    @Json(name = "metric") val metric: Double? = null,
    @Json(name = "imperial") val imperial: Double? = null,
    @Json(name = "unit") val unit: String? = null,
)

/** Corresponds conceptually to `*ForecastHour` / `*HourlyForecast`. */
@JsonClass(generateAdapter = true)
data class StudyHourlyForecastDto(
    @Json(name = "validTime") val validTime: Long? = null,
    @Json(name = "iconCode") val iconCode: String? = null,
    @Json(name = "phrase") val phrase: String? = null,
    @Json(name = "temperature") val temperature: StudyUnitValueDto? = null,
    @Json(name = "precipChance") val precipChance: Int? = null,
    @Json(name = "precipType") val precipType: String? = null,
    @Json(name = "precipAmount") val precipAmount: StudyUnitValueDto? = null,
    @Json(name = "windSpeed") val windSpeed: StudyUnitValueDto? = null,
    @Json(name = "windDirection") val windDirection: String? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "dayOrNight") val dayOrNight: String? = null,
    @Json(name = "expireTime") val expireTime: Long? = null,
)

/**
 * Corresponds conceptually to `*ForecastDay` / `*DailyForecast` and the `*DayNight`
 * pair type.
 *
 * Observed responsibility: a day row nests a `day` and a `night` object — the schema's
 * doubled columns and the domain's `StudyDailyObservation.dayCondition` /
 * `nightCondition` both come from this shape.
 */
@JsonClass(generateAdapter = true)
data class StudyDailyForecastDto(
    @Json(name = "validTime") val validTime: Long? = null,
    @Json(name = "temperatureMax") val temperatureMax: StudyUnitValueDto? = null,
    @Json(name = "temperatureMin") val temperatureMin: StudyUnitValueDto? = null,
    @Json(name = "sunrise") val sunrise: Long? = null,
    @Json(name = "sunset") val sunset: Long? = null,
    @Json(name = "day") val day: StudyDayNightDto? = null,
    @Json(name = "night") val night: StudyDayNightDto? = null,
    @Json(name = "expireTime") val expireTime: Long? = null,
)

/** Corresponds conceptually to `TwcDayNight` / `HuaDayNight` / `SRCDayNight`. */
@JsonClass(generateAdapter = true)
data class StudyDayNightDto(
    @Json(name = "iconCode") val iconCode: String? = null,
    @Json(name = "phrase") val phrase: String? = null,
    @Json(name = "narrative") val narrative: String? = null,
    @Json(name = "precipChance") val precipChance: Int? = null,
)

/** Corresponds conceptually to `*LifeIndex`, `*IndexCategory`, `*NewLifeIndex`. */
@JsonClass(generateAdapter = true)
data class StudyLifeIndexDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "value") val value: Double? = null,
    @Json(name = "level") val level: Int? = null,
    @Json(name = "levelText") val levelText: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "unit") val unit: String? = null,
    @Json(name = "extra") val extra: String? = null,
    @Json(name = "link") val link: String? = null,
)

/** Corresponds conceptually to `TwcAlert` / `WjpAlert` / `WkrAlert` / `SRCAlerts`. */
@JsonClass(generateAdapter = true)
data class StudyAlertDto(
    @Json(name = "detailKey") val detailKey: String? = null,
    @Json(name = "eventDescription") val eventDescription: String? = null,
    @Json(name = "severity") val severity: Int? = null,
    @Json(name = "significance") val significance: String? = null,
    @Json(name = "issueTime") val issueTime: String? = null,
    @Json(name = "issueTimeZone") val issueTimeZone: String? = null,
    @Json(name = "expireTime") val expireTime: Long? = null,
    @Json(name = "link") val link: String? = null,
)

/**
 * Corresponds conceptually to `TwcAqi`, `TwcGlobalAirQuality`, `TwcAqiPollutants`,
 * `TwcPollutantsItem`, `HuaAirQuality`, `SRCAirQuality`.
 *
 * Observed responsibility: the AQI payload is scale-dependent — `scale` names which
 * national index is in force, which is why `StudyIndexLevel.AqiScale` exists and why
 * the app has four regional `GetAqiGraphViewEntity` variants.
 */
@JsonClass(generateAdapter = true)
data class StudyAirQualityDto(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "scale") val scale: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "level") val level: Int? = null,
    @Json(name = "pollutants") val pollutants: List<StudyPollutantDto>? = null,
)

/** Corresponds conceptually to `TwcPollutantsItem`. */
@JsonClass(generateAdapter = true)
data class StudyPollutantDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "unit") val unit: String? = null,
    @Json(name = "index") val index: Int? = null,
    @Json(name = "level") val level: Int? = null,
)

/** Corresponds conceptually to `TwcInsight`, `TwcInsightSupplement`, `*InsightContent`. */
@JsonClass(generateAdapter = true)
data class StudyInsightDto(
    @Json(name = "type") val type: Int? = null,
    @Json(name = "order") val order: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "shortText") val shortText: String? = null,
    @Json(name = "defaultText") val defaultText: String? = null,
    @Json(name = "timeDescription") val timeDescription: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "showNotification") val showNotification: Boolean? = null,
    @Json(name = "showWidget") val showWidget: Boolean? = null,
    @Json(name = "showDetail") val showDetail: Boolean? = null,
    @Json(name = "additionalInfo") val additionalInfo: Map<String, Any?>? = null,
    @Json(name = "expireTime") val expireTime: Long? = null,
)

/** Corresponds conceptually to `TwcSearch` / `TwcGeoSearch` / `*Search` / `TwcLocationList`. */
@JsonClass(generateAdapter = true)
data class StudySearchDto(
    @Json(name = "results") val results: List<StudyLocationDto>? = null,
    @Json(name = "count") val count: Int? = null,
)

/**
 * Corresponds conceptually to `com.samsung.android.weather.network.models.SubModel`.
 *
 * Observed responsibility: the providers wrap payloads in an envelope with status and
 * metadata beside the data. The original has a dedicated type for it, which is why the
 * DTOs are layered rather than flat.
 */
@JsonClass(generateAdapter = true)
data class StudyEnvelopeDto<T>(
    @Json(name = "status") val status: String? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null,
)

/**
 * Corresponds conceptually to `com.samsung.android.weather.network.models.NullSkipJsonAdapter`.
 *
 * Observed responsibility: the feeds send `null` entries *inside* arrays, and the
 * original ships an adapter that drops them rather than failing the parse. Reconstructed
 * as an annotation marker plus the helper the converters call — the behaviour is what
 * matters, not the adapter plumbing.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class StudyNullSkip

/** Drops nulls from a provider array, as the original's `NullSkipJsonAdapter` does. */
fun <T> List<T?>?.skipNulls(): List<T> = this?.filterNotNull() ?: emptyList()
