package dev.local.weatherstudy.network.gateway

import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.domain.type.StudyInsightType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns the public source's documents into the reconstruction's own wire shape —
 * `StudyLocalWeatherDto` as JSON. This is the server-side half of the gateway: it decides
 * icon codes, phrases, index levels and insight cards, which is what a weather backend
 * does and what the per-provider converters then normalise.
 *
 * Icon codes are emitted in provider A's 0..40 external range so that
 * `StudyProviderACodeConverter` and `StudyAssignIconNum` do real work on them.
 */
internal class StudyGatewayForecastComposer {

    fun location(place: StudyGatewayPlace): JSONObject = JSONObject()
        .put("placeId", place.placeId)
        .put("city", place.city)
        .put("state", place.state)
        .put("country", place.country)
        .put("countryCode", place.countryCode)
        .put("latitude", place.latitude)
        .put("longitude", place.longitude)
        .put(
            "timeZone",
            JSONObject()
                .put("name", place.timeZone)
                .put("offset", place.utcOffsetSeconds / SECONDS_PER_HOUR),
        )

    fun compose(place: StudyGatewayPlace, weather: JSONObject, air: JSONObject?, now: Long): JSONObject {
        val current = weather.getJSONObject("current")
        val hourly = weather.getJSONObject("hourly")
        val daily = weather.getJSONObject("daily")

        val dailyTimes = daily.getJSONArray("time")
        val todayIndex = (0 until dailyTimes.length())
            .lastOrNull { dailyTimes.getLong(it) * MILLIS <= now }
            ?: 0
        val isDay = current.optInt("is_day", 1) == 1
        val named = place.withFallbackName()

        return JSONObject()
            .put("location", location(named))
            .put("currentConditions", currentConditions(current, daily, todayIndex, isDay, now))
            .put("hourlyForecast", hourlyForecast(hourly, now))
            .put("dailyForecast", dailyForecast(daily, now))
            .put("lifeIndex", lifeIndex(current, daily, todayIndex, now))
            .put("airQuality", air?.optJSONObject("current")?.let(::airQuality) ?: JSONObject.NULL)
            .put("insight", insights(current, hourly, daily, todayIndex, now, named.timeZone))
            .put("links", JSONObject().put("attribution", ATTRIBUTION_URL))
            .put("expireTime", now + FORECAST_TTL)
    }

    // ---------------------------------------------------------------- current

    private fun currentConditions(
        current: JSONObject,
        daily: JSONObject,
        today: Int,
        isDay: Boolean,
        now: Long,
    ): JSONObject {
        val code = current.optInt("weather_code", 0)
        val chance = daily.optJSONArray("precipitation_probability_max").int(today)
        return JSONObject()
            .put("observationTime", current.optLong("time") * MILLIS)
            .put("iconCode", iconCode(code))
            .put("phrase", phrase(code, isDay))
            .put("narrative", narrative(code, isDay, chance))
            .put("temperature", unit(current.optDouble("temperature_2m")))
            .put("feelsLike", unit(current.optDouble("apparent_temperature")))
            .put("temperatureMax", unit(daily.optJSONArray("temperature_2m_max").double(today)))
            .put("temperatureMin", unit(daily.optJSONArray("temperature_2m_min").double(today)))
            .put("sunrise", daily.optJSONArray("sunrise").millis(today))
            .put("sunset", daily.optJSONArray("sunset").millis(today))
            .put("dayOrNight", if (isDay) DAY else NIGHT)
            .put("expireTime", now + OBSERVATION_TTL)
    }

    // ---------------------------------------------------------------- hourly

    private fun hourlyForecast(hourly: JSONObject, now: Long): JSONArray {
        val times = hourly.getJSONArray("time")
        val start = (0 until times.length())
            .lastOrNull { times.getLong(it) * MILLIS <= now }
            ?: 0
        val result = JSONArray()
        for (i in start until minOf(start + HOURLY_COUNT, times.length())) {
            val code = hourly.optJSONArray("weather_code").int(i) ?: 0
            val isDay = (hourly.optJSONArray("is_day").int(i) ?: 1) == 1
            val amount = hourly.optJSONArray("precipitation").double(i) ?: 0.0
            result.put(
                JSONObject()
                    .put("validTime", times.getLong(i) * MILLIS)
                    .put("iconCode", iconCode(code))
                    .put("phrase", phrase(code, isDay))
                    .put("temperature", unit(hourly.optJSONArray("temperature_2m").double(i)))
                    .put("precipChance", hourly.optJSONArray("precipitation_probability").int(i) ?: 0)
                    .put("precipType", precipType(code, amount))
                    .put("precipAmount", unit(amount))
                    .put("windSpeed", unit(hourly.optJSONArray("wind_speed_10m").double(i)))
                    .put("windDirection", compass(hourly.optJSONArray("wind_direction_10m").double(i) ?: 0.0))
                    .put("humidity", hourly.optJSONArray("relative_humidity_2m").int(i) ?: 0)
                    .put("dayOrNight", if (isDay) DAY else NIGHT)
                    .put("expireTime", now + FORECAST_TTL),
            )
        }
        return result
    }

