package dev.local.weatherstudy.domain.entity.content

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.type.StudyLifeStyleIntervalType
import dev.local.weatherstudy.domain.type.StudyLifeStyleStateType
import dev.local.weatherstudy.domain.type.StudyLifeStyleType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.content.LifeStyleContent
 *
 * Observed responsibilities:
 * - one "life style" row — golf / running / drying laundry / car wash suitability
 * - `type` selects the row; which types are visible is a USER SETTING, stored in
 *   its own table (TABLE_LIFESTYLE_SETTINGS_INFO) and edited by
 *   `LifeStyleSettingsFragment`. The content and the visibility are therefore two
 *   separate repositories in the original — preserved here.
 * - `statesByTime` backs the `detail_life_style_inner_state_by_time_item` layout:
 *   a single row can expand into a per-time-slot strip
 * - `type`, `stateType` and `intervalType` vocabularies live in [StudyLifeStyleType],
 *   [StudyLifeStyleStateType] and [StudyLifeStyleIntervalType], as in the original
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyLifeStyleContent(
    val type: Int = StudyLifeStyleType.RUNNING,
    val titleText: String = "",
    val stateType: Int = StudyLifeStyleStateType.FAIR_OF_3LEVELS,
    val stateText: String = "",
    val descriptionText: String = "",
    val intervalType: Int = StudyLifeStyleIntervalType.DAY,
    val url: String = "",
    val statesByTime: List<StudyLifeStyleContentByTime> = emptyList(),
)

/** Corresponds conceptually to `LifeStyleContent.LifeStyleContentByTime`. */
@JsonClass(generateAdapter = true)
data class StudyLifeStyleContentByTime(
    val epochTime: Long,
    val stateType: Int = StudyLifeStyleStateType.FAIR_OF_3LEVELS,
    val stateText: String = "",
)

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.LifeStyleSettingsEntity's
 * domain view — which life style rows the user has enabled.
 */
data class StudyLifeStyleSettings(
    val type: Int,
    val enabled: Boolean,
)
