package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.type.StudyLocationsLabelType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Location
 *
 * Observed responsibilities:
 * - identifies one saved place; `key` is the provider's location key and the
 *   primary key used everywhere downstream (COL_WEATHER_KEY in Room)
 * - `oldKey` exists so a provider change can migrate saved locations
 * - `priority` drives list order; `CURRENT_LOCATION_PRIORITY` marks the
 *   device-location entry, which the original keeps at the top of the list
 * - `isDisputedArea` suppresses the country name in territory disputes
 * - `label` / `labelType` back the user-assigned nickname feature
 *
 * 16 constructor components, matching the original's @Metadata.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyLocation(
    val key: String,
    val id: String = "",
    val oldKey: String = "",
    val priority: Int = DEFAULT_PRIORITY,
    val latitude: Double = INVALID_COORDINATE,
    val longitude: Double = INVALID_COORDINATE,
    val cityName: String = "",
    val stateName: String = "",
    val countryName: String = "",
    val countryCode: String = "",
    val postalCode: String = "",
    val shortAddress: String = "",
    val isDisputedArea: Boolean = false,
    val updateTime: Long = 0L,
    val label: String = "",
    val labelType: Int = StudyLocationsLabelType.NONE,
) {
    companion object {
        const val CURRENT_LOCATION_PRIORITY = 0
        const val DEFAULT_PRIORITY = -1
        const val INVALID_COORDINATE = 0.0

    }
}

/** Reconstruction of `LocationKt`: the "is this the device's location" test. */
val StudyLocation.isCurrentLocation: Boolean
    get() = priority == StudyLocation.CURRENT_LOCATION_PRIORITY

/** Reconstruction of `LocationKt`: the display name the UI actually shows. */
val StudyLocation.displayName: String
    get() = when {
        label.isNotEmpty() -> label
        cityName.isNotEmpty() -> cityName
        else -> shortAddress
    }
