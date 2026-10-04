package dev.local.weatherstudy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.local.weatherstudy.logger.StudyLifeCycleLogger
import dev.local.weatherstudy.widget.home.StudyWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.App
 *
 * ### The startup sequence, resolved
 *
 * `reports/FINAL_REPORT.md` §24 lists the order inside `App` as UNKNOWN. Reconstructing
 * the condition chain answers it — the sequence is:
 *
 * ```
 * 1. androidx.startup.InitializationProvider
 *      runs EmojiCompat, ProcessLifecycle and ProfileInstaller initializers
 *      — BEFORE Application.onCreate, because a ContentProvider is created first
 * 2. App.onCreate  (@HiltAndroidApp)
 *      Hilt builds the SingletonComponent; nothing is eagerly resolved
 * 3. Configuration.Provider
 *      hands WorkManager a HiltWorkerFactory, so workers can be injected
 * 4. MainActivity → NavHostFragment → weather_nav.xml start destination
 * 5. MainFragment runs ConditionManager.check(Scenario.DetailProcess)
 *      ← the EULA/permission gating happens HERE, not in the Application
 * 6. the chain's first unsatisfied condition becomes a MainDestination
 * ```
 *
 * The important finding is step 5: the gating is a **navigation decision made by the
 * start destination**, not an Application-level block. Nothing in `App` touches the
 * database, the network or the location subsystem — they are all resolved lazily on
 * first injection. See `reports/startup-flow.md`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@HiltAndroidApp
class StudyWeatherApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var lifeCycleLogger: StudyLifeCycleLogger

    @Inject lateinit var widgetUpdater: StudyWidgetUpdater

    /** process-lifetime work that belongs to no screen */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** step 3: workers need injection, so WorkManager is configured rather than default-initialised */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        lifeCycleLogger.onProcessStart()
        // widgets are pushed, never polled: redraw them whenever the stored weather changes
        widgetUpdater.start(applicationScope)
    }
}