    // ---------------------------------------------------------------- daily

    private fun dailyForecast(daily: JSONObject, now: Long): JSONArray {
        val times = daily.getJSONArray("time")
        val result = JSONArray()
        for (i in 0 until times.length()) {
            val code = daily.optJSONArray("weather_code").int(i) ?: 0
            val chance = daily.optJSONArray("precipitation_probability_max").int(i) ?: 0
            result.put(
                JSONObject()
                    .put("validTime", times.getLong(i) * MILLIS)
                    .put("temperatureMax", unit(daily.optJSONArray("temperature_2m_max").double(i)))
                    .put("temperatureMin", unit(daily.optJSONArray("temperature_2m_min").double(i)))
                    .put("sunrise", daily.optJSONArray("sunrise").millis(i))
                    .put("sunset", daily.optJSONArray("sunset").millis(i))
                    .put("day", dayNight(code, isDay = true, chance = chance))
                    .put("night", dayNight(code, isDay = false, chance = chance))
                    .put("expireTime", now + FORECAST_TTL),
            )
        }
        return result
    }

    private fun dayNight(code: Int, isDay: Boolean, chance: Int) = JSONObject()
        .put("iconCode", iconCode(code))
        .put("phrase", phrase(code, isDay))
        .put("narrative", narrative(code, isDay, chance))
        .put("precipChance", chance)

    // ---------------------------------------------------------------- indices

    private fun lifeIndex(current: JSONObject, daily: JSONObject, today: Int, now: Long): JSONArray {
        val result = JSONArray()

        val uv = current.optDouble("uv_index", 0.0).takeUnless { it.isNaN() } ?: 0.0
        val uvMax = daily.optJSONArray("uv_index_max").double(today) ?: uv
        result.put(
            index("uv", uv, uvLevel(uv), uvLevelText(uv))
                .put("description", "Peaks at ${uvMax.roundToInt()} today. ${uvAdvice(uvMax)}"),
        )

        val humidity = current.optDouble("relative_humidity_2m", 0.0)
        val dewPoint = current.optDouble("dew_point_2m", 0.0)
        result.put(
            index("humidity", humidity, 0, humidityText(humidity))
                .put("description", "The air feels ${humidityText(humidity).lowercase(Locale.US)} right now."),
        )

        val pressure = current.optDouble("pressure_msl", 0.0)
        result.put(
            index("pressure", pressure, StudyIndexLevel.Pressure.STEADY, pressureText(pressure))
                .put("description", "Sea-level pressure is ${pressureText(pressure).lowercase(Locale.US)}."),
        )

        val wind = current.optDouble("wind_speed_10m", 0.0)
        val bearing = current.optDouble("wind_direction_10m", 0.0)
        result.put(
            index("wind", wind, 0, compass(bearing))
                .put("extra", bearing.roundToInt().toString())
                .put("description", "${windText(wind)} from the ${compass(bearing)}."),
        )

        val visibilityKm = current.optDouble("visibility", 0.0) / METERS_PER_KM
        result.put(
            index("visibility", visibilityKm, 0, visibilityText(visibilityKm))
                .put("description", "Visibility is ${visibilityText(visibilityKm).lowercase(Locale.US)}."),
        )

        result.put(
            index("dewPoint", dewPoint, 0, dewPointText(dewPoint))
                .put("description", "The dew point makes it feel ${dewPointText(dewPoint).lowercase(Locale.US)}."),
        )

        result.put(index("precipAmount", daily.optJSONArray("precipitation_sum").double(today) ?: 0.0, 0, ""))
        result.put(
            index(
                "precipChance",
                (daily.optJSONArray("precipitation_probability_max").int(today) ?: 0).toDouble(),
                0,
                "",
            ),
        )

        // the moon is computed, not fetched: phase as the level, illumination as the value
        val moon = StudyGatewayMoon.at(now)
        result.put(index("moonPhase", moon.illumination, moon.phase, ""))
        return result
    }

