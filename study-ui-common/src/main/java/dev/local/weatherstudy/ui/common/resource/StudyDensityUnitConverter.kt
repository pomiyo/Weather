package dev.local.weatherstudy.ui.common.resource

import android.content.Context

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.ui.common.resource.DensityUnitConverter
 *
 * Observed responsibility: the two conversions the detail screen's layout arithmetic
 * needs. It exists because those decisions are made in code — `GetContentAreaWidth`
 * multiplies `screenWidthDp` by a ratio, `GetColumnSize` compares the result against a
 * 618dp threshold — and neither number can come from a dimension resource.
 *
 * `dpToPx` rounds with `+ 0.5f` before truncating; `pxToDp` truncates. That asymmetry is
 * the original's and is kept: the padding arithmetic in `PagerViewHolder` depends on
 * dpToPx landing on the same pixel as the resource compiler's own rounding.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyDensityUnitConverter {

    fun dpToPx(dp: Float, context: Context): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    fun pxToDp(px: Int, context: Context): Int =
        (px / context.resources.displayMetrics.density).toInt()
}
