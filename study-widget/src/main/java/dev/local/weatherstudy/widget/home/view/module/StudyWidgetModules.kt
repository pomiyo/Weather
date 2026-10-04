package dev.local.weatherstudy.widget.home.view.module

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.compose.ui.unit.dp
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import dev.local.weatherstudy.widget.home.view.component.StudyWeatherIcon
import dev.local.weatherstudy.widget.home.view.component.StudyWeatherText
import dev.local.weatherstudy.widget.home.view.item.StudyDailyListItemData
import dev.local.weatherstudy.widget.home.view.item.StudyHourlyListItemData
import dev.local.weatherstudy.widget.home.view.item.StudyIndexItemData
import dev.local.weatherstudy.widget.home.view.item.StudyWeatherTemplateData

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the eight Kt files in
 * com.sec.android.daemonapp.home.view.module:
 * `WeatherDegreeModuleKt`, `HourlyListModuleKt`, `DailyListModuleKt`,
 * `ExtraInfoModuleKt`, `ClockModuleKt`, `ClockMediumWeatherModuleKt`,
 * `RefreshModuleKt`, `TemplateModuleKt`, + `ClockSize`, `ExtraInfoAlignment`,
 * `RefreshAlignment`
 *
 * Observed responsibility: the **reorderable blocks** a widget is made of. These are the
 * same blocks `StudyWidgetComponent.type` names in the database — the widget editor lets
 * the user reorder them, and the order is persisted in `TABLE_WIDGET_COMPONENT_INFO`.
 * So the module list here and the component types there are two halves of one feature.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Composable
fun StudyWeatherDegreeModule(data: StudyWeatherTemplateData) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        StudyWeatherIcon(data.iconNum, sizeDp = DEGREE_ICON_DP)
        Spacer(modifier = GlanceModifier.width(MODULE_GAP_DP.dp))
        StudyWeatherText(data.temperatureText, sizeSp = DEGREE_TEXT_SP)
        Spacer(modifier = GlanceModifier.width(MODULE_GAP_DP.dp))
        Column {
            StudyWeatherText(data.conditionText)
            StudyWeatherText(data.highLowText)
        }
    }
}

/** `HourlyListModuleKt`. */
@Composable
fun StudyHourlyListModule(items: List<StudyHourlyListItemData>) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        items.take(HOURLY_SLOTS).forEach { item ->
            // equal weights: the slots share the row however wide the widget is
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                StudyWeatherText(item.timeText, sizeSp = SMALL_TEXT_SP)
                StudyWeatherIcon(item.iconNum)
                StudyWeatherText(item.temperatureText)
            }
        }
    }
}

/** `DailyListModuleKt`. */
@Composable
fun StudyDailyListModule(items: List<StudyDailyListItemData>) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        items.take(DAILY_SLOTS).forEach { item ->
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                StudyWeatherText(item.dayText, modifier = GlanceModifier.defaultWeight())
                StudyWeatherIcon(item.iconNum)
                Spacer(modifier = GlanceModifier.width(MODULE_GAP_DP.dp))
                StudyWeatherText("${item.highText} / ${item.lowText}")
            }
        }
    }
}

/** `ExtraInfoModuleKt` + `ExtraInfoAlignment`. */
@Composable
fun StudyExtraInfoModule(items: List<StudyIndexItemData>) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        items.take(EXTRA_INFO_SLOTS).forEach { item ->
            Column(modifier = GlanceModifier.defaultWeight()) {
                StudyWeatherText(item.titleText, sizeSp = SMALL_TEXT_SP)
                StudyWeatherText(item.valueText)
            }
        }
    }
}

/** `ClockModuleKt` + `ClockSize`. */
@Composable
fun StudyClockModule() {
    StudyWeatherText("--:--", sizeSp = CLOCK_TEXT_SP)
}

/**
 * `RefreshModuleKt` + `RefreshAlignment`.
 *
 * The refresh affordance. The original animates its spinner via
 * `RemoteViews.semSetAnimation` — a Samsung extension with no AOSP equivalent, so on a
 * non-Samsung build the spinner is a static frame. See `StudyRemoteViewsService`.
 */
@Composable
fun StudyRefreshModule(updateTimeText: String) {
    Row(modifier = GlanceModifier.fillMaxWidth()) { StudyWeatherText(updateTimeText) }
}

/** `TemplateModuleKt` — assembles a frame from a component order. */
@Composable
fun StudyTemplateModule(data: StudyWeatherTemplateData, componentTypes: List<Int>) {
    val types = dev.local.weatherstudy.domain.entity.widget.StudyWidgetComponent
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        componentTypes.forEach { type ->
            when (type) {
                types.TYPE_CLOCK -> StudyClockModule()
                types.TYPE_CURRENT -> StudyWeatherDegreeModule(data)
                types.TYPE_HOURLY -> StudyHourlyListModule(data.hourly)
                types.TYPE_DAILY -> StudyDailyListModule(data.daily)
                types.TYPE_INDEX -> StudyExtraInfoModule(data.indices)
                types.TYPE_REFRESH -> StudyRefreshModule(data.updateTimeText)
                else -> Unit
            }
        }
    }
}

private const val DEGREE_TEXT_SP = 36
private const val CLOCK_TEXT_SP = 40
private const val HOURLY_SLOTS = 5
private const val DAILY_SLOTS = 4
private const val DEGREE_ICON_DP = 40
private const val MODULE_GAP_DP = 8
private const val SMALL_TEXT_SP = 12
private const val EXTRA_INFO_SLOTS = 3
