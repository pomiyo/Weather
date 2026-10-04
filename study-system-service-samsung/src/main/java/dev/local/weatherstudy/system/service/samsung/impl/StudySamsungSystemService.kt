package dev.local.weatherstudy.system.service.samsung.impl

import dev.local.weatherstudy.system.service.StudyActivityService
import dev.local.weatherstudy.system.service.StudyConnectivityService
import dev.local.weatherstudy.system.service.StudyCscFeature
import dev.local.weatherstudy.system.service.StudyDesktopService
import dev.local.weatherstudy.system.service.StudyDeviceService
import dev.local.weatherstudy.system.service.StudyEdgeManager
import dev.local.weatherstudy.system.service.StudyFloatingFeature
import dev.local.weatherstudy.system.service.StudyFoldStateService
import dev.local.weatherstudy.system.service.StudyListViewService
import dev.local.weatherstudy.system.service.StudyLocaleService
import dev.local.weatherstudy.system.service.StudyPackageService
import dev.local.weatherstudy.system.service.StudyPlatformType
import dev.local.weatherstudy.system.service.StudyRemoteViewsService
import dev.local.weatherstudy.system.service.StudySensitiveService
import dev.local.weatherstudy.system.service.StudyShortcutService
import dev.local.weatherstudy.system.service.StudySipService
import dev.local.weatherstudy.system.service.StudySmartTipService
import dev.local.weatherstudy.system.service.StudySystemService
import dev.local.weatherstudy.system.service.StudyTelephonyService
import dev.local.weatherstudy.system.service.StudyViewService
import dev.local.weatherstudy.system.service.StudyWidgetService
import dev.local.weatherstudy.system.service.StudyWindowService
import javax.inject.Inject
import javax.inject.Provider

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.sep.SepSystemService
 *
 * Observed responsibility: the Samsung facade. Structurally identical to the AOSP one
 * with one difference worth keeping — the decompiled output contains a synthetic
 * `SepSystemService$foldStateService$2` class, i.e. **the fold-state service is created
 * lazily** (`by lazy`), where the other nineteen are injected eagerly. That is because
 * constructing it registers a listener with `SemWindowManager`, so a device that never
 * asks about folding never touches the API.
 *
 * Reconstructed with a `Provider` + `by lazy` for exactly that field.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudySamsungSystemService @Inject constructor(
    private val activityService: StudyActivityService,
    private val connectivityService: StudyConnectivityService,
    private val cscFeature: StudyCscFeature,
    private val desktopService: StudyDesktopService,
    private val deviceService: StudyDeviceService,
    private val edgeManager: StudyEdgeManager,
    private val floatingFeature: StudyFloatingFeature,
    private val foldStateServiceProvider: Provider<StudyFoldStateService>,
    private val listViewService: StudyListViewService,
    private val localeService: StudyLocaleService,
    private val packageService: StudyPackageService,
    private val remoteViewsService: StudyRemoteViewsService,
    private val sensitiveService: StudySensitiveService,
    private val shortcutService: StudyShortcutService,
    private val sipService: StudySipService,
    private val smartTipService: StudySmartTipService,
    private val telephonyService: StudyTelephonyService,
    private val viewService: StudyViewService,
    private val widgetService: StudyWidgetService,
    private val windowService: StudyWindowService,
) : StudySystemService {

    /** lazily created: constructing it registers a SemWindowManager fold listener */
    private val lazyFoldStateService: StudyFoldStateService by lazy { foldStateServiceProvider.get() }

    override fun getType(): String = StudyPlatformType.SAMSUNG.name

    override fun getActivityService() = activityService
    override fun getConnectivityService() = connectivityService
    override fun getCscFeature() = cscFeature
    override fun getDesktopService() = desktopService
    override fun getDeviceService() = deviceService
    override fun getEdgeManager() = edgeManager
    override fun getFloatingFeature() = floatingFeature
    override fun getFoldStateService() = lazyFoldStateService
    override fun getListViewService() = listViewService
    override fun getLocaleService() = localeService
    override fun getPackageService() = packageService
    override fun getRemoteViewsService() = remoteViewsService
    override fun getSensitiveService() = sensitiveService
    override fun getShortcutService() = shortcutService
    override fun getSipService() = sipService
    override fun getSmartTipService() = smartTipService
    override fun getTelephonyService() = telephonyService
    override fun getViewService() = viewService
    override fun getWidgetService() = widgetService
    override fun getWindowService() = windowService
}
