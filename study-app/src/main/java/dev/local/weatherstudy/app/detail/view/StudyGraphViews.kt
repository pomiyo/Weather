package dev.local.weatherstudy.app.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import dev.local.weatherstudy.app.R
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
 * Session 4 rewrite. This drew a 270° ARC, on the assumption that "percentage of a ring"
 * was the reading. It is not a ring at all - it is a 12dp-high rounded BAR, and the
 * mismatch was glaring: `detail_large_index_humidity_graph_height` is 12dp, so
 * `min(width, height) / 2` made a 6dp circle out of a view that is as wide as the tile.
 * The humidity tile rendered a small blue crescent under a centred number.
 *
 * ```java
 * float r = Math.min(getWidth(), getHeight()) / 2.0f;              // the bar's own radius
 * float w = getWidth() * clamp(value / 100f, 0f, 100f);
 * canvas.drawRoundRect(0, 0, getWidth(), getHeight(), r, r, whiteBgPaint);   // 40% white
 * paint.setShader(new LinearGradient(0, 0, w, getHeight(), colors, positions, CLAMP));
 * canvas.drawRoundRect(0, 0, w, getHeight(), r, r, paint);
 * ```
 *
 * Two details worth keeping. The track is white at **alpha 102**, not a colour resource -
 * which is why it reads the same against every one of the eleven backgrounds. And the
 * gradient's end point is the FILL's width, not the view's, so the two stops always span
 * the filled part however short it is; at 10% humidity you still see the whole ramp.
 */
class StudyHumidityGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 0..1 */
    var value: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TRACK_COLOR
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var shaderWidth = -1f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val radius = min(w, h) / 2f
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, trackPaint)

        val filled = w * value
        if (filled <= 0f) return
        if (shaderWidth != filled) {
            shaderWidth = filled
            fillPaint.shader = android.graphics.LinearGradient(
                0f, 0f, filled, h,
                intArrayOf(GRADIENT_MIN, GRADIENT_MAX),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(0f, 0f, filled, h, radius, radius, fillPaint)
    }

    private companion object {
        /** white at alpha 102 - the original builds it in code, not from a colour */
        const val TRACK_COLOR = 0x66FFFFFF.toInt()
        /** `humidity_graph_gradient_min` / `_max` */
        const val GRADIENT_MIN = 0xFFCDFAFF.toInt()
        const val GRADIENT_MAX = 0xFF6EDBFC.toInt()
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.PressureGraph
 *
 * Session 4 rewrite. This drew a needle from the centre of a 270° track, with a tendency
 * arrow beside it - a car dashboard, invented here. The original has no needle at all:
 *
 * ```java
 * canvas.drawBitmap(scaled(R.drawable.pressure_bg, 140dp, 140dp), 0, 0, null);
 * paint.setColor(col_50_FAFAFA);  paint.setStyle(STROKE);
 * paint.setStrokeWidth(detail_large_index_pressure_graph_stroke_size);   // 12dp
 * paint.setStrokeCap(ROUND);
 * path.arcTo(rect(radius 62dp), -210f, calculateAngle(value), false);    // sweep 0..240
 * canvas.drawPath(path, paint);
 * ```
 *
 * A ticked 240° track, drawn from artwork, with a 12dp half-transparent white arc laid
 * over the part the reading has reached - starting at -210°, which puts 0 at the lower
 * left and full scale at the lower right. The scale is absolute, not relative:
 * `calculateAngle` maps 960.04..1066.71 hPa onto 0..240°, so a reading near sea-level
 * standard sits just under halfway round whatever the day's range is.
 *
 * The tendency (rising / falling / steady) is NOT drawn here - it is the tile's
 * description line, "Currently falling rapidly", which is why `levelFor` produces it.
 *
 * The artwork is a LOCAL STUDY RESOURCE. Without it the ticked track is drawn
 * procedurally, so a tree without the assets still renders a gauge.
 */
class StudyPressureGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 0..1 across the absolute 960.04..1066.71 hPa scale */
    var value: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ARC_COLOR
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = TRACK_COLOR
    }
    private val bounds = RectF()
    private val background = StudyStudyResource.drawable(context, "study_pressure_bg")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val stroke = resources.getDimension(R.dimen.study_detail_large_index_pressure_graph_stroke_size)
        val radius = resources.getDimension(R.dimen.study_detail_large_index_pressure_graph_radius)
        arcPaint.strokeWidth = stroke
        trackPaint.strokeWidth = stroke

        val centre = size / 2f
        val inset = centre - radius + stroke / 2f
        bounds.set(inset, inset, size - inset, size - inset)

        if (background != null) {
            background.setBounds(0, 0, size.toInt(), size.toInt())
            background.draw(canvas)
        } else {
            canvas.drawArc(bounds, START_DEGREES, SWEEP_DEGREES, false, trackPaint)
        }
        canvas.drawArc(bounds, START_DEGREES, SWEEP_DEGREES * value, false, arcPaint)
    }

    private companion object {
        /** `col_50_FAFAFA` */
        const val ARC_COLOR = 0x7FFAFAFA
        const val TRACK_COLOR = 0x33FFFFFF
        /** the original's own start angle: 0 at the lower left */
        const val START_DEGREES = -210f
        const val SWEEP_DEGREES = 240f
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.WindGraph
 *
 * Session 4 rewrite. This drew a procedural ring with four ticks and a filled triangle
 * whose length scaled with the speed. The original draws two bitmaps and rotates one:
 *
 * ```java
 * canvas.drawBitmap(scaled(R.drawable.wind_bg, 109dp, 109dp), 0, 0, null);
 * canvas.save();
 * canvas.rotate(windDirectionDegree(direction), getWidth() / 2f, getHeight() / 2f);
 * canvas.drawBitmap(scaled(R.drawable.wind_arrow, 109dp, 109dp), 0, 0, null);
 * canvas.restore();
 * ```
 *
 * `wind_bg` is the lettered compass ring - N, NE, E … - so the ring is artwork, not
 * geometry, and the arrow is a separate full-size bitmap rotated about the centre. Speed
 * does not enter into it: the needle is always the same length, which is the opposite of
 * what the invented version did.
 *
 * **The direction table is inverted and that is deliberate.** `windDirectionDegree` maps
 * S to 0°, W to 90°, N to 180°, E to 270° - a wind FROM the south is drawn pointing up
 * the screen, i.e. the arrow shows where the wind is going. Reading those numbers as
 * compass bearings would put every reading 180° out.
 *
 * The artwork is a LOCAL STUDY RESOURCE; without it the ring and arrow are drawn
 * procedurally.
 */
class StudyWindGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** meteorological degrees: 0 = from the north */
    var directionDegree: Float = 0f
        set(value) { field = value; invalidate() }

    var isCalm: Boolean = false
        set(value) { field = value; invalidate() }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = RING_STROKE
        color = 0x33FFFFFF
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val arrowPath = Path()

    private val background = StudyStudyResource.drawable(context, "study_wind_bg")
    private val arrow = StudyStudyResource.drawable(context, "study_wind_arrow")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toInt()
        if (size <= 0) return
        val cx = width / 2f
        val cy = height / 2f

        if (background != null) {
            background.setBounds(0, 0, size, size)
            background.draw(canvas)
        } else {
            canvas.drawCircle(cx, cy, size / 2f - RING_STROKE, ringPaint)
        }

        if (isCalm) return

        // the arrow points where the wind is GOING, hence the half turn
        val rotation = directionDegree + HALF_CIRCLE
        if (arrow != null) {
            canvas.save()
            canvas.rotate(rotation, cx, cy)
            arrow.setBounds(0, 0, size, size)
            arrow.draw(canvas)
            canvas.restore()
            return
        }

        val angle = Math.toRadians(rotation.toDouble())
        val length = (size / 2f) * ARROW_LENGTH
        arrowPaint.color = 0xFFFFFFFF.toInt()
        arrowPath.reset()
        arrowPath.moveTo(cx + (length * sin(angle)).toFloat(), cy - (length * cos(angle)).toFloat())
        val left = angle + Math.toRadians(ARROW_SPREAD)
        val right = angle - Math.toRadians(ARROW_SPREAD)
        val back = length * ARROW_BACK
        arrowPath.lineTo(cx + (back * sin(left)).toFloat(), cy - (back * cos(left)).toFloat())
        arrowPath.lineTo(cx, cy)
        arrowPath.lineTo(cx + (back * sin(right)).toFloat(), cy - (back * cos(right)).toFloat())
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowPaint)
    }

    private companion object {
        const val RING_STROKE = 4f
        const val HALF_CIRCLE = 180f
        const val ARROW_LENGTH = 0.8f
        const val ARROW_SPREAD = 150.0
        const val ARROW_BACK = 0.55f
    }
}

/**
 * Resolves a LOCAL STUDY RESOURCE by name, the same way `StudyIconProvider` does.
 *
 * The extracted artwork is absent from a fresh clone, so referencing it as
 * `R.drawable.study_wind_bg` would stop the module compiling without it. Looked up by
 * name instead; null means "draw it procedurally". DO NOT REDISTRIBUTE.
 */
private object StudyStudyResource {
    fun drawable(context: Context, name: String): android.graphics.drawable.Drawable? {
        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (id == 0) null else androidx.core.content.ContextCompat.getDrawable(context, id)
    }
}
