package dev.local.weatherstudy.widget.home.view.item

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the five classes in
 * com.sec.android.daemonapp.home.view.item:
 * `WeatherTemplateData`, `ErrorTemplateData`, `HourlyListItemData`,
 * `DailyListItemData`, `IndexItemData`
 *
 * Observed responsibility: the widget's render input. Note it is a **separate model
 * from the detail screen's state** — widgets render from `StudyBriefWeather`, not the
 * full aggregate, because a RemoteViews update must be cheap and may run for many widget
 * ids at once. That is also why the in-memory DAO tier exists.
 *
 * Strings are pre-formatted here too; the notation layer runs before the widget state is
 * built, exactly as for the detail screen.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyWeatherTemplateData(
    val cityName: String = "",
    val temperatureText: String = "",
    val highLowText: String = "",
    val conditionText: String = "",
    val iconNum: Int = 0,
    val updateTimeText: String = "",
    val isDay: Boolean = true,
    val hourly: List<StudyHourlyListItemData> = emptyList(),
    val daily: List<StudyDailyListItemData> = emptyList(),
    val indices: List<StudyIndexItemData> = emptyList(),
)

/** `…view.item.ErrorTemplateData` — the widget's failure render. */
data class StudyErrorTemplateData(
    val messageText: String = "",
    val isRetryVisible: Boolean = true,
)

/** `…view.item.HourlyListItemData`. */
data class StudyHourlyListItemData(
    val timeText: String = "",
    val temperatureText: String = "",
    val iconNum: Int = 0,
)

/** `…view.item.DailyListItemData`. */
data class StudyDailyListItemData(
    val dayText: String = "",
    val highText: String = "",
    val lowText: String = "",
    val iconNum: Int = 0,
)

/** `…view.item.IndexItemData`. */
data class StudyIndexItemData(
    val titleText: String = "",
    val valueText: String = "",
)
