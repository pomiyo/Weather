package dev.local.weatherstudy.domain.source.location

import android.content.Intent
import dev.local.weatherstudy.domain.entity.location.StudyLocationPosition
import dev.local.weatherstudy.domain.entity.location.StudyWeatherGeofence
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.location.LocationProvider
 *
 * Observed responsibility: the domain's only view of "where is the device". Every
 * concrete source in `:study-system-location` (fused, GPS, network, criteria,
 * last-known, and the Samsung one) implements this, and the delegation source picks
 * between them. The domain never names a platform API.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyLocationProvider {
    fun getLocation(): Flow<StudyLocationPosition>
}

/** Corresponds conceptually to `…source.location.RepresentLocationProvider`. */
interface StudyRepresentLocationProvider {
    fun getRepresentLocation(): Flow<StudyLocationPosition>
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.location.WeatherGeofenceProvider
 *
 * Observed responsibility: registration and transition decoding for the single
 * away-mode geofence. [getFence] parses a broadcast Intent — that is the receiver
 * boundary, and it is why the domain interface has an `Intent` in its signature
 * (the original does the same rather than introducing a transport type).
 */
interface StudyWeatherGeofenceProvider {
    fun updateFence(latitude: Double, longitude: Double)
    fun updateFence(geofence: StudyWeatherGeofence)
    fun removeFence()
    fun getFence(intent: Intent): StudyWeatherGeofence
    fun getDwellTime(): Long
    suspend fun subscribe(onTransition: suspend (StudyWeatherGeofence) -> Unit)
    suspend fun emit(geofence: StudyWeatherGeofence)
}

/** Corresponds conceptually to `…source.location.GeofenceDataSource`. */
interface StudyGeofenceDataSource {
    suspend fun getFence(): StudyWeatherGeofence
    suspend fun updateFence(latitude: Double, longitude: Double)
    suspend fun updateFence(geofence: StudyWeatherGeofence)
}
