package dev.local.weatherstudy.app.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
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

    init {
        clipChildren = false
        clipToPadding = false
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

/** Corresponds conceptually to the bar in `detail_precipitation_item.xml`. */
class StudyPrecipitationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var amountRatio: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var barColor: Int = 0xFF4FC3F7.toInt()
        set(value) { field = value; invalidate() }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val radius = w / 2f

        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        fillPaint.color = barColor
        rect.set(0f, h * (1f - amountRatio), w, h)
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
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
