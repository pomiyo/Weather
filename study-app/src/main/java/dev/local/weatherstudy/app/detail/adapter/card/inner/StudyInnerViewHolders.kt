package dev.local.weatherstudy.app.detail.adapter.card.inner

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.ui.common.resource.StudyIconProvider
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTemperatureNotation
import dev.local.weatherstudy.app.detail.view.*
import dev.local.weatherstudy.domain.type.StudyIndexType
import dev.local.weatherstudy.ui.common.detail.state.*

/**
 * Educational reconstruction of the 15 inner holders in
 * com.sec.android.daemonapp.app.detail.adapter.card.inner
 *
 * These render ONE ROW of a nested RecyclerView inside an outer card holder. Like the
 * outer 20, they only bind — every string is already formatted and every ratio already
 * normalised by the state providers.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
abstract class StudyInnerViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
}

/** `HourlyInnerViewHolder` — owns one StudyBezierLineGraphItemView drawing its own curve slice. */
class StudyHourlyInnerViewHolder(
    itemView: View,
    private val isRtl: Boolean = false,
    private val showDebugPoints: Boolean = false,
) : StudyInnerViewHolder<StudyDetailHourlyItemState>(itemView) {

    private val time: TextView = itemView.findViewById(R.id.hourly_time)
    private val icon: ImageView = itemView.findViewById(R.id.hourly_icon)
    private val precip: TextView = itemView.findViewById(R.id.hourly_precipitation)
    private val precipLayout: View = itemView.findViewById(R.id.hourly_precipitation_layout)
    private val graph: StudyBezierLineGraphItemView = itemView.findViewById(R.id.hourly_graph)
    private val temp: TextView = itemView.findViewById(R.id.hourly_temperature)
    private val sun: TextView = itemView.findViewById(R.id.hourly_sun)
    private val windLayout: View = itemView.findViewById(R.id.hourly_wind_layout)
    private val windText: TextView = itemView.findViewById(R.id.hourly_wind_text)
    private val wind: StudyWindGraph = itemView.findViewById(R.id.hourly_wind)

    override fun bind(item: StudyDetailHourlyItemState) {
        time.text = item.timeText
        temp.text = item.temperatureText
        precip.text = item.precipitationText
        precip.visibility = if (item.precipitationText.isEmpty()) View.GONE else View.VISIBLE
        // A spliced sun column carries its own drawable; an hour resolves one from the
        // icon vocabulary. getWhiteResource, not getResource: the detail cards sit on dark
        // painted artwork whatever the system theme is doing, so the white-disc variant
        // would show a disc against the sky.
        if (item.iconRes != 0) {
            icon.setImageResource(item.iconRes)
        } else {
            icon.setImageResource(StudyIconProvider.getWhiteResource(itemView.context, item.iconNum))
        }

        // the three ratios are what make the curve continuous across item boundaries
        graph.currentRatio = item.temperatureRatio
        graph.previousRatio = item.previousRatio
        graph.nextRatio = item.nextRatio
        graph.slope = item.slope
        graph.previousSlope = item.previousSlope
        graph.nextSlope = item.nextSlope
        graph.isRtl = isRtl
        graph.showDebugPoints = showDebugPoints

        // The sun label overlays the temperature at sunrise/sunset rather than replacing its
        // text, so the two can carry different text appearances - see the layout's note 2.
        //
        // `HourlySunriseItem` and `HourlySunsetItem` show the word where the temperature
        // goes and keep a temperature VALUE for the curve, which is why the graph above is
        // bound from the same fields for all three kinds.
        val isSunEvent = item.kind != StudyDetailHourlyKind.HOUR
        sun.visibility = if (isSunEvent) View.VISIBLE else View.GONE
        temp.visibility = if (isSunEvent) View.INVISIBLE else View.VISIBLE
        if (isSunEvent) {
            sun.setText(
                if (item.kind == StudyDetailHourlyKind.SUNRISE) {
                    R.string.study_label_sunrise
                } else {
                    R.string.study_label_sunset
                },
            )
        }

        // The whole precipitation row goes, not just its text: hiding the label alone would
        // leave the droplet glyph floating under an empty hour.
        precipLayout.visibility =
            if (item.precipitationText.isEmpty()) View.INVISIBLE else View.VISIBLE

        // Wind is opt-in per provider (StudyWeatherPolicy.supportWind); when it is off the
        // whole deco block is gone so the cells keep their natural height.
        if (item.windText.isEmpty()) {
            windLayout.visibility = View.GONE
        } else {
            windLayout.visibility = View.VISIBLE
            windText.text = item.windText
            wind.directionDegree = item.windDirectionDegree
        }
    }
}

