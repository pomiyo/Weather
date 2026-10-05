package dev.local.weatherstudy.network.gateway

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import org.json.JSONArray
import org.json.JSONObject

/**
 * Educational reconstruction — a reconstruction-only component.
 *
 * Turns MET Norway's `locationforecast/2.0/complete` into the shape
 * [StudyGatewayForecastComposer] already consumes, so the second service costs one
 * adapter and nothing else. Every rule downstream — the icon vocabulary, the phrases, the
 * narratives, the index tiles, the insight cards, the spliced sun columns, the moon — runs
 * unchanged on both services, which is the point: switching the service changes the
 * NUMBERS and nothing about the app.
 *
 * ### The three things MET Norway does not send
 *
 * `locationforecast` is a pure forecast product. It has no sunrise or sunset (those are a
 * separate Sunrise 3.0 API), no visibility and no air quality. Sunrise and sunset are
 * computed here, by the same kind of arithmetic [StudyGatewayMoon] already does for the
 * moon; visibility and air quality are simply absent, and the detail screen drops a tile
 * whose index is missing — a provider supplying less is a case the original's card
 * policies were already written for.
 *
 * ### Its symbol codes are a vocabulary, like Samsung's icon numbers
 *
 * `symbol_code` is `name_variant`: `lightrainshowers_day`, `partlycloudy_night`,
 * `heavyrainandthunder`. The variant is the day/night suffix and carries no weather, so it
 * is split off; the name maps onto a WMO code and from there through the SAME
 * `iconCode(wmo)` table the Open-Meteo path uses. Mapping met.no → our icon vocabulary
 * directly would have been a second table to keep in step with the first.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
internal class StudyMetNoAdapter {

    /**
     * @param now epoch millis, used to decide which hour is "current"
     */
    fun toOpenMeteoShape(response: JSONObject, place: StudyGatewayPlace, now: Long): JSONObject {
        val series = response.optJSONObject("properties")?.optJSONArray("timeseries")
            ?: JSONArray()
        val entries = (0 until series.length()).mapNotNull { series.optJSONObject(it)?.let(::Entry) }
            .filter { it.epochSeconds > 0 }
        if (entries.isEmpty()) return JSONObject()

        val offsetSeconds = place.utcOffsetSeconds
        val sun = StudyGatewaySun

        // hourly: the part of the series that still carries a one-hour block
        val hourly = entries.filter { it.hasNextHour }.take(HOURLY_LIMIT)
        val currentIndex = hourly.indexOfLast { it.epochSeconds * MILLIS <= now }.coerceAtLeast(0)
        val current = hourly.getOrElse(currentIndex) { hourly.first() }

        // daily: group every entry by its LOCAL day, so a day boundary falls where the
        // reader's clock says it does rather than where UTC says it does
        val days = entries.groupBy { localDay(it.epochSeconds, offsetSeconds) }
            .toSortedMap()
            .entries
            .take(DAILY_LIMIT)

        val dailyTimes = JSONArray()
        val dailyCode = JSONArray()
        val dailyMax = JSONArray()
        val dailyMin = JSONArray()
        val dailySunrise = JSONArray()
        val dailySunset = JSONArray()
        val dailyChance = JSONArray()
        val dailySum = JSONArray()
        val dailyUv = JSONArray()

        days.forEach { (day, items) ->
            val dayStart = day * SECONDS_PER_DAY - offsetSeconds
            val temps = items.mapNotNull { it.temperature }
            // the symbol at local midday reads as "the day's weather" far better than the
            // first entry, which at 00:00 is always a clear night
            val midday = items.minByOrNull { abs(it.epochSeconds - (dayStart + HALF_DAY_SECONDS)) }
            val riseSet = sun.riseSet(dayStart * MILLIS, place.latitude, place.longitude)

            dailyTimes.put(dayStart)
            dailyCode.put(wmoOf(midday?.symbol ?: items.first().symbol))
            dailyMax.put(temps.maxOrNull() ?: JSONObject.NULL)
            dailyMin.put(temps.minOrNull() ?: JSONObject.NULL)
            dailySunrise.put(riseSet.first / MILLIS)
            dailySunset.put(riseSet.second / MILLIS)
            // null, not 0, when the service did not say.
            //
            // `narrative(code, isDay, chance)` reads 0 as "No precipitation expected" and
            // null as "say nothing about it", so a missing probability must stay missing -
            // otherwise a rain symbol arrives under the sentence "Rain. No precipitation
            // expected." MET Norway omits probability_of_precipitation outside the Nordic
            // nowcast area, which is most of the world.
            dailyChance.put(
                items.mapNotNull { it.probability }.maxOrNull()?.toInt() ?: JSONObject.NULL,
            )
            dailySum.put(items.sumOf { it.precipitation ?: 0.0 })
            dailyUv.put(items.mapNotNull { it.uvIndex }.maxOrNull() ?: 0.0)
        }

        val todayRiseSet = sun.riseSet(now, place.latitude, place.longitude)
        val isDayNow = now in todayRiseSet.first until todayRiseSet.second

        return JSONObject()
            .put(
                "current",
                JSONObject()
                    .put("time", current.epochSeconds)
                    .put("temperature_2m", current.temperature ?: JSONObject.NULL)
                    // MET Norway sends no apparent temperature; the reading stands in for
                    // it rather than a wind-chill formula this project would have invented
                    .put("apparent_temperature", current.temperature ?: JSONObject.NULL)
                    .put("is_day", if (isDayNow) 1 else 0)
                    .put("precipitation", current.precipitation ?: 0.0)
                    .put("weather_code", wmoOf(current.symbol))
                    .put("pressure_msl", current.pressure ?: JSONObject.NULL)
                    .put("wind_speed_10m", current.windKph ?: JSONObject.NULL)
                    .put("wind_direction_10m", current.windDirection ?: 0.0)
                    .put("relative_humidity_2m", current.humidity ?: 0.0)
                    .put("dew_point_2m", current.dewPoint ?: JSONObject.NULL)
                    .put("uv_index", current.uvIndex ?: 0.0),
            )
            .put(
                "hourly",
                JSONObject()
                    .put("time", JSONArray().apply { hourly.forEach { put(it.epochSeconds) } })
                    .put("temperature_2m", column(hourly) { it.temperature })
                    .put("weather_code", JSONArray().apply { hourly.forEach { put(wmoOf(it.symbol)) } })
                    .put("precipitation_probability", column(hourly) { it.probability })
                    .put("precipitation", column(hourly) { it.precipitation ?: 0.0 })
                    .put("wind_speed_10m", column(hourly) { it.windKph })
                    .put("wind_direction_10m", column(hourly) { it.windDirection })
                    .put("relative_humidity_2m", column(hourly) { it.humidity })
                    .put(
                        "is_day",
                        JSONArray().apply {
                            hourly.forEach {
                                val t = it.epochSeconds * MILLIS
                                val rs = sun.riseSet(t, place.latitude, place.longitude)
                                put(if (t in rs.first until rs.second) 1 else 0)
                            }
                        },
                    ),
            )
            .put(
                "daily",
                JSONObject()
                    .put("time", dailyTimes)
                    .put("weather_code", dailyCode)
                    .put("temperature_2m_max", dailyMax)
                    .put("temperature_2m_min", dailyMin)
                    .put("sunrise", dailySunrise)
                    .put("sunset", dailySunset)
                    .put("precipitation_probability_max", dailyChance)
                    .put("precipitation_sum", dailySum)
                    .put("uv_index_max", dailyUv),
            )
    }

    private fun column(items: List<Entry>, pick: (Entry) -> Double?): JSONArray =
        JSONArray().apply { items.forEach { put(pick(it) ?: JSONObject.NULL) } }

    private fun localDay(epochSeconds: Long, offsetSeconds: Int): Long =
        floor((epochSeconds + offsetSeconds).toDouble() / SECONDS_PER_DAY).toLong()

    /** one `timeseries` entry, flattened */
    private class Entry(node: JSONObject) {
        private val instant = node.optJSONObject("data")
            ?.optJSONObject("instant")?.optJSONObject("details")
        private val nextHour = node.optJSONObject("data")?.optJSONObject("next_1_hours")

        val epochSeconds: Long = parseIsoUtc(node.optString("time"))
        val hasNextHour: Boolean = nextHour != null
        val symbol: String = nextHour?.optJSONObject("summary")?.optString("symbol_code")
            ?: node.optJSONObject("data")?.optJSONObject("next_6_hours")
                ?.optJSONObject("summary")?.optString("symbol_code")
            ?: ""
        val temperature = instant?.optDoubleOrNull("air_temperature")
        val pressure = instant?.optDoubleOrNull("air_pressure_at_sea_level")
        val humidity = instant?.optDoubleOrNull("relative_humidity")
        val dewPoint = instant?.optDoubleOrNull("dew_point_temperature")
        val uvIndex = instant?.optDoubleOrNull("ultraviolet_index_clear_sky")
        val windDirection = instant?.optDoubleOrNull("wind_from_direction")
        /** MET Norway reports metres per second; everything downstream expects km/h */
        val windKph = instant?.optDoubleOrNull("wind_speed")?.times(KPH_PER_MS)
        val precipitation = nextHour?.optJSONObject("details")
            ?.optDoubleOrNull("precipitation_amount")
        val probability = nextHour?.optJSONObject("details")
            ?.optDoubleOrNull("probability_of_precipitation")
    }

    private companion object {
        const val MILLIS = 1000L
        const val SECONDS_PER_DAY = 86_400L
        const val HALF_DAY_SECONDS = 43_200L
        const val KPH_PER_MS = 3.6
        const val HOURLY_LIMIT = 48
        const val DAILY_LIMIT = 10

        fun JSONObject.optDoubleOrNull(name: String): Double? =
            if (has(name)) optDouble(name).takeUnless { it.isNaN() } else null

        /** `2026-10-05T09:00:00Z` — fixed width, fixed zone, so it is read rather than parsed */
        fun parseIsoUtc(text: String): Long {
            if (text.length < 20) return 0L
            return runCatching {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.clear()
                calendar.set(
                    text.substring(0, 4).toInt(),
                    text.substring(5, 7).toInt() - 1,
                    text.substring(8, 10).toInt(),
                    text.substring(11, 13).toInt(),
                    text.substring(14, 16).toInt(),
                    text.substring(17, 19).toInt(),
                )
                calendar.timeInMillis / MILLIS
            }.getOrDefault(0L)
        }

        /**
         * `symbol_code` → WMO, so both services share one icon path.
         *
         * The `_day` / `_night` suffix is dropped: it is a rendering hint, and the time of
         * day is already decided upstream by `is_day`. Keeping it would double-apply the
         * distinction — the same trap `IconProvider.dayIcons` sets, recorded in pass 3.
         */
        fun wmoOf(symbolCode: String): Int {
            val name = symbolCode.substringBefore('_')
            val thunder = name.contains("thunder")
            val base = name.removeSuffix("andthunder")
            return when {
                thunder && base.startsWith("heavy") -> 99
                thunder -> 95
                base == "clearsky" -> 0
                base == "fair" -> 1
                base == "partlycloudy" -> 2
                base == "cloudy" -> 3
                base == "fog" -> 45
                base == "lightrainshowers" || base == "lightrain" -> 61
                base == "rainshowers" || base == "rain" -> 63
                base == "heavyrainshowers" || base == "heavyrain" -> 65
                base == "lightsleet" || base == "lightsleetshowers" -> 66
                base == "sleet" || base == "sleetshowers" ||
                    base == "heavysleet" || base == "heavysleetshowers" -> 67
                base == "lightsnow" || base == "lightsnowshowers" -> 71
                base == "snow" || base == "snowshowers" -> 73
                base == "heavysnow" || base == "heavysnowshowers" -> 75
                else -> 3
            }
        }
    }
}

