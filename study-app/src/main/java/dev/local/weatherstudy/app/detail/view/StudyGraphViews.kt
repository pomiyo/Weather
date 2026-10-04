package dev.local.weatherstudy.app.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import dev.local.weatherstudy.ui.common.detail.state.StudyIndexGraphViewEntity
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.SunCurvedPathView
 *
 * Observed responsibility: the sunrise→sunset arc with the sun's current position on it.
 * The arc is a `Path`, and the marker is placed with `PathMeasure.getPosTan` at a
 * fraction of its length — which is why [progress] is a 0..1 fraction computed by
 * `StudySunProgressNotation` rather than a time the view has to interpret.
 *
 * The polar cases matter: with `arcticNightType` set there is no sunrise or sunset, and
 * the view draws a full or empty arc instead of placing a marker. See
 * `StudyForecastTime.ARCTIC_POLAR_DAY`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudySunCurvedPathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 0 = sunrise, 1 = sunset */
    var progress: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var isPolarDay: Boolean = false
        set(value) { field = value; invalidate() }

    var isPolarNight: Boolean = false
        set(value) { field = value; invalidate() }

    var arcColor: Int = 0x66FFFFFF
        set(value) { field = value; invalidate() }

    var elapsedColor: Int = 0xFFFFC107.toInt()
        set(value) { field = value; invalidate() }

    private val arcPath = Path()
    private val elapsedPath = Path()
    private val pathMeasure = PathMeasure()
    private val markerPosition = FloatArray(2)

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(DASH_ON, DASH_OFF), 0f)
    }

    private val elapsedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = HORIZON_STROKE_WIDTH
        color = 0x33FFFFFF
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val inset = STROKE_WIDTH + MARKER_RADIUS
        val bounds = RectF(inset, inset, w - inset, h * 2f - inset)

        arcPath.reset()
        arcPath.addArc(bounds, ARC_START_DEGREES, ARC_SWEEP_DEGREES)
        arcPaint.color = arcColor
        canvas.drawPath(arcPath, arcPaint)

        canvas.drawLine(0f, h - inset, w, h - inset, horizonPaint)

        val effectiveProgress = when {
            isPolarDay -> 1f
            isPolarNight -> 0f
            else -> progress
        }

        pathMeasure.setPath(arcPath, false)
        val length = pathMeasure.length

        if (effectiveProgress > 0f) {
            elapsedPath.reset()
            pathMeasure.getSegment(0f, length * effectiveProgress, elapsedPath, true)
            elapsedPaint.color = elapsedColor
            canvas.drawPath(elapsedPath, elapsedPaint)
        }

        if (!isPolarNight) {
            pathMeasure.getPosTan(length * effectiveProgress, markerPosition, null)
            markerPaint.color = elapsedColor
            canvas.drawCircle(markerPosition[0], markerPosition[1], MARKER_RADIUS, markerPaint)
        }
    }

    private companion object {
        const val STROKE_WIDTH = 5f
        const val HORIZON_STROKE_WIDTH = 2f
        const val MARKER_RADIUS = 12f
        const val DASH_ON = 8f
        const val DASH_OFF = 10f
        const val ARC_START_DEGREES = 180f
        const val ARC_SWEEP_DEGREES = 180f
    }
}

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.DetailMoonPhaseView
 *
 * Observed responsibility: draw one of the eight moon phases
 * ([dev.local.weatherstudy.domain.type.StudyIndexLevel.MoonPhase]). The terminator is a
 * second circle offset horizontally — the classic two-circle construction — and
 * [illuminationFraction] drives the offset, so a continuous value renders smoothly
 * between the eight named phases.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyMoonPhaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 0 = new moon, 0.5 = full, 1 = new again */
    var illuminationFraction: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var isWaxing: Boolean = true
        set(value) { field = value; invalidate() }

    var litColor: Int = 0xFFF5F5F5.toInt()
        set(value) { field = value; invalidate() }

    var shadowColor: Int = 0xFF2B2B2B.toInt()
        set(value) { field = value; invalidate() }

    private val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val clipPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = min(w, h) / 2f - EDGE_INSET
        if (radius <= 0f) return
        val cx = w / 2f
        val cy = h / 2f

        litPaint.color = litColor
        shadowPaint.color = shadowColor

        // the lit disc
        canvas.drawCircle(cx, cy, radius, litPaint)

        // the terminator: a second circle offset along x, clipped to the disc
        val offset = (1f - illuminationFraction * 2f).coerceIn(-1f, 1f) * radius * 2f
        val direction = if (isWaxing) -1f else 1f

        clipPath.reset()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawCircle(cx + offset * direction, cy, radius, shadowPaint)
        canvas.restore()
    }

    private companion object {
        const val EDGE_INSET = 2f
    }
}

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.UvGraph
 *
 * Observed responsibility: a banded horizontal bar with the current value marked. The
 * bands come from [StudyIndexGraphViewEntity], produced by `GetIndexGraphViewEntity` —
 * **not** from view constants, because the banding is regional. That is the same reason
 * `AirQualityBar` takes its bands from a use case.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyUvGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var entity: StudyIndexGraphViewEntity = StudyIndexGraphViewEntity()
        set(value) { field = value; invalidate() }

    var value: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    private val density = resources.displayMetrics.density
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }
    private val markerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x40000000
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF
    }
    private val rect = RectF()
    private val clip = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val barHeight = BAR_HEIGHT_DP * density
        val markerRadius = MARKER_RADIUS_DP * density
        // inset by the marker so it is whole at both ends of the scale
        val left = markerRadius
        val right = w - markerRadius
        val barTop = (h - barHeight) / 2f
        rect.set(left, barTop, right, barTop + barHeight)
        canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, trackPaint)

        val boundaries = entity.bandBoundaries
        val colors = entity.bandColors
        if (boundaries.isNotEmpty() && colors.size >= boundaries.size) {
            // one rounded clip for the whole bar, so the bands meet squarely inside it
            clip.reset()
            clip.addRoundRect(rect, barHeight / 2f, barHeight / 2f, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clip)
            var start = left
            boundaries.forEachIndexed { index, boundary ->
                val end = left + boundary.coerceIn(0f, 1f) * (right - left)
                bandPaint.color = colors[index]
                canvas.drawRect(start, barTop, end, barTop + barHeight, bandPaint)
                start = end
            }
            canvas.restore()
        }

        val markerX = left + (right - left) * value
        val markerY = barTop + barHeight / 2f
        canvas.drawCircle(markerX, markerY, markerRadius + density, markerRingPaint)
        canvas.drawCircle(markerX, markerY, markerRadius, markerPaint)
    }

    private companion object {
        const val BAR_HEIGHT_DP = 6f
        const val MARKER_RADIUS_DP = 6f
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.AirQualityBar
 *
 * Same banded-bar construction as [StudyUvGraph] but with band labels, because the AQI
 * card names its bands ("Good", "Moderate", …) and the national scale decides how many
 * there are — see `StudyIndexLevel.AqiScale`.
 */
class StudyAirQualityBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var entity: StudyIndexGraphViewEntity = StudyIndexGraphViewEntity()
        set(value) { field = value; invalidate() }

    var value: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var bandLabels: List<String> = emptyList()
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = LABEL_TEXT_SP * resources.displayMetrics.scaledDensity
        color = 0xB3FFFFFF.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }
    private val markerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x40000000
    }
    private val rect = RectF()
    private val clip = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val barHeight = BAR_HEIGHT_DP * density
        val markerRadius = MARKER_RADIUS_DP * density
        val hasLabels = bandLabels.isNotEmpty()
        // with labels the bar sits above them; without, it is centred
        val labelArea = if (hasLabels) labelPaint.textSize + LABEL_GAP_DP * density else 0f
        val barTop = (h - labelArea - barHeight) / 2f
        val barBottom = barTop + barHeight
        val left = markerRadius
        val right = w - markerRadius

        rect.set(left, barTop, right, barBottom)
        clip.reset()
        clip.addRoundRect(rect, barHeight / 2f, barHeight / 2f, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        var start = left
        val edges = ArrayList<Float>()
        entity.bandBoundaries.forEachIndexed { index, boundary ->
            val end = left + boundary.coerceIn(0f, 1f) * (right - left)
            bandPaint.color = entity.bandColors.getOrElse(index) { 0x33FFFFFF }
            canvas.drawRect(start, barTop, end, barBottom, bandPaint)
            edges += (start + end) / 2f
            start = end
        }
        canvas.restore()

        if (hasLabels) {
            edges.forEachIndexed { index, centre ->
                bandLabels.getOrNull(index)?.let { canvas.drawText(it, centre, h - LABEL_GAP_DP * density / 2f, labelPaint) }
            }
        }

        val markerX = left + (right - left) * value
        val markerY = barTop + barHeight / 2f
        canvas.drawCircle(markerX, markerY, markerRadius + density, markerRingPaint)
        canvas.drawCircle(markerX, markerY, markerRadius, markerPaint)
    }

    private companion object {
        const val BAR_HEIGHT_DP = 8f
        const val MARKER_RADIUS_DP = 7f
        const val LABEL_TEXT_SP = 10f
        const val LABEL_GAP_DP = 4f
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.HumidityGraph
 *
 * Observed responsibility: a circular fill gauge. Reconstructed as an arc sweep, which
 * is the simplest construction that matches a "percentage of a ring" reading.
 */
class StudyHumidityGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var value: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var fillColor: Int = 0xFF4FC3F7.toInt()
        set(value) { field = value; invalidate() }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        color = 0x33FFFFFF
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
    }
    private val bounds = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val inset = STROKE_WIDTH / 2f + 1f
        val left = (width - size) / 2f + inset
        val top = (height - size) / 2f + inset
        bounds.set(left, top, left + size - inset * 2f, top + size - inset * 2f)

        canvas.drawArc(bounds, START_DEGREES, SWEEP_DEGREES, false, trackPaint)
        fillPaint.color = fillColor
        canvas.drawArc(bounds, START_DEGREES, SWEEP_DEGREES * value, false, fillPaint)
    }

    private companion object {
        const val STROKE_WIDTH = 10f
        const val START_DEGREES = 135f
        const val SWEEP_DEGREES = 270f
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.PressureGraph
 *
 * Observed responsibility: a dial with a tendency arrow. The tendency
 * (rising / falling / steady) is a separate banded value —
 * `StudyIndexLevel.Pressure` — not derived from the reading, because the provider sends
 * it. That is why this view takes two inputs.
 */
class StudyPressureGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var value: Float = 0.5f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    /** `StudyIndexLevel.Pressure.RISING` / `FALLING` / `STEADY` */
    var tendency: Int = dev.local.weatherstudy.domain.type.StudyIndexLevel.Pressure.STEADY
        set(value) { field = value; invalidate() }

    var needleColor: Int = 0xFFFFFFFF.toInt()
        set(value) { field = value; invalidate() }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        color = 0x33FFFFFF
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = NEEDLE_STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ARROW_STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val bounds = RectF()
    private val arrowPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f - STROKE_WIDTH
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(bounds, START_DEGREES, SWEEP_DEGREES, false, trackPaint)

        val angleDegrees = START_DEGREES + SWEEP_DEGREES * value
        val angleRadians = Math.toRadians(angleDegrees.toDouble())
        needlePaint.color = needleColor
        canvas.drawLine(
            cx, cy,
            cx + (radius * NEEDLE_LENGTH * cos(angleRadians)).toFloat(),
            cy + (radius * NEEDLE_LENGTH * sin(angleRadians)).toFloat(),
            needlePaint,
        )

        drawTendencyArrow(canvas, cx, cy, radius)
    }

    private fun drawTendencyArrow(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val pressure = dev.local.weatherstudy.domain.type.StudyIndexLevel.Pressure
        val dy = when (tendency) {
            pressure.RISING -> -radius * ARROW_SPAN
            pressure.FALLING -> radius * ARROW_SPAN
            else -> 0f
        }
        arrowPaint.color = needleColor
        arrowPath.reset()
        if (tendency == pressure.STEADY) {
            arrowPath.moveTo(cx - radius * ARROW_SPAN, cy + radius * ARROW_OFFSET)
            arrowPath.lineTo(cx + radius * ARROW_SPAN, cy + radius * ARROW_OFFSET)
        } else {
            arrowPath.moveTo(cx, cy + radius * ARROW_OFFSET - dy)
            arrowPath.lineTo(cx, cy + radius * ARROW_OFFSET + dy)
            arrowPath.moveTo(cx - ARROW_HEAD, cy + radius * ARROW_OFFSET - dy + dy * ARROW_HEAD_RATIO)
            arrowPath.lineTo(cx, cy + radius * ARROW_OFFSET - dy)
            arrowPath.lineTo(cx + ARROW_HEAD, cy + radius * ARROW_OFFSET - dy + dy * ARROW_HEAD_RATIO)
        }
        canvas.drawPath(arrowPath, arrowPaint)
    }

    private companion object {
        const val STROKE_WIDTH = 8f
        const val NEEDLE_STROKE = 5f
        const val ARROW_STROKE = 4f
        const val NEEDLE_LENGTH = 0.7f
        const val START_DEGREES = 150f
        const val SWEEP_DEGREES = 240f
        const val ARROW_SPAN = 0.18f
        const val ARROW_OFFSET = 0.45f
        const val ARROW_HEAD = 8f
        const val ARROW_HEAD_RATIO = 0.35f
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.WindGraph
 *
 * Observed responsibility: a compass with a direction arrow. [directionDegree] comes
 * straight from the provider; the 16-point label is produced separately by
 * `StudyWindNotation.formatDirection` — the view draws the angle, the notation layer
 * names it.
 */
class StudyWindGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** meteorological degrees: 0 = from the north */
    var directionDegree: Float = 0f
        set(value) { field = value; invalidate() }

    /** 0..1, scales the arrow length */
    var speedRatio: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var isCalm: Boolean = false
        set(value) { field = value; invalidate() }

    var arrowColor: Int = 0xFFFFFFFF.toInt()
        set(value) { field = value; invalidate() }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = RING_STROKE
        color = 0x33FFFFFF
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = TICK_STROKE
        color = 0x55FFFFFF
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val arrowPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f - RING_STROKE

        canvas.drawCircle(cx, cy, radius, ringPaint)

        // four cardinal ticks
        for (i in 0 until CARDINAL_COUNT) {
            val angle = Math.toRadians((i * (FULL_CIRCLE / CARDINAL_COUNT)).toDouble())
            val inner = radius * TICK_INNER
            canvas.drawLine(
                cx + (inner * sin(angle)).toFloat(),
                cy - (inner * cos(angle)).toFloat(),
                cx + (radius * sin(angle)).toFloat(),
                cy - (radius * cos(angle)).toFloat(),
                tickPaint,
            )
        }

        if (isCalm) return

        // meteorological "from" direction: the arrow points where the wind is going
        val angle = Math.toRadians((directionDegree + HALF_CIRCLE).toDouble())
        val length = radius * (ARROW_MIN + (ARROW_MAX - ARROW_MIN) * speedRatio)
        val tipX = cx + (length * sin(angle)).toFloat()
        val tipY = cy - (length * cos(angle)).toFloat()
        val leftAngle = angle + Math.toRadians(ARROW_SPREAD)
        val rightAngle = angle - Math.toRadians(ARROW_SPREAD)
        val backLength = length * ARROW_BACK

        arrowPaint.color = arrowColor
        arrowPath.reset()
        arrowPath.moveTo(tipX, tipY)
        arrowPath.lineTo(
            cx + (backLength * sin(leftAngle)).toFloat(),
            cy - (backLength * cos(leftAngle)).toFloat(),
        )
        arrowPath.lineTo(cx, cy)
        arrowPath.lineTo(
            cx + (backLength * sin(rightAngle)).toFloat(),
            cy - (backLength * cos(rightAngle)).toFloat(),
        )
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowPaint)
    }

    private companion object {
        const val RING_STROKE = 4f
        const val TICK_STROKE = 2f
        const val TICK_INNER = 0.82f
        const val CARDINAL_COUNT = 4
        const val FULL_CIRCLE = 360f
        const val HALF_CIRCLE = 180f
        const val ARROW_MIN = 0.35f
        const val ARROW_MAX = 0.8f
        const val ARROW_SPREAD = 150.0
        const val ARROW_BACK = 0.55f
    }
}
