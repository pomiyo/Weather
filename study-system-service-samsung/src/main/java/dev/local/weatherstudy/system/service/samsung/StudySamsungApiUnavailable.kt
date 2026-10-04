package dev.local.weatherstudy.system.service.samsung

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to: nothing in the original — this type exists only in the
 * reconstruction.
 *
 * ### Why
 *
 * `com.samsung.android.weather.system.service.sep.impl` contains 21 classes that call
 * Samsung firmware APIs: `SemCscFeature`, `SemFloatingFeature`, `SemWindowManager`,
 * `SemView`, `SemListView`, `SemRemoteViews`, `SemSmartTipManager`,
 * `SemTelephonyManager`, `SemLocationManager`, and the `sem*` extension methods on
 * ordinary framework classes. None of those classes exist on a stock Android build, and
 * they cannot be legitimately supplied — they live in the device's `framework.jar`.
 *
 * Rather than delete the Samsung family (which would hide where the dependency is), the
 * reconstruction keeps all 21 classes and has each unavailable method throw this. The
 * module therefore still *documents* the seam: open `:study-system-service-samsung` in
 * Android Studio and every Samsung platform dependency in the app is one file list.
 *
 * Per STEP 23, nothing here attempts to spoof a platform signature or reach a
 * privileged service — the stubs fail loudly and name what is missing.
 */
class StudySamsungApiUnavailable(api: String, reason: String = DEFAULT_REASON) :
    UnsupportedOperationException("$api is unavailable: $reason") {

    companion object {
        const val DEFAULT_REASON =
            "the original component depends on a Samsung framework class that is " +
                "present only in Samsung firmware; see reports/samsung-bridge-map.md"
    }
}
