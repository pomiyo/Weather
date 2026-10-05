package dev.local.weatherstudy.app.detail.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.domain.type.StudyLifeStyleStateType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.DetailCardConstraintLayout (+ its Kt file)
 *
 * **22 layout references** — the root of every detail card. It owns the card chrome and,
 * crucially, reports its own span preference: `DetailAdapter.onBindViewHolder` reads
 * `contentColumnSize` and sets `StaggeredGridLayoutManager.LayoutParams.isFullSpan` on
 * this view. Having a dedicated root type is what makes that one-line bind possible.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailCardConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    /** set by the view holder from the card's state; the adapter applies it to the LayoutParams */
    var prefersFullSpan: Boolean = false

    private val roundMaskPath = Path()
    private val viewClipBounds = Rect()
    private val roundRadius: Float =
        resources.getDimensionPixelSize(R.dimen.study_detail_card_radius).toFloat()

    /**
     * Darkens the card by compositing 20% black over its background with [BlendMode.SRC].
     *
     * The original exposes this as `setBgDarken` and uses it when a card needs to read as
     * recessed against the artwork behind it. SRC (not SRC_OVER) means the filter replaces
     * the drawable's own colour rather than tinting it.
     */
    var isBgDarken: Boolean = false
        set(value) {
            field = value
            val bg = background ?: return
            if (value) {
                bg.colorFilter = BlendModeColorFilter(
                    Color.argb(0.2f, 0f, 0f, 0f),
                    BlendMode.SRC,
                )
            } else {
                bg.clearColorFilter()
            }
        }

    init {
        clipToPadding = false
        // The background is set HERE, not via android:background in the 22 card layouts.
        // Centralising it is what lets every card share one surface definition; it is also
        // why none of the detail_*_view_holder layouts carry a background attribute.
        background = ContextCompat.getDrawable(context, R.drawable.study_card_background)
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = ContextCompat.getDrawable(context, outValue.resourceId)
        stateListAnimator = pressScaleAnimator()
    }

    /**
     * The press feedback, approximating `?seslSmallTouchAnimator`.
     *
     * The original sets that theme attribute as `android:stateListAnimator` on 18 views
     * across the detail layouts - it is One UI's press-scale, and it is a large part of why
     * the real app feels responsive and a static reconstruction does not. It lives in the
     * unpublished SESL AppCompat fork, so there is no AndroidX equivalent to point at.
     *
     * Reconstructed as a plain StateListAnimator: scale to [PRESSED_SCALE] over
     * [PRESS_DURATION_MS] on press and back on release. The numbers are judged by eye
     * against the device, not traced - the original's animator is a compiled resource in a
     * fork this project does not have. Flagged as an approximation in
     * reports/visual-comparison.md.
     */
    private fun pressScaleAnimator(): StateListAnimator {
        fun scaleTo(value: Float, duration: Long) = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@StudyDetailCardConstraintLayout, SCALE_X, value),
                ObjectAnimator.ofFloat(this@StudyDetailCardConstraintLayout, SCALE_Y, value),
            )
            this.duration = duration
        }
        return StateListAnimator().apply {
            addState(intArrayOf(android.R.attr.state_pressed), scaleTo(PRESSED_SCALE, PRESS_DURATION_MS))
            addState(IntArray(0), scaleTo(1f, RELEASE_DURATION_MS))
        }
    }

    private companion object {
        const val PRESSED_SCALE = 0.97f
        const val PRESS_DURATION_MS = 100L
        const val RELEASE_DURATION_MS = 350L
    }

    /**
     * Rounds the card by clipping the canvas to a round-rect path, rather than relying on
     * the background shape alone.
     *
     * This matters because children can draw outside the background's bounds - the hourly
     * card's bezier segments deliberately extend past their own cell. Clipping in
     * dispatchDraw guarantees the corner is round regardless of what children do.
     *
     * The guard on a zero-sized clip is in the original too: a card measured at 0 would
     * otherwise produce an empty path and blank the card.
     */
    override fun dispatchDraw(canvas: Canvas) {
        if (canvas.getClipBounds(viewClipBounds)) {
            if (viewClipBounds.width() != 0 && viewClipBounds.height() != 0) {
                roundMaskPath.reset()
                roundMaskPath.addRoundRect(
                    RectF(viewClipBounds),
                    roundRadius,
                    roundRadius,
                    Path.Direction.CW,
                )
                canvas.clipPath(roundMaskPath)
            }
        }
        super.dispatchDraw(canvas)
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.CollapsibleToolbar
 *
 * The collapsing header. It coordinates with `StudyDetailAppBarOffsetChangedListener`,
 * which drives [collapseProgress]; at full collapse the header switches its icon from
 * the Lottie animation to the static WebP — see `StudyDetailImageType`.
 */
class StudyCollapsibleToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 0 = fully expanded, 1 = fully collapsed */
    var collapseProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            onCollapseProgressChanged?.invoke(field)
            invalidate()
        }

    var onCollapseProgressChanged: ((Float) -> Unit)? = null
}

/** Corresponds conceptually to `…detail.view.DetailSwipeRefresh`. */
class StudyDetailSwipeRefresh @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : androidx.swiperefreshlayout.widget.SwipeRefreshLayout(context, attrs) {

