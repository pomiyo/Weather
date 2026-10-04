package dev.local.weatherstudy.widget.home

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.local.weatherstudy.domain.entity.weather.StudyForecastTime
import dev.local.weatherstudy.domain.entity.weather.StudyIndex
import dev.local.weatherstudy.domain.entity.weather.displayName
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.type.StudyIndexCategory
import dev.local.weatherstudy.domain.type.StudyIndexType
import dev.local.weatherstudy.domain.usecase.StudyGetFavoriteLocation
import dev.local.weatherstudy.domain.usecase.StudyObserveWeatherChange
import dev.local.weatherstudy.ui.common.usecase.notation.StudyIndexNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTemperatureNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTimeNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyWindNotation
import dev.local.weatherstudy.widget.home.view.item.StudyDailyListItemData
import dev.local.weatherstudy.widget.home.view.item.StudyHourlyListItemData
import dev.local.weatherstudy.widget.home.view.item.StudyIndexItemData
import dev.local.weatherstudy.widget.home.view.item.StudyWeatherTemplateData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.widget.home.state.WidgetStateProvider
 *
 * Observed responsibility: the widget's counterpart of the detail screen's state
 * providers. It reads the same aggregate through the same use case and the same notation
 * classes, and produces the flat, pre-formatted [StudyWeatherTemplateData] the Glance
 * modules render — so a temperature reads identically on the widget and in the app.
 *
 * Returns null when nothing is saved yet, which the widget shows as its empty state.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyWidgetStateProvider @Inject constructor(
    private val getFavoriteLocation: StudyGetFavoriteLocation,
    private val settingsRepo: StudySettingsRepo,
    private val temperatureNotation: StudyTemperatureNotation,
    private val timeNotation: StudyTimeNotation,
    private val indexNotation: StudyIndexNotation,
    private val windNotation: StudyWindNotation,
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): StudyWeatherTemplateData? {
        val weather = getFavoriteLocation() ?: return null
        val tempScale = settingsRepo.getTempScale()
        val condition = weather.currentObservation.condition
        val time = weather.currentObservation.time
        val zone = time.ianaTimeZone

        fun index(type: Int): StudyIndex? = condition.indexList.firstOrNull {
            it.type == type && it.category == StudyIndexCategory.DETAIL && it.value != StudyIndex.INVALID_VALUE
        }

        return StudyWeatherTemplateData(
            cityName = weather.location.displayName,
            temperatureText = temperatureNotation.format(condition.temperature, tempScale),
            highLowText = temperatureNotation.formatHighLow(condition.maxTemp, condition.minTemp, tempScale),
            conditionText = condition.weatherText,
            iconNum = condition.iconNum,
            updateTimeText = timeNotation.formatClock(time.updateTime),
            isDay = time.isDayOrNight == StudyForecastTime.DAY,
            // the next hours, not the one already under way
            hourly = weather.hourlyObservations.drop(1).take(HOURLY_SLOTS).map { hour ->
                StudyHourlyListItemData(
                    timeText = timeNotation.formatHour(hour.time.epochTime, false, zone),
                    temperatureText = temperatureNotation.format(hour.condition.temperature, tempScale),
                    iconNum = hour.condition.iconNum,
                )
            },
            daily = weather.dailyObservations
                .filter { it.time.epochTime + DAY_MILLIS > now }
                .take(DAILY_SLOTS)
                .map { day ->
                    StudyDailyListItemData(
                        dayText = timeNotation.formatDayOfWeek(day.time.epochTime, zone),
                        highText = temperatureNotation.format(day.dayCondition.maxTemp, tempScale),
                        lowText = temperatureNotation.format(day.nightCondition.minTemp, tempScale),
                        iconNum = day.dayCondition.iconNum,
                    )
                },
            indices = listOfNotNull(
                index(StudyIndexType.HUMIDITY)?.let {
                    StudyIndexItemData("Humidity", indexNotation.formatPercent(it.value))
                },
                index(StudyIndexType.WIND)?.let {
                    StudyIndexItemData("Wind", windNotation.formatSpeed(it.value, StudyWindNotation.UNIT_KPH))
                },
                index(StudyIndexType.UV)?.let {
                    StudyIndexItemData("UV", indexNotation.formatUvIndex(it.value))
                },
            ),
        )
    }

    private companion object {
        const val HOURLY_SLOTS = 5
        const val DAILY_SLOTS = 4
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}

/**
 * Corresponds conceptually to `…widget.WidgetUpdater` / `WeatherWidgetUpdater`.
 *
 * Observed responsibility: push. Widgets declare `updatePeriodMillis="0"`, so nothing
 * redraws them on a timer; they are redrawn when the data they show has changed. This
 * watches the stored weather, the favourite location and the unit, and asks every placed
 * widget to re-run `provideGlance`.
 */
@Singleton
class StudyWidgetUpdater @Inject constructor(
    private val context: Context,
    private val observeWeatherChange: StudyObserveWeatherChange,
    private val settingsRepo: StudySettingsRepo,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                observeWeatherChange(),
                settingsRepo.observeTempScale(),
                settingsRepo.observeFavoriteLocation(),
            ) { _, _, _ -> }
                // the first value is the state the widgets were already drawn from
                .drop(1)
                .collectLatest { updateAll() }
        }
    }

    suspend fun updateAll() {
        runCatching {
            StudyWidgetFrame.entries.forEach { StudyHomeWeatherAppWidget(it).updateAll(context) }
            StudyHomeClockAppWidget().updateAll(context)
        }
    }
}
