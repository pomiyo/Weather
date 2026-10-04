package dev.local.weatherstudy.logger.analytics.tracking

import dev.local.weatherstudy.logger.StudyWeatherAnalytics
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 28 classes in
 * com.samsung.android.weather.logger.analytics.tracking
 * (`DetailTracking`, `MainTracking`, `LocationsTracking`, `SearchTracking`,
 * `SettingTracking`, `WidgetTracking`, …)
 *
 * ### Why there is one tracking class per screen
 *
 * Samsung Analytics events are identified by a **screen id plus an event id**, both
 * fixed strings. Putting them in a per-screen class means:
 *
 * - the screen id is written once, not at every call site
 * - the event vocabulary for a screen is a readable list of methods, so you can see
 *   what a screen measures by opening one file
 * - the ViewModel injects a typed tracker rather than an analytics SDK — which is why
 *   `DetailViewModel` has `DetailTracking` as one of its 29 constructor parameters
 *   and never touches the analytics facade directly
 *
 * The reconstruction keeps the pattern and a representative set of the events. Nothing
 * is transmitted; see [dev.local.weatherstudy.logger.StudyLogcatAnalytics].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailTracking @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun onEnterDetail() = analytics.trackScreen(SCREEN)
    fun onPullToRefresh() = analytics.trackEvent(SCREEN, "refresh_pull")
    fun onCardClick(cardType: String) = analytics.trackEvent(SCREEN, "card_click", cardType)
    fun onInsightCardSwipe(index: Int) =
        analytics.trackEvent(SCREEN, "insight_swipe", value = index.toLong())
    fun onLocationPagerSwipe(index: Int) =
        analytics.trackEvent(SCREEN, "location_swipe", value = index.toLong())
    fun onWebLinkClick(target: String) = analytics.trackEvent(SCREEN, "web_link", target)
    fun onGoToLocations() = analytics.trackEvent(SCREEN, "goto_locations")
    fun onGoToSettings() = analytics.trackEvent(SCREEN, "goto_settings")

    private companion object {
        const val SCREEN = "detail"
    }
}

/** Corresponds conceptually to `…tracking.MainTracking`. */
class StudyMainTracking @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun onLaunch(from: Int) = analytics.trackEvent(SCREEN, "launch", value = from.toLong())
    fun onFirstRun() = analytics.trackEvent(SCREEN, "first_run")
    fun onConsentAccepted() = analytics.trackEvent(SCREEN, "consent_accepted")
    fun onConsentDeclined() = analytics.trackEvent(SCREEN, "consent_declined")
    fun onProviderChanged(from: String, to: String) =
        analytics.trackEvent(SCREEN, "provider_changed", "$from->$to")

    private companion object {
        const val SCREEN = "main"
    }
}

/** Corresponds conceptually to `…tracking.LocationsTracking`. */
class StudyLocationsTracking @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun onEnter() = analytics.trackScreen(SCREEN)
    fun onAddCurrentLocation() = analytics.trackEvent(SCREEN, "add_current")
    fun onDelete(count: Int) = analytics.trackEvent(SCREEN, "delete", value = count.toLong())
    fun onReorder() = analytics.trackEvent(SCREEN, "reorder")
    fun onLabelAssigned(labelType: Int) =
        analytics.trackEvent(SCREEN, "label", value = labelType.toLong())

    private companion object {
        const val SCREEN = "locations"
    }
}

/** Corresponds conceptually to `…tracking.SearchTracking`. */
class StudySearchTracking @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun onEnter() = analytics.trackScreen(SCREEN)
    fun onQuery(length: Int) = analytics.trackEvent(SCREEN, "query", value = length.toLong())
    fun onResultSelected(position: Int) =
        analytics.trackEvent(SCREEN, "result_selected", value = position.toLong())
    fun onAutocompleteUsed() = analytics.trackEvent(SCREEN, "autocomplete")
    fun onNoResults() = analytics.trackEvent(SCREEN, "no_results")

    private companion object {
        const val SCREEN = "search"
    }
}

/** Corresponds conceptually to `…tracking.SettingTracking`. */
class StudySettingTracking @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun onEnter() = analytics.trackScreen(SCREEN)
    fun onTempScaleChanged(scale: Int) =
        analytics.trackEvent(SCREEN, "temp_scale", value = scale.toLong())
    fun onAutoRefreshChanged(interval: Int) =
        analytics.trackEvent(SCREEN, "auto_refresh", value = interval.toLong())
    fun onLifeStyleToggled(type: Int, enabled: Boolean) =
        analytics.trackEvent(SCREEN, "lifestyle_toggle", "$type=$enabled")

    private companion object {
        const val SCREEN = "setting"
    }
}

/** Corresponds conceptually to `…tracking.WidgetTracking`. */
class StudyWidgetTracking @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun onWidgetAdded(widgetType: String) = analytics.trackEvent(SCREEN, "added", widgetType)
    fun onWidgetRemoved(widgetType: String) = analytics.trackEvent(SCREEN, "removed", widgetType)
    fun onWidgetTapped(widgetType: String) = analytics.trackEvent(SCREEN, "tapped", widgetType)
    fun onWidgetConfigured(widgetType: String) = analytics.trackEvent(SCREEN, "configured", widgetType)

    private companion object {
        const val SCREEN = "widget"
    }
}
