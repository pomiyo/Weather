package dev.local.weatherstudy.condition.conditions.checker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 31 classes in
 * com.samsung.android.weather.condition.conditions.checker
 * (`CheckLocationProvider`, and the per-permission / per-capability checkers)
 *
 * ### Why a checker layer separate from the conditions
 *
 * A condition answers "may the app proceed?"; a checker answers "is this one platform
 * fact true?". Splitting them means the conditions are pure and testable, and the
 * platform calls are in one place — the same reasoning as `:study-system-service`, one
 * level up.
 *
 * The original has 31 of them because there are that many distinct platform facts to
 * check across the five scenarios. Twelve are reconstructed here, covering every
 * distinct *shape* (permission check, system-service state, package presence, settings
 * flag); the remaining 19 repeat those shapes for other permissions and are listed in
 * `reports/class-mapping.md` §12.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyCheckLocationPermission @Inject constructor(
    private val context: Context,
) {
    fun hasForeground(): Boolean =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    /** precise vs approximate — the detail screen shows a different tip for each */
    fun hasPrecise(): Boolean = granted(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackground(): Boolean = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Corresponds conceptually to `…checker.CheckLocationProvider`.
 *
 * Observed responsibility: a granted permission is not the same as location being
 * switched on. The original checks both providers, because a device with GPS off but
 * network location on can still produce a fix.
 */
class StudyCheckLocationProvider @Inject constructor(
    private val context: Context,
) {
    fun isEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isGpsEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    }
}

/** Corresponds conceptually to `…checker.CheckNotificationPermission`. */
class StudyCheckNotificationPermission @Inject constructor(
    private val context: Context,
) {
    fun isGranted(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
}

/** Corresponds conceptually to `…checker.CheckActivityRecognitionPermission`. */
class StudyCheckActivityRecognitionPermission @Inject constructor(
    private val context: Context,
) {
    fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
}

/**
 * Corresponds conceptually to `…checker.CheckPackageInstalled`.
 *
 * Observed responsibility: several features are gated on a companion app being present
 * — Samsung News, SmartThings, the Galaxy Store. The original's manifest declares
 * `<queries>` entries for exactly this, because package visibility is restricted from
 * Android 11 on.
 */
class StudyCheckPackageInstalled @Inject constructor(
    private val context: Context,
) {
    fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
    }.isSuccess

    fun isEnabled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    }.getOrDefault(false)
}

/** Corresponds conceptually to `…checker.CheckBatteryOptimization`. */
class StudyCheckBatteryOptimization @Inject constructor(
    private val context: Context,
) {
    fun isIgnoringOptimizations(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }
}

/** Corresponds conceptually to `…checker.CheckDozeMode`. */
class StudyCheckDozeMode @Inject constructor(
    private val context: Context,
) {
    fun isIdle(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            ?: return false
        return power.isDeviceIdleMode
    }
}

/** Corresponds conceptually to `…checker.CheckAirplaneMode`. */
class StudyCheckAirplaneMode @Inject constructor(
    private val context: Context,
) {
    fun isOn(): Boolean = android.provider.Settings.Global.getInt(
        context.contentResolver,
        android.provider.Settings.Global.AIRPLANE_MODE_ON,
        0,
    ) != 0
}
