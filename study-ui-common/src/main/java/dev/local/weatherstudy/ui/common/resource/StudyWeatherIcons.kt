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

    /** day or night is already folded into the number by `StudyAssignIconNum` */
    fun iconRes(iconNum: Int): Int = when (iconNum) {
        StudyAssignIconNum.ICON_CLEAR_DAY -> R.drawable.study_ic_weather_clear_day
        StudyAssignIconNum.ICON_CLEAR_NIGHT -> R.drawable.study_ic_weather_clear_night
        StudyAssignIconNum.ICON_PARTLY_DAY -> R.drawable.study_ic_weather_partly_day
        StudyAssignIconNum.ICON_PARTLY_NIGHT -> R.drawable.study_ic_weather_partly_night
        StudyAssignIconNum.ICON_CLOUDY -> R.drawable.study_ic_weather_cloudy
        StudyAssignIconNum.ICON_RAIN -> R.drawable.study_ic_weather_rain
        StudyAssignIconNum.ICON_SNOW -> R.drawable.study_ic_weather_snow
        StudyAssignIconNum.ICON_THUNDERSTORM -> R.drawable.study_ic_weather_thunderstorm
        StudyAssignIconNum.ICON_SANDSTORM -> R.drawable.study_ic_weather_sandstorm
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

    override fun getGradientColors(iconNum: Int, isDay: Boolean): Pair<Int, Int> = when (iconNum) {
        StudyAssignIconNum.ICON_CLEAR_DAY -> SUNNY
        StudyAssignIconNum.ICON_CLEAR_NIGHT -> SUNNY_NIGHT
        StudyAssignIconNum.ICON_PARTLY_DAY -> SUNNY
        StudyAssignIconNum.ICON_PARTLY_NIGHT -> PARTLY_SUNNY_NIGHT
        StudyAssignIconNum.ICON_CLOUDY -> if (isDay) CLOUDY else PARTLY_SUNNY_NIGHT
        StudyAssignIconNum.ICON_RAIN -> RAIN
        StudyAssignIconNum.ICON_SNOW -> if (isDay) COLD else PARTLY_SUNNY_NIGHT
        StudyAssignIconNum.ICON_THUNDERSTORM -> THUNDERSTORM
        StudyAssignIconNum.ICON_SANDSTORM -> SANDSTORM
        else -> DEFAULT
    }

    override fun getSplashThemeName(iconNum: Int, isDay: Boolean): String = when (iconNum) {
        StudyAssignIconNum.ICON_CLEAR_DAY, StudyAssignIconNum.ICON_PARTLY_DAY -> "Sunny"
        StudyAssignIconNum.ICON_CLEAR_NIGHT -> "SunnyNight"
        StudyAssignIconNum.ICON_PARTLY_NIGHT -> "PartlySunnyNight"
        StudyAssignIconNum.ICON_CLOUDY -> "Cloudy"
        StudyAssignIconNum.ICON_RAIN -> "Rain"
        StudyAssignIconNum.ICON_SNOW -> "Cold"
        StudyAssignIconNum.ICON_THUNDERSTORM -> "Thunderstorm"
        StudyAssignIconNum.ICON_SANDSTORM -> "Sandstorm"
        else -> ""
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
    }
}