/**
 * `IndexInnerViewHolder` — the base row the four graph-bearing variants extend.
 *
 * ### The tile is clickable, and the link decides it
 *
 * ```java
 * container.setClickable(!linkUri.equals(Uri.EMPTY));
 * if (container.isClickable()) {
 *     DetailBindingKt.startContextMenu(container, linkUri, viewModel.isDesktopMode());
 *     container.setOnClickListener(v -> viewModel.getIntent()
 *         .goToWeb(linkUri, state.getTrackingEvent(), state.getTrackingEventDetail()));
 * }
 * ```
 *
 * Both halves matter. A tile with no link is left NOT clickable, which in Android also
 * means no ripple and no press animation — so an inert tile looks inert, deliberately.
 * And the listener is attached inside the same `if`, which is why this reconstruction had
 * neither: nothing ever set a link, so nothing was ever clickable. Same root cause as the
 * cards in pass 6 — a View with no click listener is not clickable, and Android draws no
 * feedback for it.
 */
open class StudyIndexInnerViewHolder(
    itemView: View,
    private val onWebLink: (String) -> Unit = {},
) : StudyInnerViewHolder<StudyDetailIndexItemState>(itemView) {

    protected val title: TextView = itemView.findViewById(R.id.index_title)
    protected val value: TextView = itemView.findViewById(R.id.index_value)
    protected val unit: TextView? = itemView.findViewById(R.id.index_unit)
    protected val level: TextView? = itemView.findViewById(R.id.index_level)
    private val icon: ImageView? = itemView.findViewById(R.id.index_icon)
    private val container: View = itemView.findViewById(R.id.container) ?: itemView

    override fun bind(item: StudyDetailIndexItemState) {
        title.text = item.titleText
        value.text = item.valueText
        // empty for the five one-line tiles; the dials put it in its own view
        unit?.text = item.unitText

        container.isClickable = item.webUrl.isNotEmpty()
        if (container.isClickable) {
            container.setOnClickListener { onWebLink(item.webUrl) }
        } else {
            container.setOnClickListener(null)
        }

        // The big line under the title is the DESCRIPTION, not the level.
        //
        // The original's large tiles read "Extreme. Take precaution" and "Lower than
        // yesterday" there - a sentence at 14sp White 400. This was binding levelText, so
        // the tiles said "High" and "Very humid": correct data in the wrong slot, and far
        // too short to fill the 190dp tile, which is what left the dead gap in the middle.
        val sentence = item.descriptionText.ifEmpty { item.levelText }
        level?.text = sentence
        level?.visibility = if (sentence.isEmpty()) View.GONE else View.VISIBLE

        // The 18dp title glyph. The original uses its weather_detail_ic_*_mtrl family,
        // which this project has no counterpart for, so the slot collapses rather than
        // holding an 18dp indent in front of every title.
        icon?.visibility = View.GONE
    }
}

/** `UvIndexInnerViewHolder` — adds StudyUvGraph. */
class StudyUvIndexInnerViewHolder(itemView: View, onWebLink: (String) -> Unit = {}) :
    StudyIndexInnerViewHolder(itemView, onWebLink) {
    private val graph: StudyUvGraph = itemView.findViewById(R.id.index_graph)
    override fun bind(item: StudyDetailIndexItemState) {
        super.bind(item)
        graph.entity = item.graphEntity
        graph.value = item.graphValue
    }
}

