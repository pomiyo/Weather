package dev.local.weatherstudy.ui.common.resource

import dev.local.weatherstudy.domain.type.StudyInsightType
import dev.local.weatherstudy.domain.usecase.StudyAssignIconNum
import dev.local.weatherstudy.ui.common.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.ui.common.resource.WeatherIconProvider's implementation
 * (and `MoonPhaseImageProvider`, `BackgroundProvider` beside it)
 *
 * Observed responsibility: the one place an icon NUMBER becomes a drawable. Every
 * surface — detail cards, the locations list, the widgets — asks here, which is why the
 * persisted value is the number and never a resource id.
 *
 * The original resolves to 4,909 bundled drawables and Lottie files. The reconstruction
 * ships ten original vector drawings, one per value `StudyAssignIconNum` can produce.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyWeatherIconProviderImpl @Inject constructor() : StudyWeatherIconProvider {
    override fun getIconRes(iconNum: Int, isDay: Boolean): Int = StudyWeatherIcons.iconRes(iconNum)
    override fun getSmallIconRes(iconNum: Int, isDay: Boolean): Int = StudyWeatherIcons.iconRes(iconNum)
}

/**
 * The same mapping as a plain object, for the view holders: they are constructed by a
 * factory rather than injected, exactly as the original's are.
 */
object StudyWeatherIcons {

    /**
     * day or night is already folded into the number by `StudyAssignIconNum`
     *
     * Session 3: the vocabulary is now the original's full 0..29, so thirty codes fold onto
     * the ten static vectors this project draws itself. The fold is lossy on purpose - a
     * placeholder set of ten is honest, whereas inventing thirty would imply a fidelity the
     * drawings do not have. The animated originals (60 AnimatedVectorDrawables, 60 Lottie)
     * are catalogued in reports/weather-icon-map.md and are the replacement for these.
     */
    fun iconRes(iconNum: Int): Int = when (iconNum) {
        StudyAssignIconNum.ICON_SUNNY,
        StudyAssignIconNum.ICON_MOSTLY_SUNNY,
        -> R.drawable.study_ic_weather_clear_day

        StudyAssignIconNum.ICON_CLEAR,
        StudyAssignIconNum.ICON_MOSTLY_CLEAR,
        -> R.drawable.study_ic_weather_clear_night

        StudyAssignIconNum.ICON_PARTLY_CLOUD,
        StudyAssignIconNum.ICON_MOSTLY_CLOUDY,
        -> R.drawable.study_ic_weather_partly_day

        StudyAssignIconNum.ICON_PARTLY_CLOUD_NIGHT,
        StudyAssignIconNum.ICON_MOSTLY_CLOUDY_NIGHT,
        -> R.drawable.study_ic_weather_partly_night

        StudyAssignIconNum.ICON_CLOUDY,
        StudyAssignIconNum.ICON_FOG,
        StudyAssignIconNum.ICON_WIND,
        -> R.drawable.study_ic_weather_cloudy

        StudyAssignIconNum.ICON_RAIN,
        StudyAssignIconNum.ICON_SHOWER,
        StudyAssignIconNum.ICON_PARTLY_SUNNY_WITH_SHOWER,
        StudyAssignIconNum.ICON_HEAVY_RAIN,
        StudyAssignIconNum.ICON_RAIN_AND_SLEET,
        -> R.drawable.study_ic_weather_rain

        StudyAssignIconNum.ICON_LIGHT_SNOW,
        StudyAssignIconNum.ICON_PARTLY_SUNNY_WITH_FLURRIES,
        StudyAssignIconNum.ICON_SNOW,
        StudyAssignIconNum.ICON_RAIN_AND_SNOW,
        StudyAssignIconNum.ICON_HEAVY_SNOW,
        StudyAssignIconNum.ICON_ICE,
        StudyAssignIconNum.ICON_COLD,
        StudyAssignIconNum.ICON_HAIL,
        -> R.drawable.study_ic_weather_snow

        StudyAssignIconNum.ICON_THUNDERSTORM,
        StudyAssignIconNum.ICON_PARTLY_SUNNY_WITH_THUNDER,
        StudyAssignIconNum.ICON_RAIN_AND_THUNDER,
        -> R.drawable.study_ic_weather_thunderstorm

        StudyAssignIconNum.ICON_SAND_STORM,
        StudyAssignIconNum.ICON_HURRICANE,
        -> R.drawable.study_ic_weather_sandstorm

        StudyAssignIconNum.ICON_HOT -> R.drawable.study_ic_thermostat

        else -> R.drawable.study_ic_weather_unknown
    }

