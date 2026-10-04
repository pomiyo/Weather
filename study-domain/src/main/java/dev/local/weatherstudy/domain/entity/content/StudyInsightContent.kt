package dev.local.weatherstudy.domain.entity.content

import com.squareup.moshi.JsonClass
import dev.local.weatherstudy.domain.type.StudyInsightType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.content.InsightContent
 *
 * Observed responsibilities:
 * - one "insight" — the narrative cards at the top of the detail screen
 *   ("rain starting in 20 minutes", "10° colder than yesterday", …)
 * - the SAME record decides whether the insight appears on the detail screen,
 *   in a widget, and in a notification — the three `show*` flags. That is why
 *   `InsightApi` output is persisted (TABLE_INSIGHT_CONTENT_INFO) rather than
 *   recomputed per surface.
 * - `order` is part of the Room composite key (COL_WEATHER_KEY + COL_INSIGHT_ORDER)
 *   and also the display order inside the insight card's pager
 * - the payload is a [Card] whose [AdditionalInfo] is a sealed hierarchy with
 *   16 variants — one per insight kind. The detail card renders a different inner
 *   layout per variant, which is why the original models it as a type hierarchy
 *   rather than a tagged bag of strings.
 * - the `insightType` vocabulary lives in [StudyInsightType], as in the original.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyInsightContent(
    val insightType: Int = StudyInsightType.NONE,
    val order: Int,
    val card: StudyInsightCard,
    val showDefault: Boolean = false,
    val showDetail: Boolean = false,
    val showWidget: Boolean = false,
    val showNotification: Boolean = false,
    val expireTime: Long = Long.MAX_VALUE,
)

/**
 * Corresponds conceptually to `InsightContent.Card`.
 *
 * Observed responsibility: the renderable text of an insight at three lengths —
 * `shortContent` for widgets and complications, `content` for the detail card,
 * `defaultContent` as the fallback when the provider sends nothing.
 */
@JsonClass(generateAdapter = true)
data class StudyInsightCard(
    val title: String = "",
    val content: String = "",
    val shortContent: String = "",
    val defaultContent: String = "",
    val timeDescription: String = "",
    val url: String = "",
    val additionalInfo: StudyAdditionalInfo = StudyAdditionalInfo.Empty,
)

/**
 * Corresponds conceptually to `InsightContent.AdditionalInfo` and its 16 subclasses.
 *
 * Observed responsibility: the typed payload behind one insight kind. Reconstructed
 * as a Kotlin `sealed interface` — the original is an open base class with
 * `data class` subclasses, which is the same dispatch with a pre-sealed-interface idiom.
 */
sealed interface StudyAdditionalInfo {

    /** `InsightContent.EmptyAdditionalInfo` */
    data object Empty : StudyAdditionalInfo

    /** `InsightContent.ShortTermPrecipitation` */
    data class ShortTermPrecipitation(
        val startEpoch: Long,
        val endEpoch: Long,
        val precipitationType: Int,
        val amount: Double,
    ) : StudyAdditionalInfo

    /** `InsightContent.ChanceOfPrecipitation` */
    data class ChanceOfPrecipitation(
        val probability: Int,
        val startEpoch: Long,
    ) : StudyAdditionalInfo

    /** `InsightContent.TemperatureChange` */
    data class TemperatureChange(
        val todayTemp: Double,
        val yesterdayTemp: Double,
        val delta: Double,
    ) : StudyAdditionalInfo

    /** `InsightContent.RecordTemperature` */
    data class RecordTemperature(
        val temperature: Double,
        val recordYear: Int,
        val isHigh: Boolean,
    ) : StudyAdditionalInfo

    /** `InsightContent.FeelsLike` */
    data class FeelsLike(
        val temperature: Double,
        val feelsLikeTemp: Double,
    ) : StudyAdditionalInfo

    /** `InsightContent.AirQuality` */
    data class AirQuality(val index: Int, val level: Int, val levelText: String) : StudyAdditionalInfo

    /** `InsightContent.FineDust` */
    data class FineDust(val value: Double, val level: Int, val levelText: String) : StudyAdditionalInfo

    /** `InsightContent.UltraFineDust` */
    data class UltraFineDust(val value: Double, val level: Int, val levelText: String) : StudyAdditionalInfo

    /** `InsightContent.UV` */
    data class UV(val index: Int, val level: Int, val levelText: String) : StudyAdditionalInfo

    /** `InsightContent.Wind` */
    data class Wind(val speed: Double, val direction: Int, val unit: String) : StudyAdditionalInfo

    /** `InsightContent.SnowFall` */
    data class SnowFall(val amount: Double, val unit: String, val startEpoch: Long) : StudyAdditionalInfo

    /** `InsightContent.ThunderstormImpact` */
    data class ThunderstormImpact(val severity: Int, val startEpoch: Long, val endEpoch: Long) : StudyAdditionalInfo

    /** `InsightContent.PollenChange` */
    data class PollenChange(val level: Int, val levelText: String, val category: String) : StudyAdditionalInfo

    /** `InsightContent.SunRiseSunSet` */
    data class SunRiseSunSet(val sunRiseTime: Long, val sunSetTime: Long, val isSunrise: Boolean) : StudyAdditionalInfo

    /** `InsightContent.TomorrowComment` */
    data class TomorrowComment(val maxTemp: Double, val minTemp: Double, val iconNum: Int) : StudyAdditionalInfo
}
