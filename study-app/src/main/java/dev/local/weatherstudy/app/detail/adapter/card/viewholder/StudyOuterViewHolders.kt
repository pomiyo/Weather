package dev.local.weatherstudy.app.detail.adapter.card.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyAirIndexInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyAlertInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyBottomIndexInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyDailyInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyHourlyInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyIndexInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyInsightInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyLifeStyleInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyLifeTipsInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudyPrecipitationInnerAdapter
import dev.local.weatherstudy.app.detail.adapter.card.inner.StudySmartThingsInnerAdapter
import dev.local.weatherstudy.app.detail.view.StudyAirQualityBar
import dev.local.weatherstudy.app.detail.view.StudyMoonPhaseView
import dev.local.weatherstudy.app.detail.view.StudySunCurvedPathView
import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAirIndexCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAlertCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailBottomIndexCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailContentCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailDailyCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailHourlyCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailIndexCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailIndicatorCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailInsightCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailLifeStyleCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailLifeTipsCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailMoonCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailPrecipitationCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailRadarCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSmartThingsCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSunAndMoonCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSunCardState
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTemperatureNotation
import kotlin.math.roundToInt

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 20 outer holders in
 * com.sec.android.daemonapp.app.detail.adapter.card.viewholder:
 * `AlertViewHolder`, `HourlyViewHolder`, `PrecipitationViewHolder`, `DailyViewHolder`,
 * `InsightViewHolder`, `SmartThingsViewHolder`, `RadarViewHolder`, `VideoViewHolder`,
 * `TodayStoryAndVideoViewHolder`, `NewsAndVideoViewHolder`, `NewsViewHolder`,
 * `AirIndexViewHolder`, `IndexViewHolder`, `SunViewHolder`, `MoonViewHolder`,
 * `SunAndMoonViewHolder`, `BottomIndexViewHolder`, `LifeTipsViewHolder`,
 * `LifeStyleViewHolder`, `IndicatorViewHolder`
 *
 * ### What an outer holder does
 *
 * One card. It owns the card's title, creates its inner adapter once, and on every
 * render hands that adapter the card's state — it never reads the domain model, only the
 * pre-formatted `StudyDetail*CardState` its state provider produced. A card that is a
 * single drawing (sun, moon, air quality) sets the Canvas view's ratios directly.
 *
 * `card<S>()` in the base class does the part every holder shares: look the state up by
 * card type, apply visibility and the span preference, and return it only if visible.
 *
 * The nine cards whose content needs a Samsung platform app or a provider capability the
 * reconstruction's source does not have are wired the same way and stay hidden, because
 * nothing produces a visible state for them — see `reports/samsung-bridge-map.md`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyAlertViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Alert
    private val adapter = StudyAlertInnerAdapter { onAction(cardType) }

    init {
        setTitle(R.string.study_card_alert)
        itemView.findViewById<RecyclerView>(R.id.alert_list).attachVertical(adapter)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailAlertCardState>(state, item) ?: return
        adapter.submitList(card.items)
    }
}

class StudyHourlyViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Hourly
    private val list: RecyclerView = itemView.findViewById(R.id.hourly_list)
    private var adapter: StudyHourlyInnerAdapter? = null

    init {
        setTitle(R.string.study_card_hourly)
        // each item draws its curve slice outside its own bounds; nothing in the chain may clip
        list.clipChildren = false
        list.itemAnimator = null
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailHourlyCardState>(state, item) ?: return
        val inner = adapter ?: StudyHourlyInnerAdapter(isRtl = state.configuration.isRtl)
            .also { adapter = it; list.adapter = it }
        inner.submitList(card.items)
    }
}

class StudyPrecipitationViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Precipitation
    private val summary: TextView = itemView.findViewById(R.id.precipitation_summary)
    private val adapter = StudyPrecipitationInnerAdapter()

    init {
        setTitle(R.string.study_card_precipitation)
        itemView.findViewById<RecyclerView>(R.id.precipitation_list).adapter = adapter
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailPrecipitationCardState>(state, item) ?: return
        summary.text = card.summaryText
        adapter.submitList(card.items)
    }
}

class StudyDailyViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Daily
    private val adapter = StudyDailyInnerAdapter()

    init {
        setTitle(R.string.study_card_daily)
        itemView.findViewById<RecyclerView>(R.id.daily_list).attachVertical(adapter)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailDailyCardState>(state, item) ?: return
        adapter.submitList(card.items)
    }
}

class StudyInsightViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Insight
    private val pager: ViewPager2 = itemView.findViewById(R.id.insight_pager)
    private val indicator: TextView = itemView.findViewById(R.id.insight_page_indicator)
    private val adapter = StudyInsightInnerAdapter { onAction(cardType) }
    private var pageCount = 0
    private var shownKey: String? = null

    init {
        // no title: each page is already a titled statement
        title?.visibility = View.GONE
        pager.adapter = adapter
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showIndicator(position)
        })
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailInsightCardState>(state, item) ?: return
        pageCount = card.items.size
        val locationChanged = shownKey != item?.key
        shownKey = item?.key
        adapter.submitList(card.items) {
            // the page index belongs to a city's cards, not to the pager
            if (locationChanged) pager.setCurrentItem(0, false)
            showIndicator(pager.currentItem)
        }
    }

    private fun showIndicator(position: Int) {
        indicator.visibility = if (pageCount > 1) View.VISIBLE else View.GONE
        indicator.text = pageDots(pageCount, position)
    }
}

/** STUB content: needs the SmartThings platform app. Wired, and never given a visible state. */
class StudySmartThingsViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.SmartThings
    private val adapter = StudySmartThingsInnerAdapter()

    init {
        setTitle(R.string.study_card_smart_things)
        itemView.findViewById<RecyclerView>(R.id.st_list).attachVertical(adapter)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailSmartThingsCardState>(state, item) ?: return
        adapter.submitList(card.items)
    }
}

/**
 * The radar surface is a raster the provider supplies, shown in a plain image view with
 * the tile source's attribution row beneath — not a map SDK widget. The reconstruction's
 * data source has no radar imagery, so the card is wired and stays hidden.
 */
class StudyRadarViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Radar
    private val contentLayout: View? = itemView.findViewById(R.id.radar_content_layout)
    private val radarView: ImageView? = itemView.findViewById(R.id.radar_view)
    private val stubLabel: View? = itemView.findViewById(R.id.radar_stub_label)
    private val playButton: ImageView? = itemView.findViewById(R.id.radar_play_btn)
    private val errorMessage: TextView? = itemView.findViewById(R.id.radar_error_msg)
    private val insight: TextView? = itemView.findViewById(R.id.tv_radar_insight)
    private val attribution: View? = itemView.findViewById(R.id.radar_source)

    init {
        itemView.setOnClickListener { onAction(cardType) }
        playButton?.setOnClickListener { onAction(cardType) }
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        card<StudyDetailRadarCardState>(state, item) ?: return
        val hasImagery = radarView?.drawable != null
        contentLayout?.visibility = View.VISIBLE
        stubLabel?.visibility = if (hasImagery) View.GONE else View.VISIBLE
        playButton?.visibility = if (hasImagery) View.VISIBLE else View.GONE
        attribution?.visibility = if (hasImagery) View.VISIBLE else View.GONE
        errorMessage?.visibility = View.GONE
        insight?.visibility = View.GONE
    }
}

/** STUB content: the provider's video feed. Three card types share one layout, as in the original. */
class StudyVideoViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Video

    init {
        setTitle(R.string.study_card_video)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        card<StudyDetailContentCardState>(state, item)
    }
}

/** STUB content: the provider's stories feed. */
class StudyTodayStoryAndVideoViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.TodayStoriesAndVideo

    init {
        setTitle(R.string.study_card_today_stories)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        card<StudyDetailContentCardState>(state, item)
    }
}

/** STUB content: needs the Samsung News app. */
class StudyNewsAndVideoViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.NewsAndVideo

    init {
        setTitle(R.string.study_card_news)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        card<StudyDetailContentCardState>(state, item)
    }
}

/** STUB content: needs the Samsung News app. */
class StudyNewsViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.News

    init {
        setTitle(R.string.study_card_news)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        card<StudyDetailContentCardState>(state, item)
    }
}

class StudyAirIndexViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.AirIndex
    private val value: TextView = itemView.findViewById(R.id.air_value)
    private val level: TextView = itemView.findViewById(R.id.air_level)
    private val bar: StudyAirQualityBar = itemView.findViewById(R.id.air_bar)
    private val adapter = StudyAirIndexInnerAdapter()

    init {
        itemView.findViewById<RecyclerView>(R.id.air_pollutant_list).attachVertical(adapter)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailAirIndexCardState>(state, item) ?: return
        title?.text = itemView.context.getString(R.string.study_card_air_index) + " · " + card.scaleName
        value.text = card.aqiText
        level.text = card.levelText
        bar.entity = card.graphEntity
        bar.value = card.graphValue
        adapter.submitList(card.pollutants)
    }
}

class StudyIndexViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Index
    private val adapter = StudyIndexInnerAdapter()

    init {
        // no title and no panel of its own: this card is a grid of tiles, each one a
        // small card. The two-column grid comes from the layout; it scrolls with the page.
        title?.visibility = View.GONE
        itemView.findViewById<RecyclerView>(R.id.index_list).apply {
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = this@StudyIndexViewHolder.adapter
            (layoutManager as? GridLayoutManager)?.let { grid ->
                // an odd number of tiles: the last one takes the whole row, not half of it
                grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val count = this@StudyIndexViewHolder.adapter.itemCount
                        return if (count % 2 == 1 && position == count - 1) grid.spanCount else 1
                    }
                }
            }
        }
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailIndexCardState>(state, item) ?: return
        adapter.submitList(card.items)
    }
}

class StudySunViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Sun
    private val binder = StudySunBinder(itemView)

    init {
        setTitle(R.string.study_card_sun)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailSunCardState>(state, item) ?: return
        binder.bind(card)
    }
}

class StudyMoonViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Moon
    private val binder = StudyMoonBinder(itemView)

    init {
        setTitle(R.string.study_card_moon)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailMoonCardState>(state, item) ?: return
        binder.bind(card)
    }
}

/**
 * The combined variant the original uses on narrow layouts: one card, both drawings. It
 * reuses the two binders rather than duplicating them, which is the reason they exist.
 */
class StudySunAndMoonViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.SunAndMoon
    private val sun = StudySunBinder(itemView)
    private val moon = StudyMoonBinder(itemView)

    init {
        setTitle(R.string.study_card_sun_and_moon)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailSunAndMoonCardState>(state, item) ?: return
        sun.bind(card.sun)
        moon.bind(card.moon)
    }
}

class StudyBottomIndexViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.BottomIndex
    private val adapter = StudyBottomIndexInnerAdapter()

    init {
        setTitle(R.string.study_card_bottom_index)
        itemView.findViewById<RecyclerView>(R.id.bottom_index_list).adapter = adapter
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailBottomIndexCardState>(state, item) ?: return
        adapter.submitList(card.items)
    }
}

/** A provider capability (`supportInsightTips`) the reconstruction's source does not have. */
class StudyLifeTipsViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.LifeTips
    private val adapter = StudyLifeTipsInnerAdapter()

    init {
        setTitle(R.string.study_card_life_tips)
        itemView.findViewById<RecyclerView>(R.id.life_tips_list).adapter = adapter
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailLifeTipsCardState>(state, item) ?: return
        adapter.submitList(card.items)
    }
}

/** A provider capability (`supportLifeStyle`) the reconstruction's source does not have. */
class StudyLifeStyleViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.LifeStyle
    private val adapter = StudyLifeStyleInnerAdapter { onAction(cardType) }

    init {
        setTitle(R.string.study_card_life_style)
        itemView.findViewById<RecyclerView>(R.id.life_style_list).attachVertical(adapter)
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailLifeStyleCardState>(state, item) ?: return
        adapter.submit(card.items, card.showSettingRow)
    }
}

