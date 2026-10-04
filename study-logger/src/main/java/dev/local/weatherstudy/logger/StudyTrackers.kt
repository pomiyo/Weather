package dev.local.weatherstudy.logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.logger.AppTracker
 * com.samsung.android.weather.logger.DataTracker
 * com.samsung.android.weather.logger.VocTracker
 * com.samsung.android.weather.logger.LifeCycleLogger
 * com.samsung.android.weather.logger.analytics.WeatherAnalytics
 * com.samsung.android.weather.logger.analytics.StatusAnalyticsManager
 *
 * ### What the original does, and where it goes
 *
 * The original has **three separate tracker surfaces**, and the split is meaningful:
 *
 * | Tracker | Destination | What it carries |
 * |---|---|---|
 * | `AppTracker` | Samsung Analytics (SA) | screen views, button events |
 * | `DataTracker` | UReCA | data-quality / payload events |
 * | `VocTracker` | Voice of Customer | user-reported feedback |
 *
 * plus `LifeCycleLogger` (process and activity lifecycle) and
 * `StatusAnalyticsManager` (a periodic snapshot of widget counts, saved locations and
 * settings — the "status logging" the `WidgetStatusLoggingInfo` entity feeds).
 *
 * All three destinations are Samsung endpoints, so the reconstruction keeps the
 * interfaces and routes everything to logcat. The 28 `*Tracking` classes in
 * `logger.analytics.tracking` are per-screen façades over these — see
 * [dev.local.weatherstudy.logger.analytics.tracking.StudyDetailTracking].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyAppTracker {
    fun trackScreen(screenId: String)
    fun trackEvent(screenId: String, eventId: String, detail: String? = null, value: Long? = null)
}

/** Corresponds conceptually to `…logger.DataTracker`. */
interface StudyDataTracker {
    fun trackPayload(providerId: String, endpoint: String, resultCode: Int)
    fun trackParseFailure(providerId: String, endpoint: String, reason: String)
}

/** Corresponds conceptually to `…logger.VocTracker`. */
interface StudyVocTracker {
    fun trackReportIncorrectInfo(locationKey: String, providerId: String, category: String)
}

/**
 * Corresponds conceptually to `…logger.analytics.WeatherAnalytics` —
 * the facade the app actually injects, over the three trackers.
 */
interface StudyWeatherAnalytics : StudyAppTracker, StudyDataTracker, StudyVocTracker

/**
 * Educational reconstruction.
 *
 * The reconstruction's single implementation: everything goes to logcat.
 *
 * Observed responsibility of the original: deliver to Samsung Analytics, UReCA and VoC.
 * Those are Samsung-mediated endpoints requiring a registered tracking id, so they are
 * not reachable and not reconstructed — see `reports/samsung-bridge-map.md`. Keeping the
 * interfaces means every call site in the app is still present and still visible, which
 * is the point: you can see *what* the original measures without sending anything.
 */
@Singleton
class StudyLogcatAnalytics @Inject constructor() : StudyWeatherAnalytics {

    override fun trackScreen(screenId: String) {
        Log.d(TAG, "screen: $screenId")
    }

    override fun trackEvent(screenId: String, eventId: String, detail: String?, value: Long?) {
        Log.d(TAG, "event: screen=$screenId id=$eventId detail=$detail value=$value")
    }

    override fun trackPayload(providerId: String, endpoint: String, resultCode: Int) {
        Log.d(TAG, "payload: provider=$providerId endpoint=$endpoint code=$resultCode")
    }

    override fun trackParseFailure(providerId: String, endpoint: String, reason: String) {
        Log.w(TAG, "parse failure: provider=$providerId endpoint=$endpoint reason=$reason")
    }

    override fun trackReportIncorrectInfo(locationKey: String, providerId: String, category: String) {
        Log.d(TAG, "voc: key=$locationKey provider=$providerId category=$category")
    }

    private companion object {
        const val TAG = "StudyAnalytics"
    }
}

/**
 * Corresponds conceptually to `…logger.LifeCycleLogger`.
 *
 * Observed responsibility: process and activity lifecycle breadcrumbs. The original
 * registers it from the Application, which is why it is a `Singleton` with no screen
 * context of its own.
 */
@Singleton
class StudyLifeCycleLogger @Inject constructor() {
    fun onProcessStart() = Log.d(TAG, "process start")
    fun onActivityResumed(name: String) = Log.d(TAG, "resumed: $name")
    fun onActivityPaused(name: String) = Log.d(TAG, "paused: $name")

    private companion object {
        const val TAG = "StudyLifeCycle"
    }
}

/**
 * Corresponds conceptually to `…logger.analytics.StatusAnalyticsManager`.
 *
 * Observed responsibility: a periodic snapshot rather than an event — how many widgets
 * of each kind are placed, how many locations are saved, what the settings are. It is
 * what `StudyWidgetStatusLoggingInfo` in the domain exists to carry.
 */
@Singleton
class StudyStatusAnalyticsManager @Inject constructor(
    private val analytics: StudyWeatherAnalytics,
) {
    fun reportStatus(
        savedLocationCount: Int,
        widgetCounts: Map<String, Int>,
        tempScale: Int,
        autoRefreshInterval: Int,
    ) {
        analytics.trackEvent(
            screenId = SCREEN_STATUS,
            eventId = EVENT_SNAPSHOT,
            detail = "locations=$savedLocationCount widgets=$widgetCounts " +
                "tempScale=$tempScale interval=$autoRefreshInterval",
        )
    }

    private companion object {
        const val SCREEN_STATUS = "status"
        const val EVENT_SNAPSHOT = "snapshot"
    }
}
