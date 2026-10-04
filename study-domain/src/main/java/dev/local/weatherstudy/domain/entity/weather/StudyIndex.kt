package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.type.StudyIndexCategory
import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.domain.type.StudyIndexType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Index
 *
 * Observed responsibilities:
 * - one life index / measurement row (UV, humidity, wind, AQI, pollen, …)
 * - `type` selects which detail card and which Canvas graph renders it,
 *   `category` groups it, `level`/`levelText` carry the banded severity
 * - `priority` orders the index card's inner RecyclerView
 *
 * `type`, `category` and `level` vocabularies live in [StudyIndexType],
 * [StudyIndexCategory] and [StudyIndexLevel], exactly as the original keeps them in
 * its `domain.type` package rather than on the entity.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyIndex(
    val type: Int = StudyIndexType.NONE,
    val category: Int = StudyIndexCategory.ALL,
    val value: Double = INVALID_VALUE,
    val level: Int = StudyIndexLevel.NONE,
    val levelText: String = "",
    val unit: String = "",
    val description: String = "",
    val extra: String = "",
    val priority: Int = 0,
    val webUrl: String = "",
) {
    companion object {
        const val INVALID_VALUE = -999.0
    }
}
