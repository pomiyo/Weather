package dev.local.weatherstudy.app.detail.state.provider

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailScreenType
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.state.provider.DetailScreenStateProvider
 *
 * Observed responsibility: classify the WINDOW — not the device — into one of the four
 * `DetailScreenTypeState` values. It reads two numbers and nothing else:
 *
 * ```java
 * private boolean isScreenWidthDpHuge()  { return config.screenWidthDp >= 960; }
 * private boolean isScreenWidthDpLarge() { int w = config.screenWidthDp; return 589 <= w && w < 960; }
 * private boolean isScreenHeightDpSmall(){ return config.screenHeightDp < 411; }
 * ```
 *
 * ### Why height decides it, and why 411
 *
 * A short window is a phone lying on its side, whatever its width says. The A35 in
 * landscape is 832dp wide — comfortably past the 589dp "large" threshold — and would be
 * treated as a tablet by width alone. Its height is 384dp, under 411, so it comes out as
 * `NormalLandscape`: one narrow centred column, a 56dp toolbar and no hero illustration.
 * 411dp is the height of a phone's short edge in the sw411dp class that Samsung's own
 * dimension ramp is built around, so "shorter than a phone is wide" is the test.
 *
 * Note the first two branches of the original's `invoke` both start from "huge", and a
 * huge-but-short window is also `NormalLandscape` rather than `Huge`. A 960dp-wide window
 * that is only 380dp tall is a desktop window someone has dragged flat, and the detail
 * screen refuses to spread the cards across it.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailScreenStateProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    operator fun invoke(): StudyDetailScreenType = invoke(context.resources.configuration)

    operator fun invoke(configuration: Configuration): StudyDetailScreenType {
        val isHuge = configuration.screenWidthDp >= HUGE_WIDTH_DP_FROM
        val isLarge = configuration.screenWidthDp in LARGE_WIDTH_DP_FROM until HUGE_WIDTH_DP_FROM
        val isShort = configuration.screenHeightDp < LANDSCAPE_HEIGHT_DP_UNTIL

        return when {
            isHuge && !isShort -> StudyDetailScreenType.HUGE
            isHuge -> StudyDetailScreenType.NORMAL_LANDSCAPE
            isLarge && !isShort -> StudyDetailScreenType.LARGE
            isLarge -> StudyDetailScreenType.NORMAL_LANDSCAPE
            else -> StudyDetailScreenType.NORMAL
        }
    }

    companion object {
        const val HUGE_WIDTH_DP_FROM = 960
        const val LARGE_WIDTH_DP_FROM = 589
        const val LANDSCAPE_HEIGHT_DP_UNTIL = 411
    }
}
