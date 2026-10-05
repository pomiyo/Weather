package dev.local.weatherstudy.app.common.resource

/**
 * Educational reconstruction of
 * `com.sec.android.daemonapp.app.detail.state.converter.DetailIllustrationStateConverter`.
 *
 * Original APK evidence:
 * - a thirty-entry list of `DetailIllustrationState` subclasses, each built from an asset
 *   path plus an intrinsic width and height, e.g.
 *   `DetailIllustrationState.Sunny("illust/sunny.json", 276, 640)`
 * - a second pass that swaps seven of them for `*_below_zero` variants when the temperature
 *   is below freezing
 * - consumed through `DetailTopInfoState.illustrationState` and rendered by the
 *   `@id/icon_illust` `LottieAnimationView` in `fragment_detail_viewpager_item.xml`
 *
 * ### This is the screen's most recognisable element
 *
 * `assets/illust/` is 6.3 MB across 37 animations — by far the largest art asset in the
 * APK, and the figure-with-weather scene that occupies the top right of the detail screen.
 * The reconstruction had no counterpart at all, which is most of why its main screen read
 * as generic.
 *
 * ### Why each entry carries its own size
 *
 * The declared sizes vary enormously — cloud.json is 143x484, sunny.json is 276x640 —
 * because each composition is cropped to its own content, and all of them are rendered into
 * one fixed 360x260dp view.
 *
 * Lottie letterboxes from the composition's own bounds, so neither the original nor this
 * reconstruction actually needs the numbers to render correctly; [StudyIllustration.aspectRatio]
 * is carried because the original carries it, and is currently unread. It is recorded here
 * rather than dropped because it is the evidence that the view's fixed size is a deliberate
 * crop box rather than the artwork's own size — which is the thing worth understanding.
 *
 * ### The below-zero variants
 *
 * Seven conditions have a cold-weather repaint, applied by temperature rather than by
 * condition code: sunny, clear, mostly sunny, partly cloudy, partly cloudy night, mostly
 * cloudy and mostly cloudy night. The remaining twenty-three are already cold-looking or
 * unaffected. The original applies this as a post-pass over the resolved state rather than
 * as extra switch arms, and that structure is kept here.
 *
 * ### Two conditions share one file
 *
 * Codes 15 (ice) and 17 (cold) both resolve to `illust/cold.json`. That is the original's
 * mapping, not an omission here.
 *
 * LOCAL STUDY RESOURCE — the JSON animations are extracted from a personally owned device
 * and are git-ignored. DO NOT REDISTRIBUTE. [resolve] returns a descriptor whatever
 * happens; the caller checks [StudyIllustration.isAvailable] before asking Lottie to load
 * it, so a tree without the assets renders no illustration instead of crashing.
 *
 * This implementation was independently reconstructed for local study.
 */
object StudyIllustrationProvider {

    /**
     * One illustration: its asset path and the intrinsic size of the composition.
     *
     * [isAvailable] is false in a clone that does not carry the local study assets.
     */
    data class StudyIllustration(
        val assetPath: String,
        val intrinsicWidth: Int,
        val intrinsicHeight: Int,
        val isAvailable: Boolean = true,
    ) {
        val aspectRatio: Float
            get() = if (intrinsicHeight == 0) 1f else intrinsicWidth.toFloat() / intrinsicHeight
    }

    /** `DetailIllustrationStateConverter`'s thirty-entry list, in code order. */
    private val byIconNum: Map<Int, StudyIllustration> = mapOf(
        0 to StudyIllustration("illust/sunny.json", 276, 640),
        1 to StudyIllustration("illust/clear.json", 307, 629),
        2 to StudyIllustration("illust/partly_cloud.json", 275, 639),
        3 to StudyIllustration("illust/partly_cloud_night.json", 307, 629),
        4 to StudyIllustration("illust/cloud.json", 143, 484),
        5 to StudyIllustration("illust/fog.json", 193, 389),
        6 to StudyIllustration("illust/rain.json", 160, 340),
        7 to StudyIllustration("illust/shower.json", 160, 340),
        8 to StudyIllustration("illust/partly_sunny_with_shower.json", 160, 340),
        9 to StudyIllustration("illust/thunderstorm.json", 191, 340),
        10 to StudyIllustration("illust/partly_sunny_with_thunder.json", 191, 340),
        11 to StudyIllustration("illust/light_snow.json", 299, 479),
        12 to StudyIllustration("illust/partly_sunny_with_flurries.json", 298, 479),
        13 to StudyIllustration("illust/snow.json", 202, 329),
        14 to StudyIllustration("illust/rain_and_snow.json", 298, 479),
        // 15 (ice) and 17 (cold) share one composition — the original's mapping
        15 to StudyIllustration("illust/cold.json", 289, 468),
        16 to StudyIllustration("illust/hot.json", 131, 260),
        17 to StudyIllustration("illust/cold.json", 289, 468),
        18 to StudyIllustration("illust/wind.json", 193, 389),
        19 to StudyIllustration("illust/rain_and_thunder.json", 191, 340),
        20 to StudyIllustration("illust/heavy_rain.json", 160, 340),
        21 to StudyIllustration("illust/sandstorm.json", 193, 389),
        22 to StudyIllustration("illust/hurricane.json", 193, 389),
        23 to StudyIllustration("illust/mostly_sunny.json", 275, 639),
        24 to StudyIllustration("illust/mostly_clear.json", 307, 629),
        25 to StudyIllustration("illust/mostly_cloudy.json", 275, 639),
        26 to StudyIllustration("illust/mostly_cloudy_night.json", 307, 629),
        27 to StudyIllustration("illust/heavy_snow.json", 201, 328),
        28 to StudyIllustration("illust/rain_and_sleet.json", 298, 479),
        29 to StudyIllustration("illust/hail.json", 298, 479),
    )

    /** The seven conditions with a cold repaint, applied as a post-pass by temperature. */
    private val belowZeroByAssetPath: Map<String, StudyIllustration> = mapOf(
        "illust/sunny.json" to StudyIllustration("illust/sunny_below_zero.json", 276, 640),
        "illust/clear.json" to StudyIllustration("illust/clear_below_zero.json", 307, 629),
        "illust/mostly_sunny.json" to
            StudyIllustration("illust/mostly_sunny_below_zero.json", 275, 639),
        "illust/partly_cloud.json" to
            StudyIllustration("illust/partly_cloud_below_zero.json", 275, 639),
        "illust/partly_cloud_night.json" to
            StudyIllustration("illust/partly_cloud_night_below_zero.json", 307, 629),
        "illust/mostly_cloudy.json" to
            StudyIllustration("illust/mostly_cloudy_below_zero.json", 275, 639),
        "illust/mostly_cloudy_night.json" to
            StudyIllustration("illust/mostly_cloudy_night_below_zero.json", 307, 629),
    )

    /** The original's fallback when the code is not in the table. */
    private val default = StudyIllustration("illust/sunny.json", 276, 640)

    /**
     * @param iconNum the thirty-value condition code, as produced by `StudyWeatherIcons`
     * @param temperatureCelsius used only for the below-zero post-pass
     */
    fun resolve(iconNum: Int, temperatureCelsius: Double): StudyIllustration {
        val base = byIconNum[iconNum] ?: default
        if (temperatureCelsius >= FREEZING_POINT_CELSIUS) return base
        return belowZeroByAssetPath[base.assetPath] ?: base
    }

    private const val FREEZING_POINT_CELSIUS = 0.0
}
