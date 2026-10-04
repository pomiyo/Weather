package dev.local.weatherstudy.system.location

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import dev.local.weatherstudy.domain.entity.location.StudyLocationPosition
import dev.local.weatherstudy.domain.entity.location.StudyWeatherGeofence
import dev.local.weatherstudy.domain.source.location.StudyLocationProvider
import dev.local.weatherstudy.domain.source.location.StudyRepresentLocationProvider
import dev.local.weatherstudy.domain.source.location.StudyWeatherGeofenceProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 42 classes in
 * com.samsung.android.weather.system.location:
 * `LocationService`, `AbsWeatherLocationService`, `RepresentLocationService`,
 * `LocationSource`, `FusedLocationSource`, `GPSLocationSource`, `NLPLocationSource`,
 * `CriteriaLocationSource`, `SingleLocationSource`, `LastKnownLocation`,
 * `DelegationLocationSource`, `SLocationSource`, `WeatherGeofenceSource`,
 * `AndroidGeofenceSource`, `SGeoFenceSource`
 *
 * ### Eight location sources, one delegating chooser
 *
 * The original does not pick a location API — it implements **all of them** behind one
 * interface and delegates:
 *
 * ```
 * FusedLocationSource     Play Services fused provider (preferred)
 * GPSLocationSource       LocationManager GPS_PROVIDER
 * NLPLocationSource       LocationManager NETWORK_PROVIDER
 * CriteriaLocationSource  LocationManager by Criteria
 * SingleLocationSource    a one-shot request
 * LastKnownLocation       the cached fix, no request at all
 * SLocationSource         Samsung SemLocationManager          ← Samsung-only
 * DelegationLocationSource  tries the others in order
 * ```
 *
 * That is why `StudyLocationProvider` in the domain has exactly one method: everything
 * above this module sees "a position, eventually", and the fallback ladder is contained.
 *
 * The AOSP sources are reconstructed; the Samsung one is a documented stub.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyLocationSource : StudyLocationProvider {
    val sourceId: String
    fun isAvailable(): Boolean
}

/** `LastKnownLocation` — the cheapest source: a cached fix, no request. */
@Singleton
class StudyLastKnownLocationSource @Inject constructor(
    private val context: Context,
) : StudyLocationSource {

    override val sourceId = "last_known"

    override fun isAvailable(): Boolean = manager() != null

    override fun getLocation(): Flow<StudyLocationPosition> = flow {
        val manager = manager()
        val fix = manager?.let { lm ->
            PROVIDERS.firstNotNullOfOrNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
        }
        emit(
            fix?.let { StudyLocationPosition(it.latitude, it.longitude) }
                ?: StudyLocationPosition.INVALID,
        )
    }

    private fun manager() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private companion object {
        val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
}

/** `GPSLocationSource`. */
@Singleton
class StudyGpsLocationSource @Inject constructor(
    private val context: Context,
) : StudyLocationSource {
    override val sourceId = "gps"

    override fun isAvailable(): Boolean =
        (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

    override fun getLocation(): Flow<StudyLocationPosition> = flow {
        emit(StudyPlatformFix.await(context, LocationManager.GPS_PROVIDER))
    }
}

/** `NLPLocationSource` — network location. */
@Singleton
class StudyNetworkLocationSource @Inject constructor(
    private val context: Context,
) : StudyLocationSource {
    override val sourceId = "nlp"

    override fun isAvailable(): Boolean =
        (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

    override fun getLocation(): Flow<StudyLocationPosition> = flow {
        emit(StudyPlatformFix.await(context, LocationManager.NETWORK_PROVIDER))
    }
}

/**
 * `FusedLocationSource` — Play Services. The original prefers it, which is why
 * `play-services-location` is a dependency alongside the platform APIs.
 */
@Singleton
class StudyFusedLocationSource @Inject constructor(
    private val context: Context,
) : StudyLocationSource {
    override val sourceId = "fused"

    override fun isAvailable(): Boolean = true

    @android.annotation.SuppressLint("MissingPermission") // the condition chain gates on the grant
    override fun getLocation(): Flow<StudyLocationPosition> = flow {
        val position = kotlinx.coroutines.withTimeoutOrNull(StudyPlatformFix.TIMEOUT_MILLIS) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val cancellation = com.google.android.gms.tasks.CancellationTokenSource()
                com.google.android.gms.location.LocationServices
                    .getFusedLocationProviderClient(context)
                    .getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellation.token,
                    )
                    .addOnSuccessListener { fix ->
                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.success(
                                    fix?.let { StudyLocationPosition(it.latitude, it.longitude) }
                                        ?: StudyLocationPosition.INVALID,
                                ),
                            )
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(StudyLocationPosition.INVALID))
                        }
                    }
                continuation.invokeOnCancellation { cancellation.cancel() }
            }
        }
        emit(position ?: StudyLocationPosition.INVALID)
    }
}

/**
 * A single fix from one platform provider, shared by the GPS and network sources.
 *
 * Reconstruction-only helper: the original's two sources each wrap the same
 * `LocationManager` request, differing only in the provider name.
 */
internal object StudyPlatformFix {
    const val TIMEOUT_MILLIS = 15_000L

