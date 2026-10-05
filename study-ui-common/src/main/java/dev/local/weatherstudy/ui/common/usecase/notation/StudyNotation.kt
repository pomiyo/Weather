package dev.local.weatherstudy.ui.common.usecase.notation

import dev.local.weatherstudy.domain.entity.weather.StudyCondition
import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.domain.type.StudySettingValue
import dev.local.weatherstudy.system.service.StudyLocaleService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 27 classes in
 * com.samsung.android.weather.ui.common.usecase.notation
 *
 * ### Why formatting is a use-case layer and not a View concern
 *
 * By the time a `ViewHolder` runs, every string is already final. The notation layer is
 * what makes that true, and it is 27 classes because each notation has a rule that is
 * not obvious:
 *
 * - **temperature** rounds with the degree sign attached and respects the user's scale,
 *   which lives in settings and is observed — so a scale change re-renders everything
 * - **the clock** respects [StudyLocaleService.isAmPmBeforeTime], because Korean,
 *   Japanese and Chinese put the meridiem *before* the time
 * - **wind direction** maps a degree to one of 16 compass labels
 * - **index levels** map a banded integer to localised text, per measurement family
 *
 * Reconstructed as one file with a class per notation, keeping the per-notation split.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyTemperatureNotation @Inject constructor() {

    /** metric is canonical in the domain; conversion happens only here */
    fun format(celsius: Double, scale: Int): String {
        if (celsius == StudyCondition.INVALID_TEMPERATURE) return INVALID_TEXT
        val value = if (scale == SCALE_FAHRENHEIT) toFahrenheit(celsius) else celsius
        return "${value.roundToInt()}$DEGREE"
    }

    /** without the degree sign — the widgets and complications use this */
    fun formatBare(celsius: Double, scale: Int): String {
        if (celsius == StudyCondition.INVALID_TEMPERATURE) return INVALID_TEXT
        val value = if (scale == SCALE_FAHRENHEIT) toFahrenheit(celsius) else celsius
        return value.roundToInt().toString()
    }

    /**
     * The header's high/low line.
     *
     * `PagerViewHolder` builds it as a bare string concatenation with the two arrow
     * glyphs inline - `"↑" + maxTemp + " / ↓" + minTemp` - not from a string resource and
     * not from a drawable, which is why the glyphs survive every locale. They were missing
     * here, so the header read `28° / 22°` where the original reads `↑28° / ↓22°`.
     */
    fun formatHighLow(highC: Double, lowC: Double, scale: Int): String =
        "$HIGH_GLYPH${format(highC, scale)} / $LOW_GLYPH${format(lowC, scale)}"

    private fun toFahrenheit(celsius: Double) = celsius * 9.0 / 5.0 + FREEZING_F

    companion object {
        const val SCALE_CELSIUS = 0
        const val SCALE_FAHRENHEIT = 1
        const val DEGREE = "°"
        /** the two glyphs `PagerViewHolder` concatenates into the high/low line */
        const val HIGH_GLYPH = "↑"
        const val LOW_GLYPH = "↓"
        const val INVALID_TEXT = "--"
        private const val FREEZING_F = 32.0
    }
}

/**
 * Corresponds conceptually to the clock notations in `…usecase.notation`.
 *
 * Observed responsibility: the meridiem position is locale-dependent, which is what
 * `LocaleService.isAmPmBeforeTime()` exists for. Getting this wrong is invisible in
 * English and immediately wrong in Korean.
 */
