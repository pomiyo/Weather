package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Alert
 *
 * Observed responsibilities:
 * - one severe-weather advisory, rendered by the Alert detail card and by
 *   `AlertNotificationView`
 * - `detailKey` is part of the Room composite primary key
 *   (COL_WEATHER_KEY + COL_ALERT_DETAIL_KEY), so one location can hold many alerts
 * - `severityCode` orders them; `significance` distinguishes warning/advisory/watch
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyAlert(
    val detailKey: String,
    val eventDescription: String = "",
    val phenomena: String = "",
    val severityCode: Int = SEVERITY_UNKNOWN,
    val significance: String = "",
    val issueTime: Long = 0L,
    val issueTimeZone: String = "",
    val expireTime: Long = 0L,
    val linkURL: String = "",
) {
    companion object {
        const val SEVERITY_UNKNOWN = -1
        const val SEVERITY_EXTREME = 1
        const val SEVERITY_SEVERE = 2
        const val SEVERITY_MODERATE = 3
        const val SEVERITY_MINOR = 4
    }
}
