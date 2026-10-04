package dev.local.weatherstudy.widget.home.view.component

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the five Kt files in
 * com.sec.android.daemonapp.home.view.component:
 * `WeatherTextKt`, `WeatherImageKt`, `ColumnDividerKt`, `WeatherTopLevelLayoutKt`,
 * `RemoteViewSizeKt`, + `WidgetDp`
 *
 * Observed responsibility: the leaf primitives. They exist because Glance's own `Text`
 * and `Image` need the widget's colour and size context threaded through, and
 * `RemoteViewSizeKt` exists because a Glance widget has to reason about its RemoteViews
 * size budget — a widget that renders too large is simply dropped by the launcher.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Composable
fun StudyWeatherText(
    text: String,
    sizeSp: Int = DEFAULT_TEXT_SP,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = sizeSp.sp,
            color = ColorProvider(Color.White),
        ),
        modifier = modifier,
    )
}

/** `…component.ColumnDividerKt`. */
/** a weather icon, resolved from its number by the same provider the app's views use */
@Composable
fun StudyWeatherIcon(iconNum: Int, sizeDp: Int = DEFAULT_ICON_DP) {
    androidx.glance.Image(
        provider = androidx.glance.ImageProvider(
            dev.local.weatherstudy.ui.common.resource.StudyWeatherIcons.iconRes(iconNum),
        ),
        contentDescription = null,
        modifier = GlanceModifier.size(sizeDp.dp),
    )
}

@Composable
fun StudyColumnDivider(heightDp: Int = DIVIDER_HEIGHT_DP) {
    Spacer(modifier = GlanceModifier.fillMaxWidth().height(heightDp.dp))
}

/** `…component.WeatherTopLevelLayoutKt`. */
@Composable
fun StudyWeatherTopLevelLayout(content: @Composable () -> Unit) {
    Column(modifier = GlanceModifier.fillMaxWidth()) { content() }
}

/**
 * Corresponds conceptually to `…component.WidgetDp` and `RemoteViewSizeKt`.
 *
 * The size thresholds a widget uses to pick its frame. The original derives them from
 * the launcher's reported span, via `StudyWidgetService.getAppWidgetColumnSpan()` — a
 * Samsung API, which is why a non-Samsung launcher falls back to these constants.
 */
object StudyWidgetDp {
    const val SMALL_MAX_WIDTH_DP = 160
    const val MEDIUM_MAX_WIDTH_DP = 280
    const val WIDE_SMALL_MAX_HEIGHT_DP = 110
}

private const val DEFAULT_TEXT_SP = 14
private const val DEFAULT_ICON_DP = 24
private const val DIVIDER_HEIGHT_DP = 8