    private fun index(type: String, value: Double, level: Int, levelText: String) = JSONObject()
        .put("type", type)
        .put("value", value)
        .put("level", level)
        .put("levelText", levelText)

    private fun airQuality(current: JSONObject): JSONObject {
        val aqi = current.optInt("us_aqi", 0)
        val pollutants = JSONArray()
            .put(pollutant("PM10", current.optDouble("pm10", 0.0)))
            .put(pollutant("PM2.5", current.optDouble("pm2_5", 0.0)))
        return JSONObject()
            .put("index", aqi)
            .put("scale", "EPA")
            .put("category", aqiCategory(aqi))
            .put("level", aqiLevel(aqi))
            .put("pollutants", pollutants)
    }

    private fun pollutant(name: String, amount: Double) = JSONObject()
        .put("name", name)
        .put("amount", amount)
        .put("unit", "µg/m³")

    // ---------------------------------------------------------------- insights

    private fun insights(
        current: JSONObject,
        hourly: JSONObject,
        daily: JSONObject,
        today: Int,
        now: Long,
        timeZone: String,
    ): JSONArray {
        val result = JSONArray()
        var order = 0
        fun add(type: Int, title: String, text: String, timeDescription: String = "") {
            result.put(
                JSONObject()
                    .put("type", type)
                    .put("order", order++)
                    .put("title", title)
                    .put("text", text)
                    .put("shortText", title)
                    .put("timeDescription", timeDescription)
                    .put("showDetail", true)
                    .put("showWidget", order == 1)
                    .put("expireTime", now + FORECAST_TTL),
            )
        }

        // 1. the next hour that is likely to be wet, within half a day
        val times = hourly.getJSONArray("time")
        val start = (0 until times.length()).lastOrNull { times.getLong(it) * MILLIS <= now } ?: 0
        val wetHour = (start until minOf(start + INSIGHT_LOOKAHEAD_HOURS, times.length())).firstOrNull {
            (hourly.optJSONArray("precipitation_probability").int(it) ?: 0) >= LIKELY_PERCENT
        }
        if (wetHour != null) {
            val chance = hourly.optJSONArray("precipitation_probability").int(wetHour) ?: 0
            val at = hourLabel(times.getLong(wetHour) * MILLIS, timeZone)
            val isSnow = precipType(hourly.optJSONArray("weather_code").int(wetHour) ?: 0, 1.0) == "snow"
            add(
                if (isSnow) StudyInsightType.SNOW_FALL else StudyInsightType.PRECIPITATION,
                if (wetHour == start) "${if (isSnow) "Snow" else "Rain"} likely now"
                else "${if (isSnow) "Snow" else "Rain"} likely around $at",
                "$chance% chance of precipitation. Consider taking an umbrella.",
                at,
            )
        } else {
            add(
                StudyInsightType.SUNNYDAY,
                "No rain expected",
                "It should stay dry for the next $INSIGHT_LOOKAHEAD_HOURS hours.",
            )
        }

        // 2. apparent temperature that differs noticeably from the measured one
        val temperature = current.optDouble("temperature_2m")
        val apparent = current.optDouble("apparent_temperature")
        if (!temperature.isNaN() && !apparent.isNaN() && abs(apparent - temperature) >= FEELS_LIKE_GAP) {
            val warmer = apparent > temperature
            add(
                StudyInsightType.FEELS_LIKE_TEMPERATURE,
                if (warmer) "Feels warmer than it is" else "Feels cooler than it is",
                if (warmer) "Humidity is making it feel hotter than the thermometer says."
                else "Wind is making it feel colder than the thermometer says.",
            )
        }

        // 3. a high UV day
        val uvMax = daily.optJSONArray("uv_index_max").double(today) ?: 0.0
        if (uvMax >= UV_HIGH) {
            add(StudyInsightType.UV, "${uvLevelText(uvMax)} UV today", uvAdvice(uvMax))
        }

        // 4. today against yesterday
        val todayMax = daily.optJSONArray("temperature_2m_max").double(today)
        val yesterdayMax = daily.optJSONArray("temperature_2m_max").double(today - 1)
        if (todayMax != null && yesterdayMax != null && abs(todayMax - yesterdayMax) >= DAY_CHANGE_GAP) {
            add(
                StudyInsightType.TEMPERATURE_CHANGE,
                if (todayMax > yesterdayMax) "Warmer than yesterday" else "Cooler than yesterday",
                "Today's high differs noticeably from yesterday's.",
            )
        }

        // 5. tomorrow in a sentence
        val tomorrowCode = daily.optJSONArray("weather_code").int(today + 1)
        if (tomorrowCode != null) {
            val chance = daily.optJSONArray("precipitation_probability_max").int(today + 1) ?: 0
            add(
                StudyInsightType.TOMORROW_COMMENT,
                "Tomorrow: ${phrase(tomorrowCode, true).lowercase(Locale.US)}",
                narrative(tomorrowCode, true, chance),
                "Tomorrow",
            )
        }
        return result
    }