    @android.annotation.SuppressLint("MissingPermission") // the condition chain gates on the grant
    suspend fun await(context: Context, provider: String): StudyLocationPosition {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return StudyLocationPosition.INVALID
        return kotlinx.coroutines.withTimeoutOrNull(TIMEOUT_MILLIS) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val signal = android.os.CancellationSignal()
                androidx.core.location.LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    androidx.core.content.ContextCompat.getMainExecutor(context),
                ) { fix ->
                    if (continuation.isActive) {
                        continuation.resumeWith(
                            Result.success(
                                fix?.let { StudyLocationPosition(it.latitude, it.longitude) }
                                    ?: StudyLocationPosition.INVALID,
                            ),
                        )
                    }
                }
                continuation.invokeOnCancellation { signal.cancel() }
            }
        } ?: StudyLocationPosition.INVALID
    }
}

/**
 * `SLocationSource` — STUB.
 *
 * Original dependency: Samsung `SemLocationManager`. Why unavailable: firmware-only.
 * Where used: the preferred source on Samsung devices, ahead of the fused provider.
 * Replacement: reports unavailable, so [StudyDelegationLocationSource] skips it.
 */
@Singleton
class StudySamsungLocationSource @Inject constructor() : StudyLocationSource {
    override val sourceId = "samsung"
    override fun isAvailable(): Boolean = false
    override fun getLocation(): Flow<StudyLocationPosition> =
        throw dev.local.weatherstudy.system.service.samsung.StudySamsungApiUnavailable(
            "SemLocationManager",
        )
}

/**
 * `DelegationLocationSource`.
 *
 * Observed responsibility: the fallback ladder. It tries each source in preference order
 * and takes the first valid fix. The order matters — last-known is tried first because
 * it costs nothing, and a cached fix a few minutes old is good enough for weather.
 */
@Singleton
class StudyDelegationLocationSource @Inject constructor(
    private val lastKnown: StudyLastKnownLocationSource,
    private val fused: StudyFusedLocationSource,
    private val gps: StudyGpsLocationSource,
    private val network: StudyNetworkLocationSource,
    private val samsung: StudySamsungLocationSource,
) : StudyLocationProvider {

    override fun getLocation(): Flow<StudyLocationPosition> = flow {
        val ordered = listOf(samsung, lastKnown, fused, gps, network)
        for (source in ordered) {
            if (!source.isAvailable()) continue
            val position = runCatching {
                var found = StudyLocationPosition.INVALID
                source.getLocation().collect { found = it }
                found
            }.getOrDefault(StudyLocationPosition.INVALID)
            if (position.isValid) {
                emit(position)
                return@flow
            }
        }
        emit(StudyLocationPosition.INVALID)
    }
}

/**
 * `RepresentLocationService`.
 *
 * The pre-permission fallback: a region-level place the app can show before any location
 * permission is granted. It is why `StudyRepresentApi` is a separate capability.
 */
@Singleton
class StudyRepresentLocationService @Inject constructor() : StudyRepresentLocationProvider {
    override fun getRepresentLocation(): Flow<StudyLocationPosition> =
        flowOf(StudyLocationPosition.INVALID)
}

/**
 * `AndroidGeofenceSource` / `WeatherGeofenceSource`.
 *
 * The single away-mode geofence. `getFence(intent)` decodes a broadcast — that is the
 * receiver boundary, and why the domain interface has an `Intent` in its signature.
 */
@Singleton
class StudyAndroidGeofenceSource @Inject constructor(
    private val context: Context,
) : StudyWeatherGeofenceProvider {

    private var current: StudyWeatherGeofence = StudyWeatherGeofence()

    override fun updateFence(latitude: Double, longitude: Double) {
        current = current.copy(
            latitude = latitude,
            longitude = longitude,
            updateDate = System.currentTimeMillis(),
        )
    }

    override fun updateFence(geofence: StudyWeatherGeofence) {
        current = geofence.copy(updateDate = System.currentTimeMillis())
    }

    override fun removeFence() {
        current = StudyWeatherGeofence()
    }

    override fun getFence(intent: Intent): StudyWeatherGeofence = current.copy(
        transition = intent.getIntExtra(
            EXTRA_TRANSITION,
            StudyWeatherGeofence.GEOFENCE_UNKNOWN,
        ),
    )

    override fun getDwellTime(): Long = StudyWeatherGeofence.DWELL_TIME * MILLIS_PER_SECOND

    override suspend fun subscribe(onTransition: suspend (StudyWeatherGeofence) -> Unit) = Unit

    override suspend fun emit(geofence: StudyWeatherGeofence) {
        current = geofence
    }

    private companion object {
        const val EXTRA_TRANSITION = "geofence_transition"
        const val MILLIS_PER_SECOND = 1000L
    }
}

/**
 * `SGeoFenceSource` — STUB.
 *
 * Original dependency: Samsung's geofence service, which supports more simultaneous
 * fences and a native dwell transition. Replacement: the AOSP source above.
 */
@Singleton
class StudySamsungGeofenceSource @Inject constructor() {
    fun isAvailable(): Boolean = false
}