    fun insightIconRes(insightType: Int): Int = when (insightType) {
        StudyInsightType.PRECIPITATION, StudyInsightType.SHORT_TERM_PRECIP -> R.drawable.study_ic_umbrella
        StudyInsightType.SNOW_FALL -> R.drawable.study_ic_weather_snow
        StudyInsightType.THUNDERSTORM_IMPACT -> R.drawable.study_ic_weather_thunderstorm
        StudyInsightType.SUNNYDAY, StudyInsightType.UV, StudyInsightType.UV_CONDITION ->
            R.drawable.study_ic_weather_clear_day
        StudyInsightType.FEELS_LIKE_TEMPERATURE,
        StudyInsightType.TEMPERATURE_CHANGE,
        StudyInsightType.RECORD_TEMPERATURE,
        -> R.drawable.study_ic_thermostat
        StudyInsightType.TOMORROW_COMMENT -> R.drawable.study_ic_weather_partly_day
        StudyInsightType.SUNRISE_SUNSET -> R.drawable.study_ic_sunrise
        else -> R.drawable.study_ic_info
    }
}

/**
 * Corresponds conceptually to `…ui.common.resource.BackgroundProvider`'s implementation.
 *
 * Observed responsibility: condition + day/night -> the gradient behind the detail
 * screen, and the name of the matching splash theme, so the splash Activity and the
 * screen it hands over to agree. The colour pairs here are the same ones the twelve
 * `study_splash_gradient_*` drawables declare.
 */
@Singleton
class StudyBackgroundProviderImpl @Inject constructor() : StudyBackgroundProvider {

    /**
     * The FALLBACK colour behind the detail screen.
     *
     * Session 3: this is no longer the background. `StudyBackgroundProvider` in :study-app
     * now resolves the original's painted 900x900 artwork, and this pair is only what shows
     * when that artwork is absent - a clone without the local study assets, or a surface
     * that cannot host a bitmap. The partition below therefore follows
     * BackgroundProvider.toDayBackground / toNightBackground exactly, so the fallback lands
     * on the same one of the eleven families the artwork would have.
     */
    override fun getGradientColors(iconNum: Int, isDay: Boolean): Pair<Int, Int> =
        if (isDay) dayGradient(iconNum) else nightGradient(iconNum)

    private fun dayGradient(iconNum: Int): Pair<Int, Int> = when (iconNum) {
        0, 2, 23, 25 -> SUNNY
        4, 5, 18 -> CLOUDY
        6, 7, 8, 20 -> RAIN
        9, 10, 19, 22 -> THUNDERSTORM
        11, 12, 13, 14, 15, 17, 27, 28, 29 -> COLD
        16 -> HOT
        21 -> SANDSTORM
        else -> SUNNY
    }

    private fun nightGradient(iconNum: Int): Pair<Int, Int> = when (iconNum) {
        1, 3, 11, 13, 14, 24, 26, 27, 28, 29 -> SUNNY_NIGHT
        4, 5, 6, 7, 18, 20 -> PARTLY_SUNNY_NIGHT
        9, 19, 22 -> THUNDERSTORM
        15, 17 -> COLD
        16 -> HOT
        21 -> SANDSTORM
        else -> SUNNY_NIGHT
    }

    /** the splash theme name, which must agree with the family chosen above */
    override fun getSplashThemeName(iconNum: Int, isDay: Boolean): String = when {
        !isDay && iconNum in setOf(4, 5, 6, 7, 18, 20) -> "PartlySunnyNight"
        !isDay && iconNum in setOf(9, 19, 22) -> "Thunderstorm"
        !isDay && iconNum in setOf(15, 17) -> "Cold"
        !isDay && iconNum == 16 -> "Hot"
        !isDay && iconNum == 21 -> "Sandstorm"
        !isDay -> "SunnyNight"
        iconNum in setOf(4, 5, 18) -> "Cloudy"
        iconNum in setOf(6, 7, 8, 20) -> "Rain"
        iconNum in setOf(9, 10, 19, 22) -> "Thunderstorm"
        iconNum in setOf(11, 12, 13, 14, 15, 17, 27, 28, 29) -> "Cold"
        iconNum == 16 -> "Hot"
        iconNum == 21 -> "Sandstorm"
        else -> "Sunny"
    }

    private companion object {
        val DEFAULT = 0xFF37474F.toInt() to 0xFF263238.toInt()
        val SUNNY = 0xFF3A7BC8.toInt() to 0xFF6FB1E6.toInt()
        val SUNNY_NIGHT = 0xFF1A237E.toInt() to 0xFF0D1333.toInt()
        val PARTLY_SUNNY_NIGHT = 0xFF283593.toInt() to 0xFF151B3B.toInt()
        val CLOUDY = 0xFF607D8B.toInt() to 0xFF37474F.toInt()
        val RAIN = 0xFF37474F.toInt() to 0xFF1C313A.toInt()
        val COLD = 0xFF3FA9DC.toInt() to 0xFF0288D1.toInt()
        val THUNDERSTORM = 0xFF263238.toInt() to 0xFF000A12.toInt()
        val SANDSTORM = 0xFF9E8A82.toInt() to 0xFF6D4C41.toInt()
        val HOT = 0xFFF68D7B.toInt() to 0xFFD2574A.toInt()
    }
}
