package dev.local.weatherstudy.app.common.util

import android.app.Activity
import android.content.res.Configuration

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.app.common.util.AppUtils
 *
 * Observed responsibility: the handful of window questions the UI asks that are not
 * answerable from a resource qualifier. Only the two the detail screen's responsiveness
 * depends on are reproduced here; the rest of the original's AppUtils is accessibility
 * and theme plumbing that lives elsewhere in this project.
 *
 * ### Two predicates that look like one
 *
 * ```java
 * isPhoneWidthDP(dp)      { return dp < 600 && dp <= 411; }
 * isPhoneModeRangeDP(dp)  { return dp < 600; }
 *
 * isPhoneAndLandScape(a)  = isPhoneWidthDP(a.config.smallestScreenWidthDp)
 *                           && !a.isInMultiWindowMode()
 *                           && a.config.orientation == ORIENTATION_LANDSCAPE
 *
 * isPhoneModeNLandscapeOrMultiWindow(a)
 *                         = (isPhoneModeRangeDP(a.config.smallestScreenWidthDp)
 *                            && a.config.orientation == ORIENTATION_LANDSCAPE)
 *                           || a.isInMultiWindowMode()
 * ```
 *
 * They differ on purpose and the difference is visible on screen:
 *
 * * the first drives the **toolbar height** (`detail_top_info_land_height`, 56dp instead
 *   of 64dp) and insists on a true phone, not split-screen — a half-screen window still
 *   gets the full-height toolbar;
 * * the second drives the **header image** (`AnimationIconOnly` instead of
 *   `IllustrationAndAnimationIcon`), and *does* include multi-window, because the reason
 *   to drop the hero illustration is a short window, however it got that way.
 *
 * `isPhoneWidthDP`'s `dp < 600 && dp <= 411` is redundant in its first clause. Left as
 * written: it is what the bytecode does, and the two bounds presumably arrived at
 * different times.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyAppUtils {

    private const val PHONE_MODE_WIDTH_DP_UNTIL = 600
    private const val PHONE_WIDTH_DP_UNTIL = 411

    private fun isPhoneWidthDp(dp: Int): Boolean =
        dp < PHONE_MODE_WIDTH_DP_UNTIL && dp <= PHONE_WIDTH_DP_UNTIL

    private fun isPhoneModeRangeDp(dp: Int): Boolean = dp < PHONE_MODE_WIDTH_DP_UNTIL

    fun isPhoneAndLandscape(activity: Activity): Boolean =
        isPhoneAndLandscape(
            configuration = activity.resources.configuration,
            isMultiWindow = activity.isInMultiWindowMode,
        )

    fun isPhoneAndLandscape(configuration: Configuration, isMultiWindow: Boolean): Boolean =
        isPhoneWidthDp(configuration.smallestScreenWidthDp) &&
            !isMultiWindow &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun isPhoneModeNLandscapeOrMultiWindow(
        configuration: Configuration,
        isMultiWindow: Boolean,
    ): Boolean =
        (
            isPhoneModeRangeDp(configuration.smallestScreenWidthDp) &&
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            ) || isMultiWindow
}
