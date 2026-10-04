package dev.local.weatherstudy.widget.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.widget.home.view.component.StudyWeatherText
import dev.local.weatherstudy.widget.home.view.frame.StudyWidgetLargeExtendedFrame
import dev.local.weatherstudy.widget.home.view.frame.StudyWidgetMediumClockFrame
import dev.local.weatherstudy.widget.home.view.frame.StudyWidgetMediumFrame
import dev.local.weatherstudy.widget.home.view.frame.StudyWidgetWideSmallFrame
import dev.local.weatherstudy.widget.home.view.item.StudyWeatherTemplateData

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.widget.home.HomeWeatherAppWidget
 * (+ `HomeClockAppWidget`, and the `WidgetStateProvider` it reads its data from)
 *
 * ### The only Compose in the app
 *
 * The phone UI is Views and XML; the home widgets are Glance, which is Compose compiled
 * to `RemoteViews`. A `GlanceAppWidget` is not injected — the framework constructs it —
 * so its collaborators arrive through a Hilt entry point, as the original's do.
 *
 * `provideGlance` reads ONE snapshot: the favourite location, formatted by
 * [StudyWidgetStateProvider]. The widget does not observe the database. It is redrawn
 * when [StudyWidgetUpdater] calls `updateAll` after the stored weather changes, which is
 * the same push model the original uses (`updatePeriodMillis` is 0 in every widget info).
 *
 * One class serves every size: the [frame] decides which blocks are laid out, so each
 * manifest receiver differs only in the frame it asks for.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyHomeWeatherAppWidget(
    private val frame: StudyWidgetFrame = StudyWidgetFrame.MEDIUM,
) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching { context.widgetEntryPoint().widgetStateProvider().invoke() }.getOrNull()
        provideContent {
            StudyWidgetSurface(context) {
                when {
                    data == null -> StudyWeatherText(EMPTY_TEXT)
                    frame == StudyWidgetFrame.WIDE_SMALL -> StudyWidgetWideSmallFrame(data)
                    frame == StudyWidgetFrame.LARGE -> StudyWidgetLargeExtendedFrame(data)
                    else -> StudyWidgetMediumFrame(data)
                }
            }
        }
    }

    private companion object {
        const val EMPTY_TEXT = "Open Weather Study to add a location"
    }
}

/** Corresponds conceptually to `…widget.home.HomeClockAppWidget`. */
class StudyHomeClockAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching { context.widgetEntryPoint().widgetStateProvider().invoke() }.getOrNull()
        provideContent {
            StudyWidgetSurface(context) {
                StudyWidgetMediumClockFrame(data ?: StudyWeatherTemplateData())
            }
        }
    }
}

/** which blocks a widget lays out — one value per `appwidget-provider` size */
enum class StudyWidgetFrame { WIDE_SMALL, MEDIUM, LARGE }

/** the rounded panel every widget sits on; a tap anywhere opens the app */
@Composable
private fun StudyWidgetSurface(context: Context, content: @Composable () -> Unit) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val base = GlanceModifier
        .fillMaxSize()
        .background(PANEL_COLOR)
        .cornerRadius(CORNER_RADIUS_DP.dp)
        .padding(horizontal = PADDING_HORIZONTAL_DP.dp, vertical = PADDING_VERTICAL_DP.dp)
    Column(modifier = if (launch != null) base.clickable(actionStartActivity(launch)) else base) {
        content()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StudyWidgetEntryPoint {
    fun widgetStateProvider(): StudyWidgetStateProvider
}

private fun Context.widgetEntryPoint(): StudyWidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, StudyWidgetEntryPoint::class.java)

private val PANEL_COLOR = Color(0xE6263238)
private const val CORNER_RADIUS_DP = 24
private const val PADDING_HORIZONTAL_DP = 16
private const val PADDING_VERTICAL_DP = 12
