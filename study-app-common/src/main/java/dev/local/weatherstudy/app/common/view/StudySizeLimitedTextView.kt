package dev.local.weatherstudy.app.common.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextClock
import androidx.appcompat.widget.AppCompatTextView

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.app.common.view.SizeLimitedTextView
 *
 * **The most-reused custom view in the whole app — 116 layout references.** It shrinks
 * its text until it fits rather than ellipsising, which is what lets the detail header
 * show a 60°-wide temperature and a long city name in the same slot across 135 locales.
 *
 * Reconstructed as a measure-time binary search on text size, with a floor.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
open class StudySizeLimitedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var maxTextSizePx: Float = textSize
    private var minTextSizePx: Float = DEFAULT_MIN_SP * resources.displayMetrics.scaledDensity

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        if (availableWidth > 0 && text.isNotEmpty()) shrinkToFit(availableWidth)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun shrinkToFit(availableWidth: Int) {
        var low = minTextSizePx
        var high = maxTextSizePx
        var best = minTextSizePx
        repeat(SEARCH_STEPS) {
            val mid = (low + high) / 2f
            paint.textSize = mid
            if (paint.measureText(text.toString()) <= availableWidth) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, best)
    }

    private companion object {
        const val DEFAULT_MIN_SP = 10f
        const val SEARCH_STEPS = 8
    }
}

/**
 * Corresponds conceptually to `…app.common.view.SizeLimitedTextClock`.
 *
 * The same shrink behaviour for a `TextClock` — used by the clock widget preview, which
 * cannot subclass [StudySizeLimitedTextView] because it needs `TextClock`'s ticking.
 */
class StudySizeLimitedTextClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextClock(context, attrs, defStyleAttr)

/** Corresponds conceptually to `…app.common.view.KeyPadListenerLinearLayout`. */
class StudyKeyPadListenerLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : android.widget.LinearLayout(context, attrs) {

    var onKeyboardVisibilityChanged: ((Boolean) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val proposedHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (lastHeight != 0 && proposedHeight != lastHeight) {
            onKeyboardVisibilityChanged?.invoke(proposedHeight < lastHeight)
        }
        lastHeight = proposedHeight
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private var lastHeight = 0
}

/**
 * Corresponds conceptually to `…app.common.setting.eula.EulaNestedScrollView`.
 *
 * Reports when the user has scrolled to the bottom — the consent button stays disabled
 * until then, which is a legal requirement, not a UX flourish.
 */
class StudyEulaNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : androidx.core.widget.NestedScrollView(context, attrs) {

    var onScrolledToBottom: (() -> Unit)? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val child = getChildAt(0) ?: return
        if (t + height >= child.height - BOTTOM_TOLERANCE_PX) onScrolledToBottom?.invoke()
    }

    private companion object {
        const val BOTTOM_TOLERANCE_PX = 8
    }
}