class StudyIndicatorViewHolder(
    itemView: View,
    onAction: (StudyDetailCardType) -> Unit,
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Indicator
    private val textLogo: View = itemView.findViewById(R.id.indicator_text_logo)
    private val provider: TextView = textLogo.findViewById(R.id.indicator_provider_text)
    private val feedback: TextView = textLogo.findViewById(R.id.indicator_feedback)
    private val updateTime: TextView = itemView.findViewById(R.id.indicator_update_time)

    init {
        // the page's closing line also carries the manual refresh: acting on this card
        // means "fetch again", which the fragment maps to the refresh action
        itemView.findViewById<View>(R.id.indicator_refresh).setOnClickListener { onAction(cardType) }
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailIndicatorCardState>(state, item) ?: return
        val context = itemView.context
        updateTime.text = context.getString(R.string.study_updated_at, item?.topInfo?.updateTimeText.orEmpty())
        provider.text = context.getString(R.string.study_data_source, card.indicator.providerName)
        feedback.visibility = View.GONE
    }
}

// ---------------------------------------------------------------- shared pieces

/** The sun arc and its two times. Shared by the sun card and the combined card. */
private class StudySunBinder(root: View) {
    private val arc: StudySunCurvedPathView = root.findViewById(R.id.sun_arc)
    private val sunrise: TextView = root.findViewById(R.id.sunrise_time)
    private val sunset: TextView = root.findViewById(R.id.sunset_time)

    fun bind(card: StudyDetailSunCardState) {
        val context = arc.context
        arc.progress = card.sunProgress
        arc.isPolarDay = card.isPolarDay
        arc.isPolarNight = card.isPolarNight
        when {
            card.isPolarDay -> {
                sunrise.text = context.getString(R.string.study_polar_day)
                sunset.text = ""
            }
            card.isPolarNight -> {
                sunrise.text = context.getString(R.string.study_polar_night)
                sunset.text = ""
            }
            else -> {
                sunrise.text = context.getString(R.string.study_sunrise, card.sunriseText)
                sunset.text = context.getString(R.string.study_sunset, card.sunsetText)
            }
        }
    }
}

/** The moon disc and its texts. Shared by the moon card and the combined card. */
private class StudyMoonBinder(root: View) {
    private val disc: StudyMoonPhaseView = root.findViewById(R.id.moon_phase)
    private val phase: TextView = root.findViewById(R.id.moon_phase_text)
    private val moonrise: TextView? = root.findViewById(R.id.moonrise_time)
    private val moonset: TextView? = root.findViewById(R.id.moonset_time)
    private val illumination: TextView? = root.findViewById(R.id.moon_illumination)

    fun bind(card: StudyDetailMoonCardState) {
        val context = disc.context
        disc.illuminationFraction = card.illuminationFraction
        // new moon -> full moon is the waxing half of the eight phases
        disc.isWaxing = card.phase <= StudyIndexLevel.MoonPhase.FULL_MOON
        phase.text = card.phaseText
        moonrise.setOrHide(card.moonriseText) { context.getString(R.string.study_moonrise, it) }
        moonset.setOrHide(card.moonsetText) { context.getString(R.string.study_moonset, it) }
        illumination?.text = context.getString(
            R.string.study_moon_illumination,
            (card.illuminationFraction * PERCENT).roundToInt(),
        )
    }

    /** a provider that does not supply the time leaves the row out rather than showing "--" */
    private fun TextView?.setOrHide(value: String, format: (String) -> String) {
        this ?: return
        val known = value.isNotEmpty() && value != StudyTemperatureNotation.INVALID_TEXT
        visibility = if (known) View.VISIBLE else View.GONE
        if (known) text = format(value)
    }

    private companion object {
        const val PERCENT = 100
    }
}

/** A list that is as tall as its rows and scrolls with the page, not inside it. */
private fun RecyclerView.attachVertical(adapter: RecyclerView.Adapter<*>) {
    layoutManager = LinearLayoutManager(context)
    isNestedScrollingEnabled = false
    itemAnimator = null
    this.adapter = adapter
}

/** "● ○ ○" — the original draws a dot indicator; text is the dependency-free equivalent. */
internal fun pageDots(count: Int, selected: Int): String =
    if (count <= 1) "" else (0 until count).joinToString("") { if (it == selected) "●" else "○" }
