package dev.local.weatherstudy.system.service.samsung.impl

import android.os.UserHandle
import dev.local.weatherstudy.system.service.StudyDeviceService
import dev.local.weatherstudy.system.service.StudyPlatformType
import dev.local.weatherstudy.system.service.samsung.StudySamsungApiUnavailable
import javax.inject.Inject

/**
 * Educational reconstruction (stub — see [StudySamsungApiUnavailable]).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.sep.impl.SepDeviceService
 *
 * Original dependency: Samsung firmware — `android.os.SemSystemProperties`
 * ("ro.csc.sales_code"), `SemFloatingFeature`, `SemSystemProperties` ONE UI version,
 * `Build.VERSION.SEM_PLATFORM_INT`, `UserHandle.semGetMyUserId()`,
 * `SemVibrator` / `semVibrate`.
 *
 * Why unavailable: these classes exist only in `framework.jar` on a Samsung device.
 *
 * Where used: region routing (`StudyForecastProvider.dispatchByCountryCode`), policy
 * selection by One UI version, retail-mode detection, the haptic on pull-to-refresh.
 *
 * Replacement behaviour: methods whose answer the AOSP implementation can supply
 * delegate to it conceptually (and are implemented plainly here); the genuinely
 * Samsung-only ones throw [StudySamsungApiUnavailable] so a mis-wired DI graph fails
 * loudly instead of silently reporting a non-Samsung device as Samsung.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudySamsungDeviceServiceBridge @Inject constructor() : StudyDeviceService {

    // ---- Samsung-only: ro.csc.sales_code, SemSystemProperties ----
    override fun getSalesCode(): String = throw unavailable("SemSystemProperties(ro.csc.sales_code)")
    override fun getSecLogLevel(): String = throw unavailable("SemSystemProperties(ro.boot.debug_level)")
    override fun getOneUiVersion(): Int = throw unavailable("SemSystemProperties ONE UI version")
    override fun semInt(): Int = throw unavailable("Build.VERSION.SEM_PLATFORM_INT")
    override fun getMyUserId(): Int = throw unavailable("UserHandle.semGetMyUserId()")
    override fun getUserHandleAll(): UserHandle = throw unavailable("UserHandle.SEM_ALL")
    override fun isSdl(): Boolean = throw unavailable("SemFloatingFeature SDL flag")
    override fun isSepLite(): Boolean = throw unavailable("SemFloatingFeature SEP-Lite flag")
    override fun isSepWear(): Boolean = throw unavailable("SemFloatingFeature SEP-Wear flag")
    override fun isRetailMode(): Boolean = throw unavailable("SemSystemProperties retail flag")
    override fun isApplyTheme(): Boolean = throw unavailable("Samsung Themes framework")
    override fun isUserBetaVersion(): Boolean = throw unavailable("SemSystemProperties beta flag")
    override fun getReduceAnimation(): Int = throw unavailable("Samsung reduce-animation setting")
    override fun isDetachMode(): Boolean = throw unavailable("Samsung DeX detach state")
    override fun isStandalone(): Boolean = throw unavailable("Samsung DeX standalone state")
    override fun haptic(): Unit = throw unavailable("SemVibrator predefined effect")
    override fun vibrate(index: Int, repeat: Boolean): Unit =
        throw unavailable("Vibrator.semVibrate(index, repeat)")
    override fun isAmxOperator(salesCode: String): Boolean = throw unavailable("CSC operator table")
    override fun isVietnamOperator(salesCode: String): Boolean = throw unavailable("CSC operator table")

    // ---- these the bridge can answer honestly ----
    override fun getPlatform(): StudyPlatformType = StudyPlatformType.SAMSUNG
    override fun isSamsungPlatform(): Boolean = true
    override fun isSep(): Boolean = true
    override fun isSepPlatform(): Boolean = true
    override fun isLegacy(): Boolean = false

    // ---- public-API answers, identical to the AOSP implementation ----
    override fun getCountryCode(): String = throw unavailable("resolved via CSC sales code")
    override fun getDeviceCountryCode(): String = throw unavailable("resolved via CSC sales code")
    override fun getLocaleCountryCode(): String = java.util.Locale.getDefault().country.uppercase()
    override fun getMcc(): String = ""
    override fun getMnc(): String = ""
    override fun getAbiType(): String = android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
    override fun getFirstAPILevel(): Int = android.os.Build.VERSION.SDK_INT
    override fun isTablet(): Boolean = false
    override fun isScreenOn(): Boolean = true
    override fun isWifiOnly(): Boolean = false
    override fun getDisplayDeviceType(): Int = 0

    private fun unavailable(api: String) = StudySamsungApiUnavailable(api)
}