/**
 * Sunrise and sunset, computed.
 *
 * MET Norway publishes neither in `locationforecast`, and this is the standard sunrise
 * equation: solar mean anomaly, the equation of centre, ecliptic longitude, declination,
 * then the hour angle at which the sun's centre sits [SUN_ALTITUDE_DEGREES] below the
 * horizon — the −0.833° that accounts for refraction and the sun's own radius.
 *
 * It is the sun's counterpart to [StudyGatewayMoon.riseSet] and far simpler, because the
 * sun has no equivalent of the moon's 50-minutes-a-day drift: every day has exactly one
 * sunrise and one sunset outside the polar circles. Inside them the hour angle has no
 * solution, and the day is returned as a polar day or polar night rather than as an error.
 */
internal object StudyGatewaySun {

    /** epoch millis of sunrise and sunset for the day containing [epochMillis] */
    fun riseSet(epochMillis: Long, latitude: Double, longitude: Double): Pair<Long, Long> {
        val julianDay = epochMillis / MILLIS_PER_DAY + JULIAN_AT_EPOCH
        val n = floor(julianDay - J2000 + 0.0008 + 0.5)
        val meanSolarNoon = n - longitude / DEGREES_PER_TURN

        val anomaly = Math.toRadians((357.5291 + 0.98560028 * meanSolarNoon) % DEGREES_PER_TURN)
        val centre = 1.9148 * sin(anomaly) + 0.0200 * sin(2 * anomaly) + 0.0003 * sin(3 * anomaly)
        val eclipticLongitude = Math.toRadians(
            (Math.toDegrees(anomaly) + centre + 180.0 + 102.9372) % DEGREES_PER_TURN,
        )
        val transit = J2000 + meanSolarNoon + 0.0053 * sin(anomaly) - 0.0069 * sin(2 * eclipticLongitude)

        val declination = asin(sin(eclipticLongitude) * sin(Math.toRadians(OBLIQUITY_DEGREES)))
        val latitudeRad = Math.toRadians(latitude)
        val cosHourAngle = (sin(Math.toRadians(SUN_ALTITUDE_DEGREES)) -
            sin(latitudeRad) * sin(declination)) / (cos(latitudeRad) * cos(declination))

        // |cos| > 1 means the sun never reaches that altitude: polar day or polar night
        if (cosHourAngle > 1.0) return POLAR_NIGHT
        if (cosHourAngle < -1.0) return POLAR_DAY

        val hourAngle = Math.toDegrees(kotlin.math.acos(cosHourAngle))
        val rise = transit - hourAngle / DEGREES_PER_TURN
        val set = transit + hourAngle / DEGREES_PER_TURN
        return toMillis(rise) to toMillis(set)
    }

    private fun toMillis(julianDay: Double): Long =
        ((julianDay - JULIAN_AT_EPOCH) * MILLIS_PER_DAY).toLong()

    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val JULIAN_AT_EPOCH = 2440587.5
    private const val J2000 = 2451545.0
    private const val DEGREES_PER_TURN = 360.0
    private const val OBLIQUITY_DEGREES = 23.4397
    /** the sun's centre at rise and set: refraction plus its own semidiameter */
    private const val SUN_ALTITUDE_DEGREES = -0.833
    private val POLAR_NIGHT = 0L to 0L
    private val POLAR_DAY = 0L to 0L
}