/** `HumidityIndexInnerViewHolder`. */
class StudyHumidityIndexInnerViewHolder(itemView: View, onWebLink: (String) -> Unit = {}) :
    StudyIndexInnerViewHolder(itemView, onWebLink) {
    private val graph: StudyHumidityGraph = itemView.findViewById(R.id.index_graph)
    override fun bind(item: StudyDetailIndexItemState) {
        super.bind(item)
        graph.value = item.graphValue
    }
}

/** `PressureIndexInnerViewHolder` — tendency is a separate provider-sent value. */
class StudyPressureIndexInnerViewHolder(itemView: View, onWebLink: (String) -> Unit = {}) :
    StudyIndexInnerViewHolder(itemView, onWebLink) {
    private val graph: StudyPressureGraph = itemView.findViewById(R.id.index_graph)
    override fun bind(item: StudyDetailIndexItemState) {
        super.bind(item)
        graph.value = item.graphValue
    }
}

/** `WindIndexInnerViewHolder`. */
class StudyWindIndexInnerViewHolder(itemView: View, onWebLink: (String) -> Unit = {}) :
    StudyIndexInnerViewHolder(itemView, onWebLink) {
    private val graph: StudyWindGraph = itemView.findViewById(R.id.index_graph)
    override fun bind(item: StudyDetailIndexItemState) {
        super.bind(item)
        graph.directionDegree = item.directionDegree
        // the original's arrow does not scale with speed - it is one bitmap, rotated
        graph.isCalm = item.graphValue <= 0f
    }
}

/** `VisibilityIndexInnerViewHolder` / `DewPointIndexInnerViewHolder` — no graph. */
class StudyPlainIndexInnerViewHolder(itemView: View, onWebLink: (String) -> Unit = {}) :
    StudyIndexInnerViewHolder(itemView, onWebLink)

/** `BottomIndexInnerViewHolder` — sunrise/sunset/moon cells. */
class StudyBottomIndexInnerViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailIndexItemState>(itemView) {
    private val icon: ImageView = itemView.findViewById(R.id.bottom_index_icon)
    private val title: TextView = itemView.findViewById(R.id.bottom_index_title)
    private val value: TextView = itemView.findViewById(R.id.bottom_index_value)
    override fun bind(item: StudyDetailIndexItemState) {
        title.text = item.titleText
        value.text = item.valueText
        icon.setImageResource(0)
    }
}

/** `AlertInnerViewHolder` — severity colour comes from state, not the layout. */
class StudyAlertInnerViewHolder(
    itemView: View,
    private val onClick: (String) -> Unit,
) : StudyInnerViewHolder<StudyDetailAlertItemState>(itemView) {
    private val stripe: View = itemView.findViewById(R.id.alert_severity_stripe)
    private val title: TextView = itemView.findViewById(R.id.alert_title)
    private val issued: TextView = itemView.findViewById(R.id.alert_issued)
    override fun bind(item: StudyDetailAlertItemState) {
        title.text = item.titleText
        issued.text = item.issuedText
        stripe.setBackgroundColor(item.severityColor)
        itemView.setOnClickListener { onClick(item.webUrl) }
    }
}

/** `InsightInnerViewHolder` — one page of the insight pager. */
class StudyInsightInnerViewHolder(
    itemView: View,
    private val onClick: (String) -> Unit,
) : StudyInnerViewHolder<StudyDetailInsightItemState>(itemView) {
    private val icon: ImageView = itemView.findViewById(R.id.insight_icon)
    private val title: TextView = itemView.findViewById(R.id.insight_title)
    private val content: TextView = itemView.findViewById(R.id.insight_content)
    private val time: TextView = itemView.findViewById(R.id.insight_time)
    override fun bind(item: StudyDetailInsightItemState) {
        title.text = item.titleText
        content.text = item.contentText
        time.text = item.timeText
        time.visibility = if (item.timeText.isEmpty()) View.GONE else View.VISIBLE
        icon.setImageResource(dev.local.weatherstudy.ui.common.resource.StudyWeatherIcons.insightIconRes(item.insightType))
        itemView.setOnClickListener { onClick(item.webUrl) }
    }
}