    // ---------------------------------------------------------------- vocabulary

    /** WMO weather code -> provider A's external icon code (0..40) */
    private fun iconCode(wmo: Int): String = when (wmo) {
        0 -> "0"
        1 -> "1"
        2 -> "3"
        3 -> "6"
        45, 48 -> "8"
        51, 53, 55, 56, 57 -> "9"
        61, 63, 65, 66, 67 -> "12"
        80, 81, 82 -> "14"
        71, 73, 75, 77 -> "20"
        85, 86 -> "24"
        95 -> "31"
        96, 99 -> "33"
        else -> "6"
    }

    private fun phrase(wmo: Int, isDay: Boolean): String = when (wmo) {
        0 -> if (isDay) "Sunny" else "Clear"
        1 -> if (isDay) "Mostly sunny" else "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80, 81 -> "Showers"
        82 -> "Heavy showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Cloudy"
    }

    private fun narrative(wmo: Int, isDay: Boolean, chance: Int?): String {
        val base = phrase(wmo, isDay)
        return when {
            chance == null -> "$base."
            chance >= LIKELY_PERCENT -> "$base with a $chance% chance of precipitation."
            chance > 0 -> "$base. A $chance% chance of precipitation."
            else -> "$base. No precipitation expected."
        }
    }

    private fun precipType(wmo: Int, amount: Double): String = when {
        wmo in SNOW_CODES -> "snow"
        wmo in STORM_CODES -> "storm"
        wmo in RAIN_CODES || amount > 0.0 -> "rain"
        else -> "none"
    }

    private fun compass(degree: Double): String {
        val points = StudyIndexLevel.WindDirection.COMPASS
        val step = FULL_CIRCLE / points.size
        return points[(((degree % FULL_CIRCLE) + step / 2) / step).toInt() % points.size]
    }

    private fun uvLevel(uv: Double) = when {
        uv < 1 -> StudyIndexLevel.Uv.VERY_LOW
        uv < 3 -> StudyIndexLevel.Uv.LOW
        uv < UV_HIGH -> StudyIndexLevel.Uv.NORMAL
        uv < 8 -> StudyIndexLevel.Uv.HIGH
        uv < 11 -> StudyIndexLevel.Uv.VERY_HIGH
        else -> StudyIndexLevel.Uv.EXTREME
    }

    private fun uvLevelText(uv: Double) = when (uvLevel(uv)) {
        StudyIndexLevel.Uv.VERY_LOW -> "Very low"
        StudyIndexLevel.Uv.LOW -> "Low"
        StudyIndexLevel.Uv.NORMAL -> "Moderate"
        StudyIndexLevel.Uv.HIGH -> "High"
        StudyIndexLevel.Uv.VERY_HIGH -> "Very high"
        else -> "Extreme"
    }

    private fun uvAdvice(uv: Double) = when {
        uv < 3 -> "No protection needed."
        uv < UV_HIGH -> "Sun protection is a good idea around midday."
        uv < 8 -> "Wear sunscreen and seek shade around midday."
        else -> "Avoid the midday sun and cover up."
    }

    private fun humidityText(value: Double) = when {
        value < 30 -> "Dry"
        value < 60 -> "Comfortable"
        value < 80 -> "Humid"
        else -> "Very humid"
    }

    private fun pressureText(hPa: Double) = when {
        hPa < 1000 -> "Low"
        hPa < 1020 -> "Normal"
        else -> "High"
    }

    private fun windText(kph: Double) = when {
        kph < 2 -> "Calm"
        kph < 12 -> "Light breeze"
        kph < 29 -> "Moderate breeze"
        kph < 50 -> "Strong breeze"
        kph < 75 -> "Gale"
        else -> "Storm-force wind"
    }

    private fun visibilityText(km: Double) = when {
        km < 1 -> "Very poor"
        km < 4 -> "Poor"
        km < 10 -> "Moderate"
        else -> "Clear"
    }

