package dev.local.weatherstudy.domain.entity.location

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.location.WeatherGeofence
 *
 * Observed responsibilities:
 * - the single registered geofence around the user's "home" area, used by the
 *   away-mode feature. There is exactly one, re-centred as the user moves —
 *   `GeofenceCalibrationWorker` is what re-registers it.
 * - `speed` is carried with the fix because the original suppresses a transition
 *   when the device is moving fast (passing through, not arriving)
 * - the DWELL transition (not just ENTER/EXIT) is why `CheckGeofenceDwell` exists:
 *   away mode only engages after the device has settled for [DWELL_TIME] seconds
 *
 * Constants are the original's values.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyWeatherGeofence(
    val key: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Int = DEFAULT_RADIUS,
    val transition: Int = GEOFENCE_UNKNOWN,
    val speed: Float = 0f,
    val updateDate: Long = 0L,
) {
    companion object {
        const val GEOFENCE_UNKNOWN = 0
        const val GEOFENCE_ENTER = 1
        const val GEOFENCE_EXIT = 2
        const val GEOFENCE_DWELL = 3

        const val MIN_RADIUS = 200
        const val DEFAULT_RADIUS = 1000
        const val MAX_RADIUS = 10000

        /** seconds the device must dwell before away mode engages */
        const val DWELL_TIME = 60
    }
}

/** Reconstruction of `WeatherGeofenceKt`. */
val StudyWeatherGeofence.isValid: Boolean
    get() = latitude != 0.0 && longitude != 0.0 && radius in StudyWeatherGeofence.MIN_RADIUS..StudyWeatherGeofence.MAX_RADIUS

/**
 * Corresponds conceptually to `…entity.location.LocationPosition` — a bare coordinate
 * pair, kept distinct from [dev.local.weatherstudy.domain.entity.weather.StudyLocation]
 * because the location subsystem deals in positions before any place is resolved.
 */
data class StudyLocationPosition(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        val INVALID = StudyLocationPosition(0.0, 0.0)
    }

    val isValid: Boolean get() = latitude != 0.0 || longitude != 0.0
}
