package dev.local.weatherstudy.system.service

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.SystemService
 *
 * ### Why this module exists
 *
 * This is the most instructive boundary in Samsung Weather. The APK contains four
 * sibling namespaces, each with its own `R.java` — i.e. four separate Gradle modules:
 *
 * ```
 * com.samsung.android.weather.system.service          ← 25 interfaces (this module)
 * com.samsung.android.weather.system.service.android  ← 23 AOSP implementations
 * com.samsung.android.weather.system.service.sep      ← 21 Samsung (SEP) implementations
 * com.samsung.android.weather.system.service.dev      ← mock implementations
 * ```
 *
 * **No app code anywhere calls a platform API directly.** Everything goes through one
 * of these interfaces, and DI picks the implementation at runtime from
 * `getType()` / `PlatformType`. That is why the app can run on a non-Samsung Android
 * build at all, and it is precisely the seam a clean-room rebuild substitutes into:
 * the reconstruction supplies the AOSP implementations and stubs the Samsung ones,
 * changing nothing above this line.
 *
 * [StudySystemService] is the single facade every consumer injects — one dependency
 * instead of 21. `DetailViewModel` takes it; so do the widgets and the condition chain.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudySystemService {
    fun getType(): String

    fun getActivityService(): StudyActivityService
    fun getConnectivityService(): StudyConnectivityService
    fun getCscFeature(): StudyCscFeature
    fun getDesktopService(): StudyDesktopService
    fun getDeviceService(): StudyDeviceService
    fun getEdgeManager(): StudyEdgeManager
    fun getFloatingFeature(): StudyFloatingFeature
    fun getFoldStateService(): StudyFoldStateService
    fun getListViewService(): StudyListViewService
    fun getLocaleService(): StudyLocaleService
    fun getPackageService(): StudyPackageService
    fun getRemoteViewsService(): StudyRemoteViewsService
    fun getSensitiveService(): StudySensitiveService
    fun getShortcutService(): StudyShortcutService
    fun getSipService(): StudySipService
    fun getSmartTipService(): StudySmartTipService
    fun getTelephonyService(): StudyTelephonyService
    fun getViewService(): StudyViewService
    fun getWidgetService(): StudyWidgetService
    fun getWindowService(): StudyWindowService
}

/**
 * Corresponds conceptually to `…system.service.PlatformType`.
 *
 * Observed responsibility: which implementation family is in force. The original
 * distinguishes several Samsung variants (`isSep`, `isSepLite`, `isSepWear`, `isSdl`),
 * because the available APIs differ between them — see [StudyDeviceService].
 */
enum class StudyPlatformType {
    /** stock Android — the `…service.android` implementations */
    ANDROID,

    /** Samsung Experience Platform — the `…service.sep` implementations */
    SAMSUNG,

    /** mocks, used by developer options and tests — the `…service.dev` implementations */
    MOCK,
}

/**
 * Corresponds conceptually to `…system.service.SystemServiceProvider`.
 *
 * Observed responsibility: decides, once, which family to bind. In the original this is
 * what makes `isSamsungPlatform()` the only runtime check in the whole app.
 */
interface StudySystemServiceProvider {
    fun getPlatformType(): StudyPlatformType
    fun getSystemService(): StudySystemService
}