class StudyTimeNotation @Inject constructor(
    private val localeService: StudyLocaleService,
) {
    /** `timeZoneId` is the LOCATION's zone; empty means the device's */
    fun formatHour(epochMillis: Long, use24Hour: Boolean, timeZoneId: String = ""): String {
        if (epochMillis <= 0L) return StudyTemperatureNotation.INVALID_TEXT
        val pattern = when {
            use24Hour -> PATTERN_24
            localeService.isAmPmBeforeTime() -> PATTERN_12_MERIDIEM_FIRST
            else -> PATTERN_12
        }
        return format(pattern, epochMillis, timeZoneId)
    }

    /** hour and minute — sunrise, sunset, the last-updated stamp */
    fun formatClock(epochMillis: Long, timeZoneId: String = ""): String =
        if (epochMillis <= 0L) StudyTemperatureNotation.INVALID_TEXT
        else format(PATTERN_CLOCK, epochMillis, timeZoneId)

    fun formatDayOfWeek(epochMillis: Long, timeZoneId: String = ""): String =
        format(PATTERN_DAY, epochMillis, timeZoneId)

    fun formatDate(epochMillis: Long, timeZoneId: String = ""): String =
        format(PATTERN_DATE, epochMillis, timeZoneId)

    fun formatRelativeUpdate(updateTimeMillis: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(now - updateTimeMillis)
        return when {
            updateTimeMillis <= 0L -> ""
            minutes < 1L -> "just now"
            minutes < MINUTES_PER_HOUR -> "$minutes min ago"
            else -> "${TimeUnit.MILLISECONDS.toHours(now - updateTimeMillis)} hr ago"
        }
    }

    private fun format(pattern: String, epochMillis: Long, timeZoneId: String): String =
        java.text.SimpleDateFormat(pattern, localeService.getLocale())
            .apply { if (timeZoneId.isNotEmpty()) timeZone = java.util.TimeZone.getTimeZone(timeZoneId) }
            .format(java.util.Date(epochMillis))

    private companion object {
        const val PATTERN_24 = "HH:mm"
        const val PATTERN_12 = "h a"
        const val PATTERN_12_MERIDIEM_FIRST = "a h"
        const val PATTERN_CLOCK = "h:mm a"
        const val PATTERN_DAY = "EEE"
        const val PATTERN_DATE = "MMM d"
        const val MINUTES_PER_HOUR = 60L
    }
}

/** Corresponds conceptually to the wind notations. */
class StudyWindNotation @Inject constructor() {

    /**
     * The speed WITHOUT its unit, and [unitLabel] beside it.
     *
     * The wind tile needs the two separately: `detail_index_wind_inner_view_holder` has
     * `wind_speed_value` and `wind_speed_unit` as two views stacked inside the compass,
     * and `WindIndexInnerViewHolder` binds `windState.speed` and `windState.unit` to them.
     * Joining them into one line is what pushed the text across the dial.
     */
    fun formatSpeedValue(speedKph: Double, unit: Int): String {
        if (speedKph < 0) return StudyTemperatureNotation.INVALID_TEXT
        val value = when (unit) {
            UNIT_MPH -> speedKph * MPH_PER_KPH
            UNIT_MS -> speedKph * MS_PER_KPH
            else -> speedKph
        }
        return value.roundToInt().toString()
    }

    fun speedUnitLabel(unit: Int): String = unitLabel(unit)

    fun formatSpeed(speedKph: Double, unit: Int): String {
        if (speedKph < 0) return StudyTemperatureNotation.INVALID_TEXT
        val value = when (unit) {
            UNIT_MPH -> speedKph * MPH_PER_KPH
            UNIT_MS -> speedKph * MS_PER_KPH
            else -> speedKph
        }
        return "${value.roundToInt()} ${unitLabel(unit)}"
    }

    /** degree → one of 16 compass labels; `WindGraph` renders the arrow from the degree */
    fun formatDirection(degree: Float): String {
        val compass = StudyIndexLevel.WindDirection.COMPASS
        val index = ((degree % FULL_CIRCLE) / (FULL_CIRCLE / compass.size)).toInt() % compass.size
        return compass[index]
    }

    /** the inverse of [formatDirection]: a 16-point name back to the angle the compass draws */
    fun toDegree(compassPoint: String): Float {
        val compass = StudyIndexLevel.WindDirection.COMPASS
        val index = compass.indexOf(compassPoint.uppercase())
        return if (index < 0) 0f else index * (FULL_CIRCLE / compass.size)
    }

