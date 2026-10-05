package dev.local.weatherstudy.app.common.resource

import androidx.annotation.DrawableRes
import dev.local.weatherstudy.app.R

/**
 * Educational reconstruction of
 * `com.samsung.android.weather.app.common.resource.BackgroundProvider`.
 *
 * Original APK evidence:
 * - a Kotlin `object` holding eleven `DetailBackgroundState` singletons, one per artwork
 * - each pairs an `R.drawable.detail_bg_gradient_*` with an internal broadcast action
 *   (`com.samsung.android.weather.intent.action.internal.DETAIL.<NAME>`)
 * - `getBackground(iconNum, isDay)` dispatches to `toDayBackground` / `toNightBackground`,
 *   each a `switch` over the thirty icon codes
 *
 * ### Thirty conditions, eleven backgrounds
 *
 * The mapping is deliberately lossy: thirty weather codes collapse onto eleven pieces of
 * artwork, and the collapse is **not** the same by day and by night. Code 2 (partly sunny)
 * takes the plain Sunny background by day but the SunnyNight one after dark; codes 6 and 7
 * (rain, showers) take Rain by day and PartlySunnyNight at night, because the night artwork
 * carries its own cloud layer and a separate rain variant would read as nearly black.
 *
 * Both switches have a `default` arm, and in each case the default is the same value as the
 * clear-sky arm — so an unrecognised code degrades to plain sky rather than to nothing.
 * That is reproduced here with an elvis onto the same constant.
 *
 * ### Sunrise and Sunset are not in either table
 *
 * The original exposes `BACKGROUND_SUNRISE` and `BACKGROUND_SUNSET` through getters but
 * never returns them from `getBackground`. They are selected by the caller from the time of
 * day, not from the condition code, which is why they sit outside the switch. The same
 * shape is kept here: [sunrise] and [sunset] are public, and [getBackground] cannot return
 * them.
 *
 * ### The artwork itself
 *
 * `detail_bg_gradient_*.png` are painted 900x900 images (sandstorm is 3600x3600), 174-553 KB
 * each — not two-stop gradients, despite the name. The reconstruction previously used XML
 * `<gradient>` drawables here, which is why its background read as flat.
 *
 * LOCAL STUDY RESOURCE — the PNGs are extracted from a personally owned device and are
 * git-ignored. DO NOT REDISTRIBUTE. When they are absent [getBackground] still returns a
 * valid state and [StudyDetailBackgroundArtwork.resId] resolves to 0, which callers treat
 * as "draw the fallback colour".
 *
 * This implementation was independently reconstructed for local study.
 */
object StudyBackgroundProvider {

    /**
     * One background: the artwork plus the internal action the original broadcasts when it
     * changes, so the always-on display and the edge panel can follow the detail screen.
     */
    data class StudyDetailBackgroundArtwork(
        @DrawableRes val resId: Int,
        val action: String,
        val fallbackColor: Int,
    )

    private const val ACTION_PREFIX = "dev.local.weatherstudy.intent.action.internal.DETAIL."

    val sunny = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_sunny, ACTION_PREFIX + "SUNNY", 0xFF5598E3.toInt(),
    )
    val rain = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_rain, ACTION_PREFIX + "RAIN", 0xFF4198C3.toInt(),
    )
    val cold = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_cold, ACTION_PREFIX + "COLD", 0xFFBEC6D7.toInt(),
    )
    val cloudy = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_cloudy, ACTION_PREFIX + "CLOUDY", 0xFF385E9A.toInt(),
    )
    val thunderstorm = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_thunderstorm,
        ACTION_PREFIX + "THUNDERSTORM",
        0xFF174367.toInt(),
    )
    val hot = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_hot, ACTION_PREFIX + "HOT", 0xFFF68D7B.toInt(),
    )
    val sunnyNight = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_sunny_night,
        ACTION_PREFIX + "SUNNY_NIGHT",
        0xFF121C4C.toInt(),
    )
    val partlySunnyNight = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_partly_sunny_night,
        ACTION_PREFIX + "PARTLY_SUNNY_NIGHT",
        0xFF2C3358.toInt(),
    )
    val sandstorm = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_sandstorm,
        ACTION_PREFIX + "SANDSTORM",
        0xFFC99C77.toInt(),
    )

    /** Selected by time of day, never by [getBackground] — see the class note. */
    val sunrise = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_sunrise, ACTION_PREFIX + "SUNRISE", 0xFF84BCE6.toInt(),
    )

    /** Selected by time of day, never by [getBackground] — see the class note. */
    val sunset = StudyDetailBackgroundArtwork(
        R.drawable.study_detail_bg_gradient_sunset, ACTION_PREFIX + "SUNSET", 0xFF4A4C83.toInt(),
    )

    fun getBackground(iconNum: Int, isDay: Boolean): StudyDetailBackgroundArtwork =
        if (isDay) toDayBackground(iconNum) else toNightBackground(iconNum)

    /** `BackgroundProvider.toDayBackground` — the day switch, arm for arm. */
    private fun toDayBackground(iconNum: Int): StudyDetailBackgroundArtwork = when (iconNum) {
        0, 2, 23, 25 -> sunny
        4, 5, 18 -> cloudy
        6, 7, 8, 20 -> rain
        9, 10, 19, 22 -> thunderstorm
        11, 12, 13, 14, 15, 17, 27, 28, 29 -> cold
        16 -> hot
        21 -> sandstorm
        // the original's `default` arm, which equals the clear-sky arm
        else -> sunny
    }

    /**
     * `BackgroundProvider.toNightBackground` — the night switch.
     *
     * Note how differently it partitions: rain and showers (6, 7) land on PartlySunnyNight
     * rather than on Rain, and the snow codes 11/13/14 land on SunnyNight rather than Cold.
     * Only thunderstorm, hot and sandstorm keep their daytime artwork.
     */
    private fun toNightBackground(iconNum: Int): StudyDetailBackgroundArtwork = when (iconNum) {
        1, 3, 11, 13, 14, 24, 26, 27, 28, 29 -> sunnyNight
        4, 5, 6, 7, 18, 20 -> partlySunnyNight
        9, 19, 22 -> thunderstorm
        15, 17 -> cold
        16 -> hot
        21 -> sandstorm
        // the original's `default` arm, which equals the clear-night arm
        else -> sunnyNight
    }
}
