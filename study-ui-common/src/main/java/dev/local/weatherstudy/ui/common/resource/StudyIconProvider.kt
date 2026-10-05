package dev.local.weatherstudy.ui.common.resource

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import dev.local.weatherstudy.domain.usecase.StudyAssignIconNum
import dev.local.weatherstudy.ui.common.R

/**
 * Educational reconstruction of
 * `com.samsung.android.weather.app.common.resource.IconProvider`.
 *
 * Original APK evidence:
 * - two `SparseIntArray`s, `dayIcons` and `nightIcons`, each with thirty entries keyed by
 *   the 0..29 icon vocabulary
 * - `getResource(iconNum)` chooses between them with
 *   `(resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES`
 * - `getWhiteResource(iconNum)` always returns from `nightIcons`
 * - a third table, `transitionLargeIcons`, pairs the two for a `TransitionDrawable`
 *
 * ### The field names are misleading, and that is the finding
 *
 * `dayIcons` and `nightIcons` have nothing to do with whether it is day or night. They are
 * selected by the **system theme**:
 *
 * ```text
 *   light theme  -> dayIcons   -> ic_<cond>_whitebg   glyph on a white disc
 *   dark theme   -> nightIcons -> ic_<cond>           bare glyph
 * ```
 *
 * Time of day is already folded into the icon NUMBER upstream by `AssignIconNum` - that is
 * what distinguishes code 0 (sunny) from code 1 (clear), and 2 from 3. By the time a number
 * reaches here the day/night question is settled, and the only remaining choice is which
 * surface the icon will sit on. Reading the names literally and passing an `isDay` flag in
 * here would double-apply the distinction.
 *
 * `getWhiteResource` exists because the detail screen always draws on dark painted artwork
 * regardless of the system theme, so it needs the bare glyph unconditionally.
 *
 * ### Resolution by name, not by R.drawable
 *
 * The sixty vectors are LOCAL STUDY RESOURCES, git-ignored, and absent from a fresh clone.
 * Referencing them as `R.drawable.study_ic_sunny` would make the module fail to COMPILE
 * without them, so they are resolved by name through `Resources.getIdentifier` and cached.
 * A missing set yields 0, and [getResource] falls back to the ten vectors this project drew
 * itself - which are tracked, and are the reason a clone still renders weather icons.
 *
 * DO NOT REDISTRIBUTE the extracted set. See reports/local-study-assets.md.
 *
 * This implementation was independently reconstructed for local study.
 */
object StudyIconProvider {

    /**
     * The thirty condition names, indexed by the icon vocabulary.
     *
     * This list IS the vocabulary's definition - `StudyAssignIconNum`'s constants and these
     * names are two views of the same table, recovered from the original's resource maps.
     */
    private val CONDITION_NAMES = arrayOf(
        "sunny", "clear", "partly_cloud", "partly_cloud_night", "cloudy",
        "fog", "rain", "shower", "partly_sunny_with_shower", "thunderstorm",
        "partly_sunny_with_thunder", "light_snow", "partly_sunny_with_flurries", "snow",
        "rain_and_snow", "ice", "hot", "cold", "wind", "rain_and_thunder",
        "heavy_rain", "sand_storm", "hurricane", "mostly_sunny", "mostly_clear",
        "mostly_cloudy", "mostly_cloudy_night", "heavy_snow", "rain_and_sleet", "hail",
    )

    /** resolved ids, keyed by resource name; 0 means "looked up and not present" */
    private val cache = HashMap<String, Int>()

    private fun resolve(context: Context, resourceName: String): Int =
        cache.getOrPut(resourceName) {
            @Suppress("DiscouragedApi")
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }

    private fun nameFor(iconNum: Int, whiteBackground: Boolean): String? {
        val condition = CONDITION_NAMES.getOrNull(iconNum) ?: return null
        return if (whiteBackground) "study_ic_${condition}_whitebg" else "study_ic_$condition"
    }

    /**
     * `IconProvider.getResource(iconNum)` — picks by system theme, as the original does.
     */
    @DrawableRes
    fun getResource(context: Context, iconNum: Int): Int {
        val isDarkTheme = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // dark theme -> the bare glyph; light theme -> the glyph on a white disc
        val name = nameFor(iconNum, whiteBackground = !isDarkTheme)
        val id = name?.let { resolve(context, it) } ?: 0
        return if (id != 0) id else StudyWeatherIcons.iconRes(iconNum)
    }

    /**
     * `IconProvider.getWhiteResource(iconNum)` — always the bare glyph.
     *
     * This is what the detail screen uses: it draws on dark painted artwork whatever the
     * system theme is doing, so the white-disc variant would show a disc against the sky.
     */
    @DrawableRes
    fun getWhiteResource(context: Context, iconNum: Int): Int {
        val id = nameFor(iconNum, whiteBackground = false)?.let { resolve(context, it) } ?: 0
        return if (id != 0) id else StudyWeatherIcons.iconRes(iconNum)
    }

    /** true when the extracted set is present; used by the study menu and the reports */
    fun isExtractedSetAvailable(context: Context): Boolean =
        resolve(context, "study_ic_sunny") != 0

    /**
     * `AnimIconProvider.getResource(icon)` — the Lottie asset path for the same code.
     *
     * The original keys two parallel asset sets off the same vocabulary and picks between
     * them on `uiMode`, exactly as [getResource] does for the vectors. The header uses this
     * path; the lists use the vector one, which is why the APK ships both.
     */
    fun getAnimationAsset(context: Context, iconNum: Int): String {
        val condition = CONDITION_NAMES.getOrNull(iconNum) ?: DEFAULT_CONDITION
        val isDarkTheme = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val set = if (isDarkTheme) "dark" else "white"
        return "$set/$condition.json"
    }

    private const val DEFAULT_CONDITION = "sunny"
}