/** `LifeStyleInnerItemViewHolder` — bandCount is decoded from stateType. */
class StudyLifeStyleInnerItemViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailLifeStyleItemState>(itemView) {
    private val icon: ImageView = itemView.findViewById(R.id.life_style_icon)
    private val title: TextView = itemView.findViewById(R.id.life_style_title)
    private val state: TextView = itemView.findViewById(R.id.life_style_state)
    private val bands: StudyLifeStyleBandView = itemView.findViewById(R.id.life_style_bands)
    override fun bind(item: StudyDetailLifeStyleItemState) {
        title.text = item.titleText
        state.text = item.stateText
        icon.setImageResource(0)
        bands.stateType = item.stateType
        bands.activeBand = item.stateType % bands.bandCount.coerceAtLeast(1) + 1
    }
}

/** `LifeStyleInnerSettingViewHolder` — the "choose activities" row at the tail. */
class StudyLifeStyleInnerSettingViewHolder(
    itemView: View,
    onClick: () -> Unit,
) : StudyInnerViewHolder<Unit>(itemView) {
    init { itemView.setOnClickListener { onClick() } }
    private val label: TextView = itemView.findViewById(R.id.life_style_setting_label)
    override fun bind(item: Unit) {
        label.setText(R.string.study_life_style_setting)
    }
}

/** backs `study_detail_life_style_inner_state_by_time_item.xml`. */
class StudyLifeStyleByTimeViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailLifeStyleByTimeState>(itemView) {
    private val band: StudyLifeStyleBandView = itemView.findViewById(R.id.by_time_band)
    private val label: TextView = itemView.findViewById(R.id.by_time_label)
    override fun bind(item: StudyDetailLifeStyleByTimeState) {
        label.text = item.timeText
        band.stateType = item.stateType
    }
}

/** `LifeTipsInnerViewHolder`. */
class StudyLifeTipsInnerViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailLifeTipsItemState>(itemView) {
    private val icon: ImageView = itemView.findViewById(R.id.life_tips_icon)
    private val title: TextView = itemView.findViewById(R.id.life_tips_title)
    private val content: TextView = itemView.findViewById(R.id.life_tips_content)
    override fun bind(item: StudyDetailLifeTipsItemState) {
        title.text = item.titleText
        content.text = item.contentText
        icon.setImageResource(0)
    }
}

/** `SmartThingsInnerViewHolder` — STUB data; needs the SmartThings platform app. */
class StudySmartThingsInnerViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailSmartThingsItemState>(itemView) {
    private val room: TextView = itemView.findViewById(R.id.st_room_name)
    private val summary: TextView = itemView.findViewById(R.id.st_room_summary)
    override fun bind(item: StudyDetailSmartThingsItemState) {
        room.text = item.roomName
        summary.text = item.summaryText
    }
}

/** one precipitation column. */
class StudyPrecipitationInnerViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailPrecipitationItemState>(itemView) {
    private val amount: TextView = itemView.findViewById(R.id.precipitation_amount)
    private val bar: StudyPrecipitationBar = itemView.findViewById(R.id.precipitation_bar)
    private val time: TextView = itemView.findViewById(R.id.precipitation_time)
    override fun bind(item: StudyDetailPrecipitationItemState) {
        amount.text = item.amountText
        time.text = item.timeText
        bar.amountRatio = item.amountRatio
    }
}

