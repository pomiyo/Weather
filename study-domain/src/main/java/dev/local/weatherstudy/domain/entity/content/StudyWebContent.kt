package dev.local.weatherstudy.domain.entity.content

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.type.StudyContentType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.content.WebContent
 *
 * Observed responsibilities:
 * - one externally-hosted item: a news story, a video, a today-story, or the radar
 *   page. All four detail cards (News, Video, TodayStoriesAndVideo, Radar) are fed
 *   by lists of this single type, differentiated by `type`.
 * - `expiredTime` lets the content refresh cycle (`ReachToContentRefreshTime`)
 *   run on a different clock from the forecast refresh cycle
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyWebContent(
    val id: String,
    val type: Int = StudyContentType.NONE,
    val order: Int = 0,
    val title: String = "",
    val summary: String = "",
    val narrative: String = "",
    val image: String = "",
    val url: String = "",
    val home: String = "",
    val expiredTime: Long = 0L,
)

/**
 * Corresponds conceptually to `…entity.content.WebMenu`.
 *
 * Observed responsibility: a provider-supplied link row shown at the bottom of the
 * detail screen ("More on weather.com"). Persisted in TABLE_WEB_MENU_INFO with a
 * composite key of (weatherKey, title, url) — i.e. the provider, not the app,
 * decides how many there are.
 */
@JsonClass(generateAdapter = true)
data class StudyWebMenu(
    val title: String,
    val url: String,
    val type: Int = TYPE_LINK,
    val image: String = "",
    val updateTime: Long = 0L,
) {
    companion object {
        const val TYPE_LINK = 0
        const val TYPE_PROVIDER_HOME = 1
        const val TYPE_PRIVACY = 2
    }
}
