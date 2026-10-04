package dev.local.weatherstudy.widget.appwidget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.widget.home.StudyHomeClockAppWidget
import dev.local.weatherstudy.widget.home.StudyHomeWeatherAppWidget
import dev.local.weatherstudy.widget.home.StudyWidgetFrame

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 12 receivers in
 * com.sec.android.daemonapp.appwidget, over `AbsHomeAppWidgetProvider` and
 * `CoverGlanceAppWidgetProvider`
 *
 * ### Twelve receivers, one dataset
 *
 * The original declares a separate `<receiver>` per widget variant, because
 * AppWidgetManager binds a provider-info XML to a receiver class — size options,
 * resizability, preview and configure activity are all per-receiver. Recovered from the
 * decoded manifest:
 *
 * ```
 * WeatherAppWidget             WeatherAppWidget2x1        WeatherForecastAppWidget
 * WeatherForecastWidget        WeatherAestheticAppWidget  WeatherInsightAppWidget
 * WeatherNewsAppWidget*        WeatherCoverAppWidget      WeatherCoverFaceWidget
 * WeatherCoverFaceDetailWidget WeatherCoverMultiWidgetHourly
 * WeatherCoverMultiWidgetSimple
 *                                        * runtime-disabled in the original too
 * ```
 *
 * The phone ones are reconstructed; the five cover-display ones are stubbed, because the
 * cover display is Samsung hardware. See `reports/samsung-bridge-map.md`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyWeatherAppWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyHomeWeatherAppWidget()
}

/** `…appwidget.WeatherAppWidget2x1` — the compact variant. */
@AndroidEntryPoint
class StudyWeatherAppWidget2x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyHomeWeatherAppWidget(StudyWidgetFrame.WIDE_SMALL)
}

/** `…appwidget.WeatherForecastAppWidget` — adds the daily list. */
@AndroidEntryPoint
class StudyWeatherForecastAppWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyHomeWeatherAppWidget(StudyWidgetFrame.LARGE)
}

/** `…appwidget.WeatherClockAppWidget` (from `weather_clock_provider.xml`). */
@AndroidEntryPoint
class StudyWeatherClockAppWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyHomeClockAppWidget()
}
