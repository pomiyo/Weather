package dev.local.weatherstudy.domain.entity.widget

import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.widget.WidgetInfo
 *
 * Observed responsibilities:
 * - one row per placed widget instance, keyed by the AppWidgetManager id
 * - binds that instance to a saved location (`weatherKey`) — which is why removing
 *   a city has to update widgets, not just the weather table
 * - carries the per-instance appearance the widget editor writes: background colour
 *   and transparency, shape, night mode, and whether hourly/news sections show
 * - `components` is an ordered list: the widget editor lets the user reorder the
 *   blocks inside a widget, stored in its own table (TABLE_WIDGET_COMPONENT_INFO)
 * - `widgetRestoreMode` and `widgetAddedInDCMLauncher` exist for backup/restore and
 *   for a carrier launcher variant — both are state, not preference
 *
 * 12 fields, matching the original.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyWidgetInfo(
    val widgetId: Int,
    val weatherKey: String = "",
    val showBackground: Int = SHOW,
    val showHourly: Int = SHOW,
    val showNews: Int = HIDE,
    val widgetBGColor: Int = BG_COLOR_DEFAULT,
    val widgetBGTransparency: Float = BG_TRANSPARENCY_DEFAULT,
    val widgetNightMode: Int = NIGHT_MODE_SYSTEM,
    val widgetShape: Int = SHAPE_ROUND,
    val widgetRestoreMode: Int = RESTORE_NONE,
    val widgetAddedInDCMLauncher: Int = HIDE,
    val components: List<StudyWidgetComponent> = emptyList(),
) {
    companion object {
        const val INVALID_WIDGET_ID = -1

        const val HIDE = 0
        const val SHOW = 1

        const val BG_COLOR_DEFAULT = 0
        const val BG_TRANSPARENCY_DEFAULT = 0.6f

        const val NIGHT_MODE_SYSTEM = 0
        const val NIGHT_MODE_LIGHT = 1
        const val NIGHT_MODE_DARK = 2

        const val SHAPE_ROUND = 0
        const val SHAPE_SQUARE = 1

        const val RESTORE_NONE = 0
        const val RESTORE_PENDING = 1
        const val RESTORE_DONE = 2
    }
}

/**
 * Corresponds conceptually to `…entity.widget.WidgetComponent`.
 *
 * Observed responsibility: one reorderable block inside a widget. `type` selects
 * which Glance module renders it, `order` is its position — and both together are the
 * composite primary key of TABLE_WIDGET_COMPONENT_INFO.
 */
@JsonClass(generateAdapter = true)
data class StudyWidgetComponent(
    val order: Int,
    val type: Int,
) {
    companion object {
        const val TYPE_NONE = 0
        const val TYPE_CLOCK = 1
        const val TYPE_CURRENT = 2
        const val TYPE_HOURLY = 3
        const val TYPE_DAILY = 4
        const val TYPE_INDEX = 5
        const val TYPE_INSIGHT = 6
        const val TYPE_REFRESH = 7
    }
}

/** Corresponds conceptually to `…entity.widget.InstalledBriefWidgetList`. */
data class StudyInstalledBriefWidgetList(val widgetIds: List<Int>)

/** Corresponds conceptually to `…entity.widget.InstalledHomeWidgetSize`. */
data class StudyInstalledHomeWidgetSize(val widgetId: Int, val columns: Int, val rows: Int)

/** Corresponds conceptually to `…entity.widget.WidgetStatusLoggingInfo`. */
data class StudyWidgetStatusLoggingInfo(
    val widgetType: String,
    val count: Int,
    val hasLocation: Boolean,
)
