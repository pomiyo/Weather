package dev.local.weatherstudy.system.service.android.impl

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.TelephonyManager
import dev.local.weatherstudy.system.service.StudyDeviceService
import dev.local.weatherstudy.system.service.StudyPlatformType
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.android.impl.AndroidDeviceService
 *
 * Observed responsibility: the AOSP half of the platform seam — the implementation
 * selected when `SystemServiceProvider` reports a non-Samsung platform. It answers
 * every question [StudyDeviceService] asks using only public Android APIs, and returns
 * a defined "not available" value for the Samsung-only ones rather than throwing,
 * because callers above this line are written to be unconditional.
 *
 * That contrast is the lesson: compare this file with
 * `StudySamsungDeviceServiceBridge` in `:study-system-service-samsung`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyAndroidDeviceService @Inject constructor(
    private val context: Context,
) : StudyDeviceService {

    // ---- identity / region: available from public APIs ----
    override fun getCountryCode(): String =
        getDeviceCountryCode().ifEmpty { getLocaleCountryCode() }

    override fun getDeviceCountryCode(): String =
        telephony()?.networkCountryIso?.uppercase().orEmpty()

    override fun getLocaleCountryCode(): String =
        context.resources.configuration.locales[0].country.uppercase()

    override fun getMcc(): String = telephony()?.networkOperator?.take(MCC_LENGTH).orEmpty()

    override fun getMnc(): String = telephony()?.networkOperator?.drop(MCC_LENGTH).orEmpty()

    override fun getAbiType(): String = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()

    override fun getFirstAPILevel(): Int = Build.VERSION.SDK_INT

    override fun getMyUserId(): Int = 0

    override fun getUserHandleAll(): UserHandle? = null

    // ---- Samsung-only identity: no AOSP equivalent, defined fallbacks ----
    /** CSC sales codes exist only in Samsung firmware. */
    override fun getSalesCode(): String = ""

    /** One UI has no version on AOSP. */
    override fun getOneUiVersion(): Int = ONE_UI_NOT_PRESENT

    override fun getSecLogLevel(): String = ""

    override fun semInt(): Int = 0

    // ---- platform discrimination ----
    override fun getPlatform(): StudyPlatformType = StudyPlatformType.ANDROID
    override fun isSamsungPlatform(): Boolean = false
    override fun isSep(): Boolean = false
    override fun isSepLite(): Boolean = false
    override fun isSepWear(): Boolean = false
    override fun isSepPlatform(): Boolean = false
    override fun isSdl(): Boolean = false
    override fun isLegacy(): Boolean = false

    // ---- device state: public APIs ----
    override fun isTablet(): Boolean =
        context.resources.configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP

    override fun isScreenOn(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return power?.isInteractive ?: true
    }

    override fun isWifiOnly(): Boolean = telephony()?.phoneType == TelephonyManager.PHONE_TYPE_NONE

    override fun isDetachMode(): Boolean = false

    override fun isStandalone(): Boolean = false

    override fun isRetailMode(): Boolean = false

    override fun isApplyTheme(): Boolean = false

    override fun isUserBetaVersion(): Boolean = false

    override fun getDisplayDeviceType(): Int =
        if (isTablet()) DISPLAY_TYPE_TABLET else DISPLAY_TYPE_PHONE

    override fun getReduceAnimation(): Int =
        if (android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        ) {
            1
        } else {
            0
        }

    // ---- carrier predicates: CSC-driven in the original, so unanswerable here ----
    override fun isAmxOperator(salesCode: String): Boolean = false
    override fun isVietnamOperator(salesCode: String): Boolean = false

    // ---- haptics: public APIs ----
    override fun haptic() = vibrate(0, false)

    override fun vibrate(index: Int, repeat: Boolean) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

    private fun telephony() =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private companion object {
        const val MCC_LENGTH = 3
        const val TABLET_SMALLEST_WIDTH_DP = 600
        const val ONE_UI_NOT_PRESENT = 0
        const val DISPLAY_TYPE_PHONE = 0
        const val DISPLAY_TYPE_TABLET = 1
    }
}
