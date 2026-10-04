package dev.local.weatherstudy.widget.complication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.complication.receiver.AbsComplicationWeatherReceiver
 * + its 8 subclasses
 *
 * ### Eight receivers, one per complication kind
 *
 * Recovered from the decoded manifest, each paired with an
 * `xml/complication_*_widget_info.xml`:
 *
 * ```
 * ComplicationSimpleWeatherReceiver     complication_simple_widget_info.xml
 * ComplicationWeatherDetailReceiver     complication_detail_widget_info.xml
 * ComplicationAirQualityReceiver        complication_aqi_widget_info.xml
 * ComplicationFineDustReceiver          complication_finedust_widget_info.xml
 * ComplicationUVReceiver                complication_uv_widget_info.xml
 * ComplicationPrecipitationReceiver     complication_precip_widget_info.xml
 * ComplicationSunriseSunsetReceiver     complication_sunrise_sunset_widget_info.xml
 * ComplicationMoonReceiver              complication_moonphase_widget_info.xml
 * ```
 *
 * Same reason as the widgets: the info XML binds to a receiver class, so a distinct
 * complication needs a distinct receiver. The abstract base holds the data lookup, and
 * each subclass declares only which measurement it publishes — which is exactly the
 * shape reconstructed here.
 *
 * They read `StudyBriefWeather` through `StudyComplicationRemoteViewModel`, never the
 * full aggregate.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
abstract class StudyAbsComplicationWeatherReceiver : BroadcastReceiver() {

    /** which measurement this complication publishes — the subclass's whole contribution */
    protected abstract val complicationType: StudyComplicationType

    override fun onReceive(context: Context, intent: Intent) {
        // the original resolves the favourite location's brief weather and publishes
        // a value for `complicationType`; the reconstruction keeps the seam
    }
}

/** The eight complication kinds. */
enum class StudyComplicationType {
    SIMPLE_WEATHER,
    WEATHER_DETAIL,
    AIR_QUALITY,
    FINE_DUST,
    UV,
    PRECIPITATION,
    SUNRISE_SUNSET,
    MOON,
}

@AndroidEntryPoint
class StudyComplicationSimpleWeatherReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.SIMPLE_WEATHER
}

@AndroidEntryPoint
class StudyComplicationWeatherDetailReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.WEATHER_DETAIL
}

@AndroidEntryPoint
class StudyComplicationAirQualityReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.AIR_QUALITY
}

@AndroidEntryPoint
class StudyComplicationFineDustReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.FINE_DUST
}

@AndroidEntryPoint
class StudyComplicationUVReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.UV
}

@AndroidEntryPoint
class StudyComplicationPrecipitationReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.PRECIPITATION
}

@AndroidEntryPoint
class StudyComplicationSunriseSunsetReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.SUNRISE_SUNSET
}

@AndroidEntryPoint
class StudyComplicationMoonReceiver : StudyAbsComplicationWeatherReceiver() {
    override val complicationType = StudyComplicationType.MOON
}