    private fun dewPointText(celsius: Double) = when {
        celsius < 10 -> "Dry"
        celsius < 16 -> "Comfortable"
        celsius < 21 -> "Sticky"
        else -> "Muggy"
    }

    private fun aqiLevel(aqi: Int) = when {
        aqi <= 50 -> StudyIndexLevel.Air.GOOD
        aqi <= 100 -> StudyIndexLevel.Air.NORMAL
        aqi <= 150 -> StudyIndexLevel.Air.UNHEALTHY_FOR_SENSITIVE
        aqi <= 200 -> StudyIndexLevel.Air.UNHEALTHY
        aqi <= 300 -> StudyIndexLevel.Air.VERY_UNHEALTHY
        else -> StudyIndexLevel.Air.HAZARDOUS
    }

    private fun aqiCategory(aqi: Int) = when (aqiLevel(aqi)) {
        StudyIndexLevel.Air.GOOD -> "Good"
        StudyIndexLevel.Air.NORMAL -> "Moderate"
        StudyIndexLevel.Air.UNHEALTHY_FOR_SENSITIVE -> "Unhealthy for sensitive groups"
        StudyIndexLevel.Air.UNHEALTHY -> "Unhealthy"
        StudyIndexLevel.Air.VERY_UNHEALTHY -> "Very unhealthy"
        else -> "Hazardous"
    }

    private fun hourLabel(epochMillis: Long, timeZone: String): String =
        SimpleDateFormat("h a", Locale.US)
            .apply { if (timeZone.isNotEmpty()) this.timeZone = TimeZone.getTimeZone(timeZone) }
            .format(Date(epochMillis))

    /** a coordinate the geocoder could not name still needs something to show */
    private fun StudyGatewayPlace.withFallbackName(): StudyGatewayPlace =
        if (city.isNotEmpty()) this
        else copy(city = timeZone.substringAfterLast('/').replace('_', ' ').ifEmpty { "My location" })

    private fun unit(value: Double?): Any =
        if (value == null || value.isNaN()) JSONObject.NULL else JSONObject().put("metric", value)

    private fun JSONArray?.double(index: Int): Double? =
        if (this == null || index !in 0 until length() || isNull(index)) null else getDouble(index)

    private fun JSONArray?.int(index: Int): Int? = double(index)?.roundToInt()

    private fun JSONArray?.millis(index: Int): Any =
        if (this == null || index !in 0 until length() || isNull(index)) JSONObject.NULL
        else getLong(index) * MILLIS

    private companion object {
        const val MILLIS = 1000L
        const val SECONDS_PER_HOUR = 3600.0
        const val METERS_PER_KM = 1000.0
        const val FULL_CIRCLE = 360.0
        const val HOURLY_COUNT = 48
        const val INSIGHT_LOOKAHEAD_HOURS = 12
        const val LIKELY_PERCENT = 50
        const val FEELS_LIKE_GAP = 3.0
        const val DAY_CHANGE_GAP = 2.0
        const val UV_HIGH = 6.0
        const val DAY = "D"
        const val NIGHT = "N"
        const val OBSERVATION_TTL = 30L * 60L * 1000L
        const val FORECAST_TTL = 3L * 60L * 60L * 1000L
        const val ATTRIBUTION_URL = "https://open-meteo.com/"
        val RAIN_CODES = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
        val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
        val STORM_CODES = setOf(95, 96, 99)
    }
}

/**
 * Moon phase from the synodic month — no network involved. Accurate to well within the
 * one-in-eight resolution the moon card draws at.
 */
internal object StudyGatewayMoon {

    data class Moon(val phase: Int, val illumination: Double)

    fun at(epochMillis: Long): Moon {
        val days = (epochMillis - REFERENCE_NEW_MOON) / MILLIS_PER_DAY
        val age = ((days % SYNODIC_MONTH) + SYNODIC_MONTH) % SYNODIC_MONTH
        val fraction = age / SYNODIC_MONTH
        val phase = (floor(fraction * PHASE_COUNT + 0.5).toInt() % PHASE_COUNT) + 1
        val illumination = (1 - cos(2 * PI * fraction)) / 2
        return Moon(phase = phase, illumination = illumination)
    }

    /** 2000-01-06 18:14 UTC, a well-documented new moon */
    private const val REFERENCE_NEW_MOON = 947_182_440_000L
    private const val SYNODIC_MONTH = 29.530588853
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val PHASE_COUNT = 8
}