    /**
     * The detail screen nests a horizontal hourly list inside a vertical grid inside
     * pull-to-refresh. The original gates the refresh gesture on the header being fully
     * expanded, so a mid-scroll downward fling does not trigger a refresh.
     */
    var isHeaderExpanded: Boolean = true

    override fun canChildScrollUp(): Boolean = !isHeaderExpanded || super.canChildScrollUp()
}

/** Corresponds conceptually to `…detail.view.KeyPadConstraintLayout`. */
class StudyKeyPadConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs)

/**
 * Corresponds conceptually to the high/low range bar in `detail_daily_inner_item.xml`.
 *
 * Both ratios are computed across the whole series in the state provider, so every row's
 * bar is on the same scale — which is the point of a range bar.
 */
class StudyDailyRangeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var lowRatio: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var highRatio: Float = 1f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var todayMarkerRatio: Float = Float.NaN
        set(value) { field = value; invalidate() }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC107.toInt() }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f) return
        val radius = h / 2f

        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        rect.set(w * lowRatio, 0f, w * highRatio, h)
        canvas.drawRoundRect(rect, radius, radius, rangePaint)

        if (!todayMarkerRatio.isNaN()) {
            canvas.drawCircle(w * todayMarkerRatio, h / 2f, radius, markerPaint)
        }
    }
}

/**
 * Corresponds conceptually to the bar in `detail_precipitation_item.xml`.
 *
 * Session 3 rewrite. The previous version drew a full-height rounded "track" behind the
 * fill and used `radius = width / 2`, which on a 48dp-wide, 64dp-tall cell produced a
 * stadium shape - the column of overlapping ellipses visible in the pre-fix capture.
 *
 * The original is not a progress bar at all. `detail_precipitation_item.xml` is a
 * ConstraintLayout holding:
 *
 *   * a movable horizontal Guideline, `gl_precipGraphBarTop`, whose percentage is set from
 *     the amount;
 *   * `graphFill`, a plain View constrained from that guideline down to the bottom, at
 *     `layout_constraintWidth_percent="0.72"`;
 *   * an ImageView of `rain_graph_top`, [CAP_HEIGHT_DP] tall and the same 72% wide, sitting
 *     ON the guideline as the bar's cap.
 *
 * So: a 72%-wide square-sided column with a small rounded cap, no track behind it. That is
 * reproduced here in one view because the three-view split exists to let the original
 * animate the guideline, which the reconstruction does not do.
 */
class StudyPrecipitationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var amountRatio: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var barColor: Int = 0xFF4FC3F7.toInt()
        set(value) { field = value; invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val density = resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || amountRatio <= 0f) return

        // layout_constraintWidth_percent = 0.72 in the original
        val barWidth = w * BAR_WIDTH_PERCENT
        val left = (w - barWidth) / 2f
        val top = h * (1f - amountRatio)

        fillPaint.color = barColor
        rect.set(left, top, left + barWidth, h)
        // The cap is the only rounded part; the sides and foot are square, which is what
        // makes a row of these read as a bar chart rather than a row of pills.
        val capRadius = CAP_HEIGHT_DP * density
        canvas.drawRoundRect(rect, capRadius, capRadius, fillPaint)
        // square off the foot that drawRoundRect just rounded
        rect.set(left, top + capRadius, left + barWidth, h)
        canvas.drawRect(rect, fillPaint)
    }

    private companion object {
        const val BAR_WIDTH_PERCENT = 0.72f
        /** detail_precipitation_graph_top_img_height */
        const val CAP_HEIGHT_DP = 2.4f
    }
}

/**
 * Corresponds conceptually to the band strip in `detail_life_style_inner_item.xml`.
 *
 * [bandCount] comes from `StudyLifeStyleStateType.bandCount(stateType)` — the original
 * encodes the band count in the state value, so a 3-level activity and a 5-level one
 * render with different segment counts from the same field.
 */
class StudyLifeStyleBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var stateType: Int = StudyLifeStyleStateType.FAIR_OF_3LEVELS
        set(value) {
            field = value
            bandCount = StudyLifeStyleStateType.bandCount(value)
            invalidate()
        }

    var bandCount: Int = 3
        private set

    var activeBand: Int = 1
        set(value) { field = value; invalidate() }

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF66BB6A.toInt() }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || bandCount <= 0) return

        val gap = BAND_GAP
        val bandWidth = (w - gap * (bandCount - 1)) / bandCount
        val radius = h / 2f

        for (i in 0 until bandCount) {
            val left = i * (bandWidth + gap)
            rect.set(left, 0f, left + bandWidth, h)
            canvas.drawRoundRect(
                rect, radius, radius,
                if (i < activeBand) activePaint else inactivePaint,
            )
        }
    }

    private companion object {
        const val BAND_GAP = 3f
    }
}

/** Corresponds conceptually to `…detail.view.PreciseLocationTips`. */
class StudyPreciseLocationTips @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    /**
     * The original prefers One UI's `SemSmartTipManager` bubble and falls back to this
     * plain view when it is unavailable — which, in the reconstruction, is always.
     * See `StudySmartTipService`.
     */
    var message: CharSequence = ""
}
