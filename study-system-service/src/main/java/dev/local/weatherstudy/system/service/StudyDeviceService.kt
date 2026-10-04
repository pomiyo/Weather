package dev.local.weatherstudy.system.service

import android.os.UserHandle

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.DeviceService
 *
 * Observed responsibility: the widest of the 21 interfaces (35 methods) and the one
 * that reveals most about what the app needs from the firmware. Three groups:
 *
 * 1. **Identity and region** — `salesCode` (the Samsung CSC code), `mcc`/`mnc`,
 *    three different country codes (device, locale, and the resolved one). The region
 *    routing in `StudyForecastProvider.dispatchByCountryCode` is fed from here.
 * 2. **Platform discrimination** — `isSep`, `isSepLite`, `isSepWear`, `isSdl`,
 *    `isSamsungPlatform`, `semInt`. Each gates a different subset of SEP APIs.
 * 3. **Device state** — tablet, detach/standalone (DeX), retail mode, screen on,
 *    Wi-Fi only, reduce-animation, haptics.
 *
 * Everything in groups 1 and 2 is unavailable off a Samsung build; see
 * `reports/samsung-bridge-map.md` for the per-method substitution.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyDeviceService {
    // identity / region
    fun getSalesCode(): String
    fun getCountryCode(): String
    fun getDeviceCountryCode(): String
    fun getLocaleCountryCode(): String
    fun getMcc(): String
    fun getMnc(): String
    fun getAbiType(): String
    fun getFirstAPILevel(): Int
    fun getOneUiVersion(): Int
    fun getSecLogLevel(): String
    fun getMyUserId(): Int
    fun getUserHandleAll(): UserHandle?

    // platform discrimination
    fun getPlatform(): StudyPlatformType
    fun isSamsungPlatform(): Boolean
    fun isSep(): Boolean
    fun isSepLite(): Boolean
    fun isSepWear(): Boolean
    fun isSepPlatform(): Boolean
    fun isSdl(): Boolean
    fun isLegacy(): Boolean
    fun semInt(): Int

    // device state
    fun isTablet(): Boolean
    fun isScreenOn(): Boolean
    fun isWifiOnly(): Boolean
    fun isDetachMode(): Boolean
    fun isStandalone(): Boolean
    fun isRetailMode(): Boolean
    fun isApplyTheme(): Boolean
    fun isUserBetaVersion(): Boolean
    fun getDisplayDeviceType(): Int
    fun getReduceAnimation(): Int

    // carrier predicates the original keeps here rather than in CscFeature
    fun isAmxOperator(salesCode: String): Boolean
    fun isVietnamOperator(salesCode: String): Boolean

    // haptics
    fun haptic()
    fun vibrate(index: Int, repeat: Boolean)
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.CscFeature
 *
 * Observed responsibility: carrier/country feature overrides read from Samsung's CSC
 * (Country Specific Code) database. This is pure firmware — there is no AOSP equivalent,
 * which is why the AOSP implementation answers from resources and build config instead.
 *
 * Note what it controls: the DEFAULT temperature unit, the DEFAULT auto-refresh
 * interval, and whether refreshing on screen-on is allowed. Carrier policy reaches into
 * app behaviour here, not just into branding.
 */
interface StudyCscFeature {
    fun getTemperatureUnit(): Int
    fun getDefaultAutoRefreshInterval(): Int
    fun enableScreenOnRefresh(): Boolean
    fun isSupportMinimizedSIP(): Boolean
    fun isVerizon(): Boolean
    fun isTaiwan(): Boolean
    fun isHongKong(): Boolean
    fun isMEA(): Boolean
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.FloatingFeature
 *
 * Observed responsibility: Samsung's `SemFloatingFeature` — compile-time device
 * capability strings. Used to tell a fold from a flip, which changes the whole layout
 * strategy (see `GetColumnSize` and the cover widgets).
 */
interface StudyFloatingFeature {
    fun isFoldDevice(): Boolean
    fun isFlipDevice(): Boolean
    fun getAodFeature(): Boolean
}

/** Corresponds conceptually to `…system.service.TelephonyService`. */
interface StudyTelephonyService {
    fun getCountryCode(): String
    fun getNetworkCountryCode(): String
    fun getMcc(): String
    fun getMnc(): String
    fun getSimState(): Int
    fun getDataState(): Int
    fun isSimEnabled(): Boolean
    fun isNetworkRoaming(): Boolean
}

/**
 * Corresponds conceptually to `…system.service.ConnectivityService`.
 *
 * Observed responsibility: `checkBackgroundRestricted()` returns an int, not a boolean —
 * it distinguishes "restricted", "allowed" and "unknown", and `BackgroundRestrictCondition`
 * branches on all three.
 */
interface StudyConnectivityService {
    fun checkNetworkConnected(): Boolean
    fun checkBackgroundRestricted(): Int
    fun hasTransport(transport: Int): Boolean

    companion object {
        const val BACKGROUND_UNKNOWN = 0
        const val BACKGROUND_ALLOWED = 1
        const val BACKGROUND_RESTRICTED = 2
    }
}

/** Corresponds conceptually to `…system.service.LocaleService`. */
interface StudyLocaleService {
    fun getLocale(): java.util.Locale
    fun getLanguage(): String

    /** drives the hour format in the hourly card and the sun/moon rows */
    fun isAmPmBeforeTime(): Boolean

    /** the detail screen mirrors its Canvas graphs on this; see StudyBezierLineGraphItemView */
    fun isRtl(): Boolean
}

/** Corresponds conceptually to `…system.service.SensitiveService` — IMEI access. */
interface StudySensitiveService {
    fun getImei(): String
}

/** Corresponds conceptually to `…system.service.PackageService`. */
interface StudyPackageService {
    fun getSystemFeatureLevel(packageName: String): Int
    fun isSecureFolderEnabled(userId: Int): Boolean

    /** how `AppLauncherActivity` gets enabled/disabled at runtime */
    fun setComponentEnable(componentName: android.content.ComponentName, enable: Boolean, flag: Int)
}

/** Corresponds conceptually to `…system.service.ActivityService`. */
interface StudyActivityService {
    fun isResumed(activity: android.app.Activity): Boolean
}

/**
 * Corresponds conceptually to `…system.service.DesktopService`.
 *
 * Observed responsibility: Samsung DeX. `DetailViewModel.isDesktopMode` comes from here
 * and changes the detail grid's column count and the context-menu behaviour.
 */
interface StudyDesktopService {
    fun isDesktopMode(floatingFeature: StudyFloatingFeature): Boolean
    fun isStandaloneMode(floatingFeature: StudyFloatingFeature, device: StudyDeviceService): Boolean
}

/** Corresponds conceptually to `…system.service.FoldStateListener`. */
interface StudyFoldStateListener {
    fun onFoldStateChanged(folded: Boolean)
}

/** Corresponds conceptually to `…system.service.FoldStateService`. */
interface StudyFoldStateService {
    fun isFolded(): Boolean
    fun isFlipCoverScreen(context: android.content.Context): Boolean
    fun registerFoldStateListener(listener: StudyFoldStateListener, handler: android.os.Handler?)
    fun unregisterFoldStateListener(listener: StudyFoldStateListener)
}

/** Corresponds conceptually to `…system.service.EdgeManager`. */
interface StudyEdgeManager {
    fun isEdgeEnabled(context: android.content.Context): Boolean
}

/** Corresponds conceptually to `…system.service.WidgetService`. */
interface StudyWidgetService {
    fun getAppWidgetColumnSpan(): String
    fun getAppWidgetRowSpan(): String
}

/** Corresponds conceptually to `…system.service.ShortcutService`. */
interface StudyShortcutService {
    fun hasShortcut(): Boolean
    fun isRequestPinShortcutSupported(homeOnlyMode: Boolean): Boolean
    fun addShortcut(
        component: android.content.ComponentName,
        labelRes: Int,
        iconRes: Int,
        homeOnlyMode: Boolean,
    )
}
