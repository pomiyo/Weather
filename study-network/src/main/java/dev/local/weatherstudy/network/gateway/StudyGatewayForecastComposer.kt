package dev.local.weatherstudy.network.gateway

import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.domain.type.StudyInsightType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
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
            .put("currentConditions", currentConditions(named, current, daily, todayIndex, isDay, now))
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
        place: StudyGatewayPlace,
        current: JSONObject,
        daily: JSONObject,
        today: Int,
        isDay: Boolean,
        now: Long,
    ): JSONObject {
        val code = current.optInt("weather_code", 0)
        val chance = daily.optJSONArray("precipitation_probability_max").int(today)
        val moon = StudyGatewayMoon.riseSet(
            epochMillis = now,
            latitude = place.latitude,
            longitude = place.longitude,
            utcOffsetSeconds = place.utcOffsetSeconds,
        )
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
            // computed, like the phase: the gateway has no moon fields to forward
            .put("moonrise", moon.rise)
            .put("moonset", moon.set)
            .put("dayOrNight", if (isDay) DAY else NIGHT)
            .put("expireTime", now + OBSERVATION_TTL)
    }

    // ---------------------------------------------------------------- hourly

    /**
     * The strip starts at the NEXT hour, not the current one.
     *
     * `DetailHourlyCardStateProvider` takes `weather.hourlyObservations` whole and labels
     * every column with a clock time - there is no "Now" item type in
     * `DetailHourlyItemState` and no `now` string anywhere in the APK. The original's
     * first column is always the next hour because that is where its provider's feed
     * begins. This started at the hour CONTAINING now, which the app then labelled "Now",
     * so the strip opened with a column that repeated the header's current temperature and
     * every later column sat one place left of the original's.
     */
    private fun hourlyForecast(hourly: JSONObject, now: Long): JSONArray {
        val times = hourly.getJSONArray("time")
        val start = (0 until times.length())
            .firstOrNull { times.getLong(it) * MILLIS > now }
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

        // A service that does not measure visibility gets NO visibility index, rather than
        // one reading zero. The detail screen drops a tile whose index is absent and shows
        // "0 km" for one that is present and zero - and zero visibility is a real reading
        // (fog), so the two cannot be conflated. MET Norway's locationforecast has no
        // visibility field at all.
        if (current.has("visibility")) {
            val visibilityKm = current.optDouble("visibility", 0.0) / METERS_PER_KM
            result.put(
                index("visibility", visibilityKm, 0, visibilityText(visibilityKm))
                    .put("description", "Visibility is ${visibilityText(visibilityKm).lowercase(Locale.US)}."),
            )
        }

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
        // "Likely" means a probability past the threshold OR, for a service that publishes
        // no probability, an hour that is actually forecast to receive rain. Reading a
        // missing probability as zero put "No rain expected" over a forecast of 25.7 mm.
        val window = start until minOf(start + INSIGHT_LOOKAHEAD_HOURS, times.length())
        val wetHour = window.firstOrNull {
            val probability = hourly.optJSONArray("precipitation_probability").int(it)
            if (probability != null) {
                probability >= LIKELY_PERCENT
            } else {
                (hourly.optJSONArray("precipitation").double(it) ?: 0.0) >= WET_HOUR_MM
            }
        }
        if (wetHour != null) {
            val chance = hourly.optJSONArray("precipitation_probability").int(wetHour)
            val at = hourLabel(times.getLong(wetHour) * MILLIS, timeZone)
            val isSnow = precipType(hourly.optJSONArray("weather_code").int(wetHour) ?: 0, 1.0) == "snow"
            add(
                if (isSnow) StudyInsightType.SNOW_FALL else StudyInsightType.PRECIPITATION,
                if (wetHour == start) "${if (isSnow) "Snow" else "Rain"} likely now"
                else "${if (isSnow) "Snow" else "Rain"} likely around $at",
                if (chance != null) {
                    "$chance% chance of precipitation. Consider taking an umbrella."
                } else {
                    "Consider taking an umbrella."
                },
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
        /** an hour forecast to receive at least this much is "wet" when no chance is given */
        const val WET_HOUR_MM = 0.2
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
 * Moon phase from the synodic month, and moonrise / moonset from the moon's own position —
 * no network involved.
 *
 * ### Why the times are computed here
 *
 * The original's moon card has two columns because its forecast provider sends moonrise
 * and moonset. Open-Meteo sends neither, and the card was left with an empty right half
 * and a guideline hack to centre what remained — informative about the provider, useless
 * to the reader. Rise and set are not provider data in any meaningful sense though: they
 * are astronomy, computable from a date and a position, which is what this does.
 *
 * The position is Montenbruck & Pfleger's low-precision lunar series (the "MiniMoon"
 * formulation), good to about 0.3° — far inside the minute-level resolution a clock face
 * shows. The rise/set search is their standard one too: sample `sin(altitude) - sin(h0)`
 * every two hours over the local day, fit a parabola through each triple, and take the
 * roots. `h0 = +0.125°` is the moon's conventional rise altitude: parallax (≈0.95°) less
 * refraction (≈0.57°) less its own semidiameter (≈0.25°).
 *
 * Two days in a lunar month have no moonrise or no moonset at all — the moon rises ~50
 * minutes later each day, so one of the two events misses the local day entirely. That is
 * a real answer, not a failure, and it returns [NO_EVENT] so the card can leave that half
 * of the pair blank the way the original does when its provider omits one.
 */
internal object StudyGatewayMoon {

    data class Moon(val phase: Int, val illumination: Double)

    /** epoch millis, or [NO_EVENT] when the event does not occur on this local day */
    data class RiseSet(val rise: Long, val set: Long)

    const val NO_EVENT = 0L

    fun at(epochMillis: Long): Moon {
        val days = (epochMillis - REFERENCE_NEW_MOON) / MILLIS_PER_DAY
        val age = ((days % SYNODIC_MONTH) + SYNODIC_MONTH) % SYNODIC_MONTH
        val fraction = age / SYNODIC_MONTH
        val illumination = (1 - cos(2 * PI * fraction)) / 2
        return Moon(phase = phaseOf(fraction), illumination = illumination)
    }

    /**
     * The eight names are NOT eight equal eighths of the month.
     *
     * Rounding `fraction × 8` to the nearest name gives each of the four instants —
     * new, first quarter, full, last quarter — a 3.7-day window, so the app calls the moon
     * "last quarter" for nearly four days. The device's own Samsung Weather called the
     * same evening's moon a waning crescent while this said last quarter: not a different
     * moon, a different rounding.
     *
     * The four instants are *events*, not phases. They get a half day either side here —
     * `QUARTER_WINDOW` = 0.5 / 29.53 of the cycle — and the crescents and gibbouses fill
     * everything between, which is the convention almanacs use and what matched the
     * original on the day.
     */
    private fun phaseOf(fraction: Double): Int = when {
        fraction < QUARTER_WINDOW || fraction >= 1.0 - QUARTER_WINDOW -> NEW_MOON
        fraction < 0.25 - QUARTER_WINDOW -> WAXING_CRESCENT
        fraction < 0.25 + QUARTER_WINDOW -> FIRST_QUARTER
        fraction < 0.5 - QUARTER_WINDOW -> WAXING_GIBBOUS
        fraction < 0.5 + QUARTER_WINDOW -> FULL_MOON
        fraction < 0.75 - QUARTER_WINDOW -> WANING_GIBBOUS
        fraction < 0.75 + QUARTER_WINDOW -> LAST_QUARTER
        else -> WANING_CRESCENT
    }

    /**
     * Moonrise and moonset for the LOCAL day containing [epochMillis].
     *
     * [utcOffsetSeconds] is the place's offset, so the scan runs from local midnight to
     * local midnight — the same day boundary the sunrise and sunset in the response use.
     */
    fun riseSet(
        epochMillis: Long,
        latitude: Double,
        longitude: Double,
        utcOffsetSeconds: Int,
    ): RiseSet {
        val localMidnightUtc = floor(
            (epochMillis + utcOffsetSeconds * MILLIS_PER_SECOND) / MILLIS_PER_DAY,
        ) * MILLIS_PER_DAY - utcOffsetSeconds * MILLIS_PER_SECOND
        val mjd0 = localMidnightUtc / MILLIS_PER_DAY + MJD_AT_EPOCH
        val latRad = Math.toRadians(latitude)

        var rise = Double.NaN
        var set = Double.NaN
        var hour = 1.0
        var yMinus = sinAltitude(mjd0, hour - 1.0, longitude, latRad) - SIN_H0

        while (hour < HOURS_PER_DAY + 1 && (rise.isNaN() || set.isNaN())) {
            val y0 = sinAltitude(mjd0, hour, longitude, latRad) - SIN_H0
            val yPlus = sinAltitude(mjd0, hour + 1.0, longitude, latRad) - SIN_H0

            // the parabola through (-1, yMinus), (0, y0), (+1, yPlus)
            val a = 0.5 * (yMinus + yPlus) - y0
            val b = 0.5 * (yPlus - yMinus)
            val xExtreme = if (a == 0.0) 0.0 else -b / (2 * a)
            val yExtreme = (a * xExtreme + b) * xExtreme + y0
            val discriminant = b * b - 4 * a * y0

            if (discriminant >= 0 && a != 0.0) {
                val dx = 0.5 * sqrt(discriminant) / abs(a)
                var z1 = xExtreme - dx
                val z2 = xExtreme + dx
                val roots = mutableListOf<Double>()
                if (abs(z1) <= 1.0) roots += z1
                if (abs(z2) <= 1.0) roots += z2
                if (z1 < -1.0) z1 = z2
                when (roots.size) {
                    1 -> if (yMinus < 0) rise = hour + roots[0] else set = hour + roots[0]
                    2 -> {
                        if (yExtreme < 0) {
                            rise = hour + roots[1]
                            set = hour + roots[0]
                        } else {
                            rise = hour + roots[0]
                            set = hour + roots[1]
                        }
                    }
                }
            }
            hour += 2.0
            yMinus = yPlus
        }

        fun toMillis(h: Double) =
            if (h.isNaN()) NO_EVENT else localMidnightUtc.toLong() + (h * MILLIS_PER_HOUR).toLong()
        return RiseSet(rise = toMillis(rise), set = toMillis(set))
    }

    /** `sin(altitude)` of the moon's centre, [hour] hours after the day's start */
    private fun sinAltitude(mjd0: Double, hour: Double, longitude: Double, latRad: Double): Double {
        val mjd = mjd0 + hour / HOURS_PER_DAY
        val t = (mjd - MJD_J2000) / DAYS_PER_CENTURY
        val (raHours, decRad) = miniMoon(t)
        val tau = Math.toRadians(DEGREES_PER_HOUR * (localSiderealTime(mjd, longitude) - raHours))
        return sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(tau)
    }

    /**
     * Montenbruck & Pfleger's MiniMoon: the moon's right ascension (hours) and
     * declination (radians) from fifteen periodic terms.
     *
     * The series is in arcseconds and the three angles are kept in REVOLUTIONS until the
     * last moment — `frac` on a revolution is exact where a modulo on degrees accumulates
     * error over the ±100 years the series is valid for.
     */
    private fun miniMoon(t: Double): Pair<Double, Double> {
        val l0 = frac(0.606433 + 1336.855225 * t)
        val l = TWO_PI * frac(0.374897 + 1325.552410 * t)
        val ls = TWO_PI * frac(0.993133 + 99.997361 * t)
        val d = TWO_PI * frac(0.827361 + 1236.853086 * t)
        val f = TWO_PI * frac(0.259086 + 1342.227825 * t)

        val dLambda = 22640 * sin(l) - 4586 * sin(l - 2 * d) + 2370 * sin(2 * d) +
            769 * sin(2 * l) - 668 * sin(ls) - 412 * sin(2 * f) -
            212 * sin(2 * l - 2 * d) - 206 * sin(l + ls - 2 * d) +
            192 * sin(l + 2 * d) - 165 * sin(ls - 2 * d) - 125 * sin(d) -
            110 * sin(l + ls) + 148 * sin(l - ls) - 55 * sin(2 * f - 2 * d)

        val s = f + (dLambda + 412 * sin(2 * f) + 541 * sin(ls)) / ARCSECONDS_PER_RADIAN
        val h = f - 2 * d
        val n = -526 * sin(h) + 44 * sin(l + h) - 31 * sin(-l + h) - 23 * sin(ls + h) +
            11 * sin(-ls + h) - 25 * sin(-2 * l + f) + 21 * sin(-l + f)

        val lambda = TWO_PI * frac(l0 + dLambda / ARCSECONDS_PER_REVOLUTION)
        val beta = (18520.0 * sin(s) + n) / ARCSECONDS_PER_RADIAN

        val cosBeta = cos(beta)
        val x = cosBeta * cos(lambda)
        val v = cosBeta * sin(lambda)
        val w = sin(beta)
        val obliquity = Math.toRadians(OBLIQUITY_DEGREES)
        val y = cos(obliquity) * v - sin(obliquity) * w
        val z = sin(obliquity) * v + cos(obliquity) * w
        val rho = sqrt(1.0 - z * z)

        val dec = atan2(z, rho)
        var ra = (HOURS_PER_DAY / TWO_PI) * atan2(y, x)
        if (ra < 0) ra += HOURS_PER_DAY
        return ra to dec
    }

    /** local mean sidereal time, in hours */
    private fun localSiderealTime(mjd: Double, longitude: Double): Double {
        val mjd0 = floor(mjd)
        val ut = (mjd - mjd0) * HOURS_PER_DAY
        val t = (mjd0 - MJD_J2000) / DAYS_PER_CENTURY
        val gmst = 6.697374558 + 1.0027379093 * ut +
            (8640184.812866 + (0.093104 - 6.2e-6 * t) * t) * t / SECONDS_PER_HOUR_D
        return ((gmst + longitude / DEGREES_PER_HOUR) % HOURS_PER_DAY + HOURS_PER_DAY) %
            HOURS_PER_DAY
    }

    private fun frac(x: Double): Double = x - floor(x)

    /** 2000-01-06 18:14 UTC, a well-documented new moon */
    private const val REFERENCE_NEW_MOON = 947_182_440_000L
    private const val SYNODIC_MONTH = 29.530588853
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val MILLIS_PER_SECOND = 1_000.0
    /** half a day either side of each of the four instants */
    private const val QUARTER_WINDOW = 0.5 / SYNODIC_MONTH
    private const val NEW_MOON = 1
    private const val WAXING_CRESCENT = 2
    private const val FIRST_QUARTER = 3
    private const val WAXING_GIBBOUS = 4
    private const val FULL_MOON = 5
    private const val WANING_GIBBOUS = 6
    private const val LAST_QUARTER = 7
    private const val WANING_CRESCENT = 8
    private const val TWO_PI = 2 * PI
    private const val HOURS_PER_DAY = 24.0
    private const val DEGREES_PER_HOUR = 15.0
    private const val SECONDS_PER_HOUR_D = 3600.0
    private const val DAYS_PER_CENTURY = 36525.0
    /** MJD of 1970-01-01, and of J2000.0 */
    private const val MJD_AT_EPOCH = 40587.0
    private const val MJD_J2000 = 51544.5
    private const val ARCSECONDS_PER_RADIAN = 206264.8062
    private const val ARCSECONDS_PER_REVOLUTION = 1_296_000.0
    private const val OBLIQUITY_DEGREES = 23.43929111
    /** sin(+0.125°) — parallax less refraction less semidiameter */
    private const val SIN_H0 = 0.002181488
}