/** one daily row. */
class StudyDailyInnerViewHolder(itemView: View) :
    StudyInnerViewHolder<StudyDetailDailyItemState>(itemView) {
    private val day: TextView = itemView.findViewById(R.id.daily_day)
    private val precip: TextView = itemView.findViewById(R.id.daily_precipitation)
    private val icon: ImageView = itemView.findViewById(R.id.daily_icon)
    private val iconNight: ImageView = itemView.findViewById(R.id.daily_icon_night)
    private val precipIcon: ImageView = itemView.findViewById(R.id.daily_precipitation_icon)
    private val low: TextView = itemView.findViewById(R.id.daily_low)
    private val high: TextView = itemView.findViewById(R.id.daily_high)

    /**
     * ### A deliberate deviation: the ↑ / ↓ glyphs
     *
     * The original ends each row with two bare numbers. `DailyViewHolder` sets both
     * `tvHigh` and `tvLow` to `detail_white_text_color` at Sec.600.White.16sp and adds
     * nothing else, so "28° 22°" is exactly what Samsung draws, and position is the only
     * thing distinguishing the high from the low.
     *
     * The glyphs are added here at the user's request, borrowed from the app's own idiom
     * rather than invented: `PagerViewHolder` builds the header's high/low line as
     * `"↑" + maxTemp + " / ↓" + minTemp`, so this screen already carries them 300dp
     * further up. This is the one place the reconstruction knowingly reads better than
     * the original; revert by dropping the two prefixes.
     */
    override fun bind(item: StudyDetailDailyItemState) {
        day.text = item.dayText
        high.text = StudyTemperatureNotation.HIGH_GLYPH + item.highText
        low.text = StudyTemperatureNotation.LOW_GLYPH + item.lowText

        // The droplet goes with its value: a row with no chance to report shows neither,
        // rather than a lone glyph.
        val hasPrecip = item.precipitationText.isNotEmpty()
        precip.text = item.precipitationText
        precip.visibility = if (hasPrecip) View.VISIBLE else View.INVISIBLE
        precipIcon.visibility = if (hasPrecip) View.VISIBLE else View.INVISIBLE

        // getWhiteResource, not getResource: the detail cards sit on dark painted artwork
        // whatever the system theme is doing, so the white-disc variant would show a disc
        // against the sky. This is why the original exposes the two separately.
        val context = itemView.context
        icon.setImageResource(StudyIconProvider.getWhiteResource(context, item.iconNum))
        iconNight.setImageResource(StudyIconProvider.getWhiteResource(context, item.nightIconNum))

        // No range bar. The original ends the row with two plain numbers - see the layout.
    }
}

/** one of the four content tiles. */
class StudyContentInnerViewHolder(
    itemView: View,
    private val onClick: (String) -> Unit,
) : StudyInnerViewHolder<StudyDetailContentItemState>(itemView) {
    private val image: dev.local.weatherstudy.app.detail.view.remote.StudyRemoteImageView =
        itemView.findViewById(R.id.content_image)
    private val title: TextView = itemView.findViewById(R.id.content_title)
    private val summary: TextView? = itemView.findViewById(R.id.content_summary)
    override fun bind(item: StudyDetailContentItemState) {
        title.text = item.titleText
        summary?.text = item.summaryText
        image.loadUrl(item.imageUrl)
        itemView.setOnClickListener { onClick(item.webUrl) }
    }
}

/**
 * Corresponds conceptually to `…adapter.card.inner.AirIndexInnerViewHolder`.
 *
 * One pollutant row under the AQI bar.
 */
class StudyAirIndexInnerViewHolder(itemView: View) :
    StudyInnerViewHolder<dev.local.weatherstudy.ui.common.detail.state.StudyDetailAirIndexItemState>(itemView) {
    private val name: TextView = itemView.findViewById(R.id.pollutant_name)
    private val value: TextView = itemView.findViewById(R.id.pollutant_value)

    override fun bind(item: dev.local.weatherstudy.ui.common.detail.state.StudyDetailAirIndexItemState) {
        name.text = item.nameText
        value.text = item.valueText
    }
}
