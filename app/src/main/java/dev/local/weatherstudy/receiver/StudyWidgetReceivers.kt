package dev.local.weatherstudy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.sync.worker.StudyBackgroundRefreshWorker
import dev.local.weatherstudy.sync.worker.StudyHomeToAwayModeWorker

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the receivers in
 * com.sec.android.daemonapp.receiver (12 app-owned) and
 * com.sec.android.daemonapp.complication.receiver (8 + an abstract base)
 *
 * ### What the receivers actually do: enqueue work
 *
 * None of them does real work inline. Every one resolves to a WorkManager enqueue, which
 * is why the unique work names in [dev.local.weatherstudy.domain.type.StudyKeys] matter:
 * a boot broadcast, five widget ticks and an alarm can arrive together and must collapse
 * into one refresh.
 *
 * The 12 app-owned receivers, from the decoded manifest:
 * `SystemReceiver`, `SystemActionReceiver`, `WidgetActionReceiver`, `CoverActionReceiver`,
 * `NotificationActionReceiver`, `WeatherGeofenceReceiver`, `IntervalRefreshReceiver`,
 * `LegacyReceiver`, `AppsAutoUpdateReceiver`, `RetailModeReceiver`,
 * `WearableRefreshRequestReceiver`, plus `bnr.BackupReceiver`,
 * `interworking.rubin.RubinReceiver` and `interworking.news.NewsPackageReceiver`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudySystemReceiver : BroadcastReceiver() {

    /** boot, package replace, time/timezone change → re-schedule and refresh widgets */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            -> enqueueRefresh(context)
        }
    }

    private fun enqueueRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            StudyBackgroundRefreshWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<StudyBackgroundRefreshWorker>().build(),
        )
    }
}

/**
 * Corresponds conceptually to `…receiver.IntervalRefreshReceiver`.
 *
 * The AlarmManager tick. The original uses an alarm rather than relying solely on
 * WorkManager's periodic work, because the user's chosen interval can be as short as an
 * hour and periodic work has a 15-minute floor with looser timing.
 */
@AndroidEntryPoint
class StudyIntervalRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            StudyBackgroundRefreshWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<StudyBackgroundRefreshWorker>().build(),
        )
    }

    companion object {
        const val ACTION_INTERVAL_REFRESH = "dev.local.weatherstudy.action.INTERVAL_REFRESH"
    }
}

/**
 * Corresponds conceptually to `…receiver.WidgetActionReceiver`.
 *
 * A widget tap arrives here, not at an Activity, so the receiver can choose between
 * "refresh in place" and "open the app" without starting a window first.
 */
@AndroidEntryPoint
class StudyWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> WorkManager.getInstance(context).enqueueUniqueWork(
                StudyBackgroundRefreshWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<StudyBackgroundRefreshWorker>().build(),
            )
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "dev.local.weatherstudy.action.WIDGET_REFRESH"
        const val EXTRA_WIDGET_ID = "widget_id"
    }
}

/**
 * Corresponds conceptually to `…receiver.WeatherGeofenceReceiver`.
 *
 * The geofence transition entry point. It enqueues `HomeToAwayModeWorker` rather than
 * acting inline, because the transition can arrive while the process is cold.
 */
@AndroidEntryPoint
class StudyWeatherGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            StudyHomeToAwayModeWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<StudyHomeToAwayModeWorker>().build(),
        )
    }
}

/** Corresponds conceptually to `…receiver.NotificationActionReceiver`. */
@AndroidEntryPoint
class StudyNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

/**
 * Corresponds conceptually to `…receiver.LegacyReceiver`.
 *
 * Answers broadcasts from the pre-Room era of the app and from long-installed companion
 * apps that still use the legacy content-provider authority. Kept because its existence
 * is the clearest evidence of how long this app has shipped.
 */
@AndroidEntryPoint
class StudyLegacyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

/**
 * Corresponds conceptually to `…receiver.RetailModeReceiver`,
 * `AppsAutoUpdateReceiver` and `WearableRefreshRequestReceiver` — STUBS.
 *
 * All three are triggered by Samsung-only broadcasts (retail demo mode, Galaxy Store
 * auto-update, a paired watch requesting a refresh). The classes are kept so the
 * receiver surface is visible; they take no action.
 */
@AndroidEntryPoint
class StudySamsungOnlyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