    private fun unitLabel(unit: Int) = when (unit) {
        UNIT_MPH -> "mph"
        UNIT_MS -> "m/s"
        else -> "km/h"
    }

    companion object {
        const val UNIT_KPH = 0
        const val UNIT_MPH = 1
        const val UNIT_MS = 2
        private const val MPH_PER_KPH = 0.621371
        private const val MS_PER_KPH = 0.277778
        private const val FULL_CIRCLE = 360f
    }
}

/** Corresponds conceptually to the pressure / humidity / visibility / dew-point notations. */
class StudyIndexNotation @Inject constructor() {

    fun formatPercent(value: Double): String =
        if (value < 0) StudyTemperatureNotation.INVALID_TEXT else "${value.roundToInt()}%"

    /** `pressure_value` and `pressure_unit`, the two stacked views inside the dial */
    fun formatPressureValue(hPa: Double, unit: Int): String {
        if (hPa <= 0) return StudyTemperatureNotation.INVALID_TEXT
        val value = if (unit == PRESSURE_INHG) hPa * INHG_PER_HPA else hPa
        return if (unit == PRESSURE_INHG) "%.2f".format(value) else value.roundToInt().toString()
    }

    fun pressureUnitLabel(unit: Int): String = if (unit == PRESSURE_INHG) "inHg" else "hPa"

    fun formatPressure(hPa: Double, unit: Int): String {
        if (hPa <= 0) return StudyTemperatureNotation.INVALID_TEXT
        val value = if (unit == PRESSURE_INHG) hPa * INHG_PER_HPA else hPa
        return if (unit == PRESSURE_INHG) {
            "${"%.2f".format(value)} inHg"
        } else {
            "${value.roundToInt()} hPa"
        }
    }

    fun formatDistance(km: Double, unit: Int): String {
        if (km < 0) return StudyTemperatureNotation.INVALID_TEXT
        val value = if (unit == DISTANCE_MILES) km * MILES_PER_KM else km
        return if (unit == DISTANCE_MILES) {
            "${value.roundToInt()} mi"
        } else {
            "${value.roundToInt()} km"
        }
    }

    fun formatUvIndex(value: Double): String =
        if (value < 0) StudyTemperatureNotation.INVALID_TEXT else value.roundToInt().toString()

    fun formatPrecipitationAmount(mm: Double, unit: Int): String {
        if (mm < 0) return StudyTemperatureNotation.INVALID_TEXT
        return if (unit == PRECIP_INCHES) {
            "${"%.2f".format(mm * INCHES_PER_MM)} in"
        } else {
            "${"%.1f".format(mm)} mm"
        }
    }

    companion object {
        const val PRESSURE_HPA = 0
        const val PRESSURE_INHG = 1
        const val DISTANCE_KM = 0
        const val DISTANCE_MILES = 1
        const val PRECIP_MM = 0
        const val PRECIP_INCHES = 1
        private const val INHG_PER_HPA = 0.02953
        private const val MILES_PER_KM = 0.621371
        private const val INCHES_PER_MM = 0.0393701
    }
}

/**
 * Corresponds conceptually to the level-text notations.
 *
 * Observed responsibility: a banded integer becomes localised text. The original reads
 * the strings from resources via the regional `TextProvider` family; the reconstruction
 * returns neutral English, because the original's string resources are not reproduced.
 */
class StudyLevelNotation @Inject constructor() {

    fun formatUvLevel(level: Int): String = when (level) {
        StudyIndexLevel.Uv.VERY_LOW -> "Very low"
        StudyIndexLevel.Uv.LOW -> "Low"
        StudyIndexLevel.Uv.NORMAL -> "Moderate"
        StudyIndexLevel.Uv.HIGH -> "High"
        StudyIndexLevel.Uv.VERY_HIGH -> "Very high"
        StudyIndexLevel.Uv.EXTREME -> "Extreme"
        else -> ""
    }

