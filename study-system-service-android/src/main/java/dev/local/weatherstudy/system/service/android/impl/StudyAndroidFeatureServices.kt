package dev.local.weatherstudy.system.service.android.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.local.weatherstudy.system.service.StudyConnectivityService
import dev.local.weatherstudy.system.service.StudyCscFeature
import dev.local.weatherstudy.system.service.StudyDesktopService
import dev.local.weatherstudy.system.service.StudyDeviceService
import dev.local.weatherstudy.system.service.StudyFloatingFeature
import dev.local.weatherstudy.system.service.StudyLocaleService
import dev.local.weatherstudy.system.service.StudySensitiveService
import dev.local.weatherstudy.system.service.StudyTelephonyService
import java.util.Locale
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the feature-flag implementations in
 * com.samsung.android.weather.system.service.android.impl:
 * AndroidCscFeature, AndroidFloatingFeature, AndroidConnectivityService,
 * AndroidLocaleService, AndroidTelephonyService, AndroidSensitiveService,
 * AndroidDesktopService
 *
 * Observed responsibility: supply plausible defaults where the Samsung firmware would
 * supply a real answer. Note [StudyAndroidCscFeature] does not read anything — it
 * returns build-time defaults, which is the honest AOSP answer, and is why a
 * non-Samsung build gets Celsius and a 3-hour refresh regardless of carrier.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyAndroidCscFeature @Inject constructor() : StudyCscFeature {
    override fun getTemperatureUnit(): Int = TEMP_UNIT_CELSIUS
    override fun getDefaultAutoRefreshInterval(): Int = DEFAULT_REFRESH_HOURS
    override fun enableScreenOnRefresh(): Boolean = false
    override fun isSupportMinimizedSIP(): Boolean = false
    override fun isVerizon(): Boolean = false
    override fun isTaiwan(): Boolean = false
    override fun isHongKong(): Boolean = false
    override fun isMEA(): Boolean = false

    private companion object {
        const val TEMP_UNIT_CELSIUS = 0
        const val DEFAULT_REFRESH_HOURS = 3
    }
}

/** Corresponds conceptually to `…android.impl.AndroidFloatingFeature`. */
class StudyAndroidFloatingFeature @Inject constructor() : StudyFloatingFeature {
    override fun isFoldDevice(): Boolean = false
    override fun isFlipDevice(): Boolean = false
    override fun getAodFeature(): Boolean = false
}

/** Corresponds conceptually to `…android.impl.AndroidConnectivityService`. */
class StudyAndroidConnectivityService @Inject constructor(
    private val context: Context,
) : StudyConnectivityService {

    override fun checkNetworkConnected(): Boolean {
        val cm = manager() ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun checkBackgroundRestricted(): Int {
        val cm = manager() ?: return StudyConnectivityService.BACKGROUND_UNKNOWN
        return when (cm.restrictBackgroundStatus) {
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED ->
                StudyConnectivityService.BACKGROUND_RESTRICTED
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED,
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED,
            -> StudyConnectivityService.BACKGROUND_ALLOWED
            else -> StudyConnectivityService.BACKGROUND_UNKNOWN
        }
    }

    override fun hasTransport(transport: Int): Boolean {
        val cm = manager() ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(transport)
    }

    private fun manager() =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
}

/** Corresponds conceptually to `…android.impl.AndroidLocaleService`. */
class StudyAndroidLocaleService @Inject constructor(
    private val context: Context,
) : StudyLocaleService {
    override fun getLocale(): Locale = context.resources.configuration.locales[0]
    override fun getLanguage(): String = getLocale().language
    override fun isAmPmBeforeTime(): Boolean = getLocale().language in AM_PM_FIRST_LANGUAGES
    override fun isRtl(): Boolean =
        android.text.TextUtils.getLayoutDirectionFromLocale(getLocale()) ==
            android.view.View.LAYOUT_DIRECTION_RTL

    private companion object {
        /** languages that write the meridiem before the clock time */
        val AM_PM_FIRST_LANGUAGES = setOf("ko", "zh", "ja")
    }
}

/** Corresponds conceptually to `…android.impl.AndroidTelephonyService`. */
class StudyAndroidTelephonyService @Inject constructor(
    private val context: Context,
) : StudyTelephonyService {
    private fun tm() = context.getSystemService(Context.TELEPHONY_SERVICE)
        as? android.telephony.TelephonyManager

    override fun getCountryCode(): String = tm()?.simCountryIso?.uppercase().orEmpty()
    override fun getNetworkCountryCode(): String = tm()?.networkCountryIso?.uppercase().orEmpty()
    override fun getMcc(): String = tm()?.networkOperator?.take(3).orEmpty()
    override fun getMnc(): String = tm()?.networkOperator?.drop(3).orEmpty()
    override fun getSimState(): Int = tm()?.simState ?: 0
    override fun getDataState(): Int = 0
    override fun isSimEnabled(): Boolean =
        tm()?.simState == android.telephony.TelephonyManager.SIM_STATE_READY
    override fun isNetworkRoaming(): Boolean = tm()?.isNetworkRoaming ?: false
}

/**
 * Corresponds conceptually to `…android.impl.AndroidSensitiveService`.
 *
 * The original reads the IMEI through a privileged Samsung path. Reading it at all has
 * required a privileged permission since Android 10, so the AOSP implementation returns
 * empty — and the reconstruction never asks for a device identifier.
 */
class StudyAndroidSensitiveService @Inject constructor() : StudySensitiveService {
    override fun getImei(): String = ""
}

/** Corresponds conceptually to `…android.impl.AndroidDesktopService`. */
class StudyAndroidDesktopService @Inject constructor() : StudyDesktopService {
    override fun isDesktopMode(floatingFeature: StudyFloatingFeature): Boolean = false
    override fun isStandaloneMode(
        floatingFeature: StudyFloatingFeature,
        device: StudyDeviceService,
    ): Boolean = false
}
