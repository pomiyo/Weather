package dev.local.weatherstudy.system.service.samsung.impl

import android.content.ComponentName
import android.content.Context
import dev.local.weatherstudy.system.service.StudyCscFeature
import dev.local.weatherstudy.system.service.StudyDesktopService
import dev.local.weatherstudy.system.service.StudyDeviceService
import dev.local.weatherstudy.system.service.StudyEdgeManager
import dev.local.weatherstudy.system.service.StudyFloatingFeature
import dev.local.weatherstudy.system.service.StudyFoldStateListener
import dev.local.weatherstudy.system.service.StudyFoldStateService
import dev.local.weatherstudy.system.service.StudyPackageService
import dev.local.weatherstudy.system.service.StudySensitiveService
import dev.local.weatherstudy.system.service.StudyTelephonyService
import dev.local.weatherstudy.system.service.StudyWidgetService
import dev.local.weatherstudy.system.service.samsung.StudySamsungApiUnavailable
import javax.inject.Inject

/**
 * Educational reconstruction (stubs — see [StudySamsungApiUnavailable]).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.sep.impl.SepCscFeature
 * …SepFloatingFeature, …SepFoldStateService, …SepEdgeManager, …SepWidgetService,
 * …SepPackageService, …SepSensitiveService, …SepTelephonyService, …SepDesktopService
 *
 * Original dependency / why unavailable / where used — per class:
 *
 * | Bridge | Samsung API | Used by |
 * |---|---|---|
 * | [StudySamsungCscFeatureBridge] | `com.samsung.android.feature.SemCscFeature` | default temp unit, default refresh interval, screen-on refresh, regional predicates |
 * | [StudySamsungFloatingFeatureBridge] | `com.samsung.android.feature.SemFloatingFeature` | fold vs flip detection → layout strategy, cover widgets, AOD |
 * | [StudySamsungFoldStateServiceBridge] | `SemWindowManager.FoldStateListener` | re-laying out the detail grid when the device folds |
 * | [StudySamsungEdgeManagerBridge] | Samsung Edge framework | whether the edge panel provider is active |
 * | [StudySamsungWidgetServiceBridge] | Samsung launcher widget span query | widget size → which Glance frame to render |
 * | [StudySamsungPackageServiceBridge] | `SemPackageManager`, Secure Folder | enabling the launcher activity, Secure Folder checks |
 * | [StudySamsungSensitiveServiceBridge] | privileged IMEI read | device identification in analytics |
 * | [StudySamsungTelephonyServiceBridge] | `SemTelephonyManager` | CSC-correlated network country |
 * | [StudySamsungDesktopServiceBridge] | Samsung DeX | desktop mode → detail column count |
 *
 * Replacement behaviour: throw, naming the API. Nothing here spoofs a signature or
 * reaches a privileged service.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudySamsungCscFeatureBridge @Inject constructor() : StudyCscFeature {
    override fun getTemperatureUnit(): Int = throw unavailable()
    override fun getDefaultAutoRefreshInterval(): Int = throw unavailable()
    override fun enableScreenOnRefresh(): Boolean = throw unavailable()
    override fun isSupportMinimizedSIP(): Boolean = throw unavailable()
    override fun isVerizon(): Boolean = throw unavailable()
    override fun isTaiwan(): Boolean = throw unavailable()
    override fun isHongKong(): Boolean = throw unavailable()
    override fun isMEA(): Boolean = throw unavailable()

    private fun unavailable() = StudySamsungApiUnavailable("com.samsung.android.feature.SemCscFeature")
}

/** Bridge for `SepFloatingFeature`. */
class StudySamsungFloatingFeatureBridge @Inject constructor() : StudyFloatingFeature {
    override fun isFoldDevice(): Boolean = throw unavailable()
    override fun isFlipDevice(): Boolean = throw unavailable()
    override fun getAodFeature(): Boolean = throw unavailable()

    private fun unavailable() =
        StudySamsungApiUnavailable("com.samsung.android.feature.SemFloatingFeature")
}

/** Bridge for `SepFoldStateService`. */
class StudySamsungFoldStateServiceBridge @Inject constructor() : StudyFoldStateService {
    override fun isFolded(): Boolean = throw unavailable()
    override fun isFlipCoverScreen(context: Context): Boolean = throw unavailable()
    override fun registerFoldStateListener(
        listener: StudyFoldStateListener,
        handler: android.os.Handler?,
    ): Unit = throw unavailable()
    override fun unregisterFoldStateListener(listener: StudyFoldStateListener): Unit = throw unavailable()

    private fun unavailable() =
        StudySamsungApiUnavailable("SemWindowManager.FoldStateListener")
}

/** Bridge for `SepEdgeManager`. */
class StudySamsungEdgeManagerBridge @Inject constructor() : StudyEdgeManager {
    override fun isEdgeEnabled(context: Context): Boolean =
        throw StudySamsungApiUnavailable("Samsung Edge panel framework")
}

/** Bridge for `SepWidgetService`. */
class StudySamsungWidgetServiceBridge @Inject constructor() : StudyWidgetService {
    override fun getAppWidgetColumnSpan(): String = throw unavailable()
    override fun getAppWidgetRowSpan(): String = throw unavailable()

    private fun unavailable() =
        StudySamsungApiUnavailable("Samsung launcher widget span options")
}

/** Bridge for `SepPackageService`. */
class StudySamsungPackageServiceBridge @Inject constructor() : StudyPackageService {
    override fun getSystemFeatureLevel(packageName: String): Int = throw unavailable()
    override fun isSecureFolderEnabled(userId: Int): Boolean = throw unavailable()
    override fun setComponentEnable(componentName: ComponentName, enable: Boolean, flag: Int): Unit =
        throw unavailable()

    private fun unavailable() = StudySamsungApiUnavailable("SemPackageManager / Secure Folder")
}

/** Bridge for `SepSensitiveService`. */
class StudySamsungSensitiveServiceBridge @Inject constructor() : StudySensitiveService {
    override fun getImei(): String =
        throw StudySamsungApiUnavailable(
            "privileged IMEI read",
            "reading a device identifier needs a privileged permission from Android 10 on; " +
                "the reconstruction never requests one",
        )
}

/** Bridge for `SepTelephonyService`. */
class StudySamsungTelephonyServiceBridge @Inject constructor() : StudyTelephonyService {
    override fun getCountryCode(): String = throw unavailable()
    override fun getNetworkCountryCode(): String = throw unavailable()
    override fun getMcc(): String = throw unavailable()
    override fun getMnc(): String = throw unavailable()
    override fun getSimState(): Int = throw unavailable()
    override fun getDataState(): Int = throw unavailable()
    override fun isSimEnabled(): Boolean = throw unavailable()
    override fun isNetworkRoaming(): Boolean = throw unavailable()

    private fun unavailable() = StudySamsungApiUnavailable("SemTelephonyManager")
}

/** Bridge for `SepDesktopService`. */
class StudySamsungDesktopServiceBridge @Inject constructor() : StudyDesktopService {
    override fun isDesktopMode(floatingFeature: StudyFloatingFeature): Boolean = throw unavailable()
    override fun isStandaloneMode(
        floatingFeature: StudyFloatingFeature,
        device: StudyDeviceService,
    ): Boolean = throw unavailable()

    private fun unavailable() = StudySamsungApiUnavailable("Samsung DeX (SemDesktopModeManager)")
}
