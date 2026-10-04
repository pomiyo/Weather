package dev.local.weatherstudy.widget.home.view.frame

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import dev.local.weatherstudy.widget.home.view.component.StudyColumnDivider
import dev.local.weatherstudy.widget.home.view.component.StudyWeatherText
import dev.local.weatherstudy.widget.home.view.item.StudyWeatherTemplateData
import dev.local.weatherstudy.widget.home.view.module.StudyClockModule
import dev.local.weatherstudy.widget.home.view.module.StudyDailyListModule
import dev.local.weatherstudy.widget.home.view.module.StudyExtraInfoModule
import dev.local.weatherstudy.widget.home.view.module.StudyHourlyListModule
import dev.local.weatherstudy.widget.home.view.module.StudyWeatherDegreeModule

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the seven Kt files in
 * com.sec.android.daemonapp.home.view.frame:
 * `WeatherBasicFrameKt`, `WidgetSmallFrameKt`, `WidgetMediumFrameKt`,
 * `WidgetMediumClockFrameKt`, `WidgetLargeExtendedFrameKt`, `WidgetWideSmallFrameKt`,
 * `WidgetSubWeatherFrameKt`, + `WidgetBasicPadding`
 *
 * Observed responsibility: **one frame per widget size**, each composing a different
 * subset of modules. A 2×1 widget gets the degree module only; a 4×2 adds hourly; the
 * large extended frame adds daily and the index row. That is why the original has six
 * frames rather than one responsive layout — RemoteViews cannot reflow, so the shape is
 * chosen up front from the reported span.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Composable
fun StudyWeatherBasicFrame(data: StudyWeatherTemplateData, content: @Composable () -> Unit) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        StudyWeatherText(data.cityName)
        content()
    }
}

/** `WidgetSmallFrameKt` — 2×1: the degree module only. */
@Composable
fun StudyWidgetSmallFrame(data: StudyWeatherTemplateData) {
    StudyWeatherBasicFrame(data) { StudyWeatherDegreeModule(data) }
}

/** `WidgetMediumFrameKt` — 4×2: degree + hourly. */
@Composable
fun StudyWidgetMediumFrame(data: StudyWeatherTemplateData) {
    StudyWeatherBasicFrame(data) {
        StudyWeatherDegreeModule(data)
        StudyColumnDivider()
        StudyHourlyListModule(data.hourly)
    }
}

/** `WidgetMediumClockFrameKt` — the clock variant. */
@Composable
fun StudyWidgetMediumClockFrame(data: StudyWeatherTemplateData) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        StudyClockModule()
        StudyColumnDivider()
        StudyWeatherDegreeModule(data)
    }
}

/** `WidgetWideSmallFrameKt` — 4×1: a single row. */
@Composable
fun StudyWidgetWideSmallFrame(data: StudyWeatherTemplateData) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically,
    ) {
        dev.local.weatherstudy.widget.home.view.component.StudyWeatherIcon(data.iconNum, sizeDp = 28)
        StudyWeatherText(" " + data.temperatureText, sizeSp = 22)
        StudyWeatherText("  " + data.cityName)
    }
}

/** `WidgetLargeExtendedFrameKt` — 4×4: everything. */
@Composable
fun StudyWidgetLargeExtendedFrame(data: StudyWeatherTemplateData) {
    StudyWeatherBasicFrame(data) {
        StudyWeatherDegreeModule(data)
        StudyColumnDivider()
        StudyHourlyListModule(data.hourly)
        StudyColumnDivider()
        StudyDailyListModule(data.daily)
        StudyColumnDivider()
        StudyExtraInfoModule(data.indices)
    }
}

/** `WidgetSubWeatherFrameKt` — the secondary-city strip in a multi-city widget. */
@Composable
fun StudyWidgetSubWeatherFrame(data: StudyWeatherTemplateData) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        StudyWeatherText(data.cityName)
        StudyWeatherText(data.highLowText)
    }
}

/** `…frame.WidgetBasicPadding`. */
object StudyWidgetBasicPadding {
    const val HORIZONTAL_DP = 12
    const val VERTICAL_DP = 10
}
