package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.WeatherEntity
 *
 * Table `TABLE_WEATHER_INFO`, 65 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibilities:
 * - the root row: one per saved location, keyed by the provider's location key
 * - **it is flat and denormalised.** 65 columns inline what the domain models as
 *   three separate objects: the location (name/state/country/coords/label), the
 *   current observation (temps, icon, text, sun/moon times), AND the whole day/night
 *   precipitation matrix (4 types x amount+probability x day+night = 16 columns).
 *   The domain's `StudyWeather` is a graph; this row is a wide tuple. That gap is
 *   exactly what the mappers in `:study-persistence` exist to bridge, and it is why
 *   reading one location is a single-row query rather than a join.
 * - three icon columns per slot (`ICON_NUM`, `CONVERTED_ICON_NUM`,
 *   `EXPANSION_ICON_NUM`) persist all three stages of the icon pipeline, so a
 *   provider change can re-derive without refetching
 * - every child table cascades from `COL_WEATHER_KEY`, so deleting a location
 *   deletes its hourly, daily, index, alert, content, insight and lifestyle rows in
 *   one statement
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_WEATHER_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY],
)
data class StudyWeatherEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_CONVERTED_ICON_NUM)
    val convertedIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_EXPANSION_ICON_NUM)
    val expansionIconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_TIME)
    val time: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_CURRENT_TEMP)
    val currentTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_WEATHER_TEXT)
    val weatherText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NAME)
    val name: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NAME_ENG)
    val nameEng: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_AQI_INDEX)
    val aqiIndex: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_STATE)
    val state: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_STATE_ENG)
    val stateEng: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_COUNTRY)
    val country: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_COUNTRY_ENG)
    val countryEng: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_COUNTRY_CODE)
    val countryCode: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_POSTAL_CODE)
    val postalCode: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LOCATION)
    val location: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LATITUDE)
    val latitude: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LONGITUDE)
    val longitude: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_THEME_CODE)
    val themeCode: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_TIMEZONE)
    val timeZone: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_IANA_TIMEZONE)
    val ianaTimeZone: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_IS_DAYLIGHT_SAVING)
    val isDaylightSaving: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_UPDATE_TIME)
    val updateTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_SUNRISE_TIME)
    val sunRiseTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_SUNSET_TIME)
    val sunSetTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_MOONRISE_TIME)
    val moonRiseTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_MOONSET_TIME)
    val moonSetTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_IS_DAY_OR_NIGHT)
    val isDayOrNight: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_FEELSLIKE_TEMP)
    val feelsLikeTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_HIGH_TEMP)
    val highTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LOW_TEMP)
    val lowTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_YESTERDAY_HIGH_TEMP)
    val yesterdayHighTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_YESTERDAY_LOW_TEMP)
    val yesterdayLowTemp: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_ICON_NUM)
    val iconNum: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_FORECAST_TEXT)
    val forecastText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_RAIN_PROBABILITY)
    val dayRainProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_SNOW_PROBABILITY)
    val daySnowProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_HAIL_PROBABILITY)
    val dayHailProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_PRECIPITATION_PROBABILITY)
    val dayPrecipitationProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_RAIN_AMOUNT)
    val dayRainAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_SNOW_AMOUNT)
    val daySnowAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_HAIL_AMOUNT)
    val dayHailAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_DAY_PRECIPITATION_AMOUNT)
    val dayPrecipitationAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_RAIN_PROBABILITY)
    val nightRainProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_SNOW_PROBABILITY)
    val nightSnowProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_HAIL_PROBABILITY)
    val nightHailProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_PRECIPITATION_PROBABILITY)
    val nightPrecipitationProbability: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_RAIN_AMOUNT)
    val nightRainAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_SNOW_AMOUNT)
    val nightSnowAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_HAIL_AMOUNT)
    val nightHailAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_NIGHT_PRECIPITATION_AMOUNT)
    val nightPrecipitationAmount: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_URL)
    val url: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_ORDER)
    val order: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_HAS_INDEX)
    val hasidx: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_PRIVACY)
    val privacy: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_BROADCAST)
    val broadcastUrl: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_10MIN)
    val tenminUrl: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_PROVIDER_NAME)
    val providerName: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_ARCTIC_NIGHT_TYPE)
    val arcticNightType: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_PUBLISH_TIME)
    val publishTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_EXPIRE_TIME)
    val expireTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LOCATION_UPDATE_TIME)
    val locationUpdateTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LOCATION_SHORT_ADDRESS)
    val locationShortAddress: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LOCATION_LABEL)
    val locationLabel: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_LOCATION_LABEL_TYPE)
    val locationLabelType: String? = null,
)