    fun formatAirLevel(level: Int): String = when (level) {
        StudyIndexLevel.Air.GOOD -> "Good"
        StudyIndexLevel.Air.NORMAL -> "Moderate"
        StudyIndexLevel.Air.UNHEALTHY_FOR_SENSITIVE -> "Unhealthy for sensitive groups"
        StudyIndexLevel.Air.UNHEALTHY -> "Unhealthy"
        StudyIndexLevel.Air.VERY_UNHEALTHY -> "Very unhealthy"
        StudyIndexLevel.Air.HAZARDOUS -> "Hazardous"
        else -> ""
    }

    fun formatLifeIndexLevel(level: Int): String = when (level) {
        StudyIndexLevel.LifeIndex.VERY_BAD -> "Very poor"
        StudyIndexLevel.LifeIndex.BAD -> "Poor"
        StudyIndexLevel.LifeIndex.NOT_GOOD -> "Fair"
        StudyIndexLevel.LifeIndex.NORMAL -> "Moderate"
        StudyIndexLevel.LifeIndex.GOOD -> "Good"
        StudyIndexLevel.LifeIndex.VERY_GOOD -> "Excellent"
        else -> ""
    }

    fun formatMoonPhase(phase: Int): String = when (phase) {
        StudyIndexLevel.MoonPhase.NEW_MOON -> "New moon"
        StudyIndexLevel.MoonPhase.WAXING_CRESCENT -> "Waxing crescent"
        StudyIndexLevel.MoonPhase.FIRST_QUARTER -> "First quarter"
        StudyIndexLevel.MoonPhase.WAXING_GIBBOUS -> "Waxing gibbous"
        StudyIndexLevel.MoonPhase.FULL_MOON -> "Full moon"
        StudyIndexLevel.MoonPhase.WANING_GIBBOUS -> "Waning gibbous"
        StudyIndexLevel.MoonPhase.LAST_QUARTER -> "Last quarter"
        StudyIndexLevel.MoonPhase.WANING_CRESCENT -> "Waning crescent"
        else -> ""
    }

    fun formatPressureTendency(tendency: Int): String = when (tendency) {
        StudyIndexLevel.Pressure.RISING -> "Rising"
        StudyIndexLevel.Pressure.FALLING -> "Falling"
        StudyIndexLevel.Pressure.STEADY -> "Steady"
        else -> ""
    }
}

/**
 * Corresponds conceptually to `…usecase.GetSimpleSunriseGraphRotationDegree`.
 *
 * Observed responsibility: the sun's position on `SunCurvedPathView`'s arc, as a
 * fraction. It is a use case rather than view code because polar day and polar night
 * have no sunrise or sunset — see `StudyForecastTime.arcticNightType`.
 */
class StudySunProgressNotation @Inject constructor() {
    fun progress(sunriseMillis: Long, sunsetMillis: Long, nowMillis: Long): Float = when {
        sunriseMillis <= 0L || sunsetMillis <= 0L -> 0f
        nowMillis <= sunriseMillis -> 0f
        nowMillis >= sunsetMillis -> 1f
        else -> (nowMillis - sunriseMillis).toFloat() / (sunsetMillis - sunriseMillis).toFloat()
    }
}

/** Corresponds conceptually to the auto-refresh interval notation in settings. */
class StudyAutoRefreshNotation @Inject constructor() {
    fun format(interval: Int): String = when (interval) {
        StudySettingValue.AutoRefreshInterval.NONE -> "Manual only"
        StudySettingValue.AutoRefreshInterval.EVERY_HOUR -> "Every hour"
        StudySettingValue.AutoRefreshInterval.EVERY_3HOUR -> "Every 3 hours"
        StudySettingValue.AutoRefreshInterval.EVERY_6HOUR -> "Every 6 hours"
        StudySettingValue.AutoRefreshInterval.EVERY_12HOUR -> "Every 12 hours"
        StudySettingValue.AutoRefreshInterval.EVERY_24HOUR -> "Every 24 hours"
        else -> ""
    }
}
