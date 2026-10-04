package dev.local.weatherstudy.app.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.BezierLineGraphItemView
 *
 * ### The most distinctive view in the application
 *
 * This is **not** a chart that draws a whole series. Each hourly RecyclerView item owns
 * one instance, and that instance draws only the curve segment around its own data
 * point — half a segment to the left neighbour, half to the right. Scrolled together,
 * the per-item segments form one continuous curve.
 *
 * Recovered from the decompiled `onDraw`, with the original's own coefficients:
 *
 * ```
 * w = canvas.width
 * left half :  moveTo(0.5w, cur)
 *              cubicTo( 0.1w, cur  - 0.4·riseCur,
 *                      -0.1w, prev + 0.4·risePrev,
 *                      -0.5w, prev)
 * right half:  moveTo(0.5w, cur)
 *              cubicTo( 0.9w, cur  + 0.4·riseCur,
 *                       1.1w, next - 0.4·riseNext,
 *                       1.5w, next)
 *
 * where rise = the tangent at that point, as y per item width
 * ```
 *
 * Three details that are easy to lose and are preserved:
 *
 * 1. **the control points sit outside the view bounds** (−0.5w and 1.5w), which is why
 *   the parent must not clip children — see `study_detail_hourly_view_holder.xml`
 * 2. **RTL is handled by flipping the canvas**, `canvas.scale(-1f, 1f, w/2, h/2)`, not
 *   by mirroring the data
 * 3. **the original ships four debug `Paint`s** — `dotPaintDebugBef1G`, `Bef2Y`,
 *   `Aft1R`, `Aft2B` — that draw the control points when a flag is on. They are kept,
 *   because seeing the control points is the fastest way to understand why the curve is
 *   continuous across item boundaries. Toggle with [showDebugPoints].
 *
 * Values arrive pre-normalised (0..1) from `StudyDetailHourlyItemState`; the
 * normalisation across the whole series happens in the state provider, not here.
 *
 * The same goes for the three **tangents**. A segment between two hours is drawn by BOTH
 * items it joins, each from its own side, so the two must agree on the tangent at each
 * end. They can only agree if neither of them works it out: the provider computes one
 * tangent per hour and every item is told its own and its neighbours'.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyBezierLineGraphItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** normalised 0..1; 0 is the bottom of the plot area */
    var currentRatio: Float = 0.5f
        set(value) { field = value; invalidate() }

    var previousRatio: Float = Float.NaN
        set(value) { field = value; invalidate() }

    var nextRatio: Float = Float.NaN
        set(value) { field = value; invalidate() }

    /** tangent at this point, in ratio per item width; positive is rising */
    var slope: Float = 0f
        set(value) { field = value; invalidate() }

    var previousSlope: Float = 0f
        set(value) { field = value; invalidate() }

    var nextSlope: Float = 0f
        set(value) { field = value; invalidate() }

    var lineColorStart: Int = DEFAULT_LINE_COLOR
        set(value) { field = value; shader = null; invalidate() }

    var lineColorEnd: Int = DEFAULT_LINE_COLOR
        set(value) { field = value; shader = null; invalidate() }

    var isRtl: Boolean = false
        set(value) { field = value; invalidate() }

    /** reconstruction of the original's debug control-point dots */
    var showDebugPoints: Boolean = false
        set(value) { field = value; invalidate() }

    private val linePath = Path()
    private var shader: Shader? = null

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = LINE_STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = DEFAULT_DOT_COLOR
    }

    // the original's four debug paints, one per control point
    private val dotPaintDebugBefore1 = debugPaint(DEBUG_COLOR_BEFORE_1)
    private val dotPaintDebugBefore2 = debugPaint(DEBUG_COLOR_BEFORE_2)
    private val dotPaintDebugAfter1 = debugPaint(DEBUG_COLOR_AFTER_1)
    private val dotPaintDebugAfter2 = debugPaint(DEBUG_COLOR_AFTER_2)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.save()
        // RTL: flip the canvas rather than the data — as the original does
        if (isRtl) canvas.scale(-1f, 1f, w / 2f, h / 2f)

        ensureShader(w)

        val cur = toY(currentRatio, h)
        val hasPrev = !previousRatio.isNaN()
        val hasNext = !nextRatio.isNaN()
        val prev = if (hasPrev) toY(previousRatio, h) else cur
        val next = if (hasNext) toY(nextRatio, h) else cur

        // tangents arrive in ratio units (up is positive); y grows downward, hence the sign.
        // A control point sits CONTROL_SCALE of an item width along x, so it moves the same
        // fraction of the per-item rise along y.
        val usable = h - VERTICAL_INSET * 2f
        val riseCur = -slope * usable * CONTROL_SCALE
        val risePrev = -previousSlope * usable * CONTROL_SCALE
        val riseNext = -nextSlope * usable * CONTROL_SCALE

        if (hasPrev) {
            linePath.reset()
            linePath.moveTo(w * MID, cur)
            linePath.cubicTo(
                w * CTRL_NEAR, cur - riseCur,
                -w * CTRL_NEAR_NEG, prev + risePrev,
                -w * MID, prev,
            )
            canvas.drawPath(linePath, linePaint)

            if (showDebugPoints) {
                canvas.drawCircle(
                    w * CTRL_NEAR, cur - riseCur,
                    DEBUG_DOT_RADIUS, dotPaintDebugBefore1,
                )
                canvas.drawCircle(
                    -w * CTRL_NEAR_NEG, prev + risePrev,
                    DEBUG_DOT_RADIUS, dotPaintDebugBefore2,
                )
            }
        }

        if (hasNext) {
            linePath.reset()
            linePath.moveTo(w * MID, cur)
            linePath.cubicTo(
                w * CTRL_FAR, cur + riseCur,
                w * CTRL_FAR_OUT, next - riseNext,
                w * OUTSIDE, next,
            )
            canvas.drawPath(linePath, linePaint)

            if (showDebugPoints) {
                canvas.drawCircle(
                    w * CTRL_FAR, cur + riseCur,
                    DEBUG_DOT_RADIUS, dotPaintDebugAfter1,
                )
                canvas.drawCircle(
                    w * CTRL_FAR_OUT, next - riseNext,
                    DEBUG_DOT_RADIUS, dotPaintDebugAfter2,
                )
            }
        }

        canvas.drawCircle(w * MID, cur, DOT_RADIUS, dotPaint)
        canvas.restore()
    }

    private fun ensureShader(w: Float) {
        if (shader == null) {
            shader = LinearGradient(
                0f, 0f, w * MID, 0f,
                lineColorStart, lineColorEnd,
                Shader.TileMode.CLAMP,
            )
            linePaint.shader = shader
        }
    }

    /** ratio 0..1 → y, inset so the dot and stroke are not clipped */
    private fun toY(ratio: Float, h: Float): Float {
        val usable = h - VERTICAL_INSET * 2f
        return VERTICAL_INSET + usable * (1f - ratio.coerceIn(0f, 1f))
    }

    private fun debugPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private companion object {
        // the original's coefficients
        const val MID = 0.5f
        const val CTRL_NEAR = 0.1f
        const val CTRL_NEAR_NEG = 0.1f
        const val CTRL_FAR = 0.9f
        const val CTRL_FAR_OUT = 1.1f
        const val OUTSIDE = 1.5f
        const val CONTROL_SCALE = 0.4f

        const val LINE_STROKE_WIDTH = 6f
        const val DOT_RADIUS = 7f
        const val DEBUG_DOT_RADIUS = 5f
        const val VERTICAL_INSET = 12f

        const val DEFAULT_LINE_COLOR = 0xFFFFFFFF.toInt()
        const val DEFAULT_DOT_COLOR = 0xFFFFFFFF.toInt()

        // the original's four debug colours, from the field names Bef1G/Bef2Y/Aft1R/Aft2B
        const val DEBUG_COLOR_BEFORE_1 = 0xFF00FF00.toInt() // G
        const val DEBUG_COLOR_BEFORE_2 = 0xFFFFFF00.toInt() // Y
        const val DEBUG_COLOR_AFTER_1 = 0xFFFF0000.toInt() // R
        const val DEBUG_COLOR_AFTER_2 = 0xFF0000FF.toInt() // B
    }
}
