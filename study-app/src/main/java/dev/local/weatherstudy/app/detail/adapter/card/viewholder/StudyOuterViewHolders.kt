package dev.local.weatherstudy.app.detail.adapter.card.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Guideline
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
    private val narrative: TextView = itemView.findViewById(R.id.hourly_narrative)
    private val divider: View = itemView.findViewById(R.id.hourly_divider)
    private var adapter: StudyHourlyInnerAdapter? = null

    init {
        // No setTitle. The hourly card is the only card on the detail screen with no
        // title - the narrative sentence occupies that slot instead.
        // each item draws its curve slice outside its own bounds; nothing in the chain may clip
        list.clipChildren = false
        list.itemAnimator = null
    }

    override fun onRender(state: StudyDetailState, item: StudyDetailItemState?) {
        val card = card<StudyDetailHourlyCardState>(state, item) ?: return

        // Narrative and divider go together: a provider with no narrative leaves the strip
        // flush against the card's top padding, which is why the RecyclerView declares
        // layout_goneMarginTop=0dp.
        val hasNarrative = card.narrative.isNotEmpty()
        narrative.text = card.narrative
        narrative.visibility = if (hasNarrative) View.VISIBLE else View.GONE
        divider.visibility = if (hasNarrative) View.VISIBLE else View.GONE

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
        itemView.findViewById<RecyclerView>(R.id.daily_list).apply {
            // qualified: inside apply{}, a bare `adapter` is the RecyclerView's own property
            attachVertical(this@StudyDailyViewHolder.adapter)
            // 18dp between rows. Without it ten days stack flush and the card reads as a
            // block of text rather than a list - see study_detail_daily_view_holder.xml.
            addItemDecoration(StudyVerticalGap(resources, R.dimen.study_detail_daily_item_vertical_gap))
        }
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
    onWebLink: (String) -> Unit = {},
) : StudyDetailCommonViewHolder(itemView, onAction) {
    override val cardType = StudyDetailCardType.Index
    private val adapter = StudyIndexInnerAdapter(onWebLink)

    init {
        // There is no title and no panel. The layout root is a plain ConstraintLayout and
        // every tile is its own card - see study_detail_index_view_holder.xml.
        itemView.findViewById<RecyclerView>(R.id.index_list).apply {
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = this@StudyIndexViewHolder.adapter
            addItemDecoration(StudyIndexGridSpacing(resources))
        }

        // The previous version carried a spanSizeLookup that let an odd final tile take the
        // whole row. That was this project's invention: the original declares spanCount=2 in
        // the layout and sets no lookup at all, so an odd tile stays half width and the row
        // is left half empty. Removed rather than kept, because the "tidier" behaviour is
        // exactly the kind of difference this reconstruction exists to avoid.
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

    // No title: the sun card's arc fills the whole card and the two labelled times carry
    // the meaning. The original declares no title view here at all.

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

    // No title, as with the sun card beside it.

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
    // Two cards, one view type: the layout is a LinearLayout of two included card layouts
    // with a 10dp Space between, so each binder addresses its own card's views.
    private val sun = StudySunBinder(itemView)
    private val moon = StudyMoonBinder(itemView)

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

/**
 * The sun arc and its two times.
 *
 * Session 3: the times are now a LABEL above a VALUE in separate views, matching the
 * original - 11sp grey over 20sp weight-600 white. The single "Sunrise 5:33 AM" line the
 * previous version drew cannot carry that contrast.
 */
private class StudySunBinder(root: View) {
    private val arc: StudySunCurvedPathView = root.findViewById(R.id.sun_curved_view)
    private val riseTitle: TextView = root.findViewById(R.id.sun_rise_title)
    private val riseValue: TextView = root.findViewById(R.id.sun_rise_value)
    private val setTitle: TextView = root.findViewById(R.id.sun_set_title)
    private val setValue: TextView = root.findViewById(R.id.sun_set_value)

    fun bind(card: StudyDetailSunCardState) {
        val context = arc.context
        arc.progress = card.sunProgress
        arc.isPolarDay = card.isPolarDay
        arc.isPolarNight = card.isPolarNight

        // Polar day and night have no sunrise or sunset to show, so the pair collapses to
        // one statement across the card rather than two empty columns.
        when {
            card.isPolarDay -> {
                riseTitle.text = ""
                riseValue.text = context.getString(R.string.study_polar_day)
                setTitle.text = ""
                setValue.text = ""
            }
            card.isPolarNight -> {
                riseTitle.text = ""
                riseValue.text = context.getString(R.string.study_polar_night)
                setTitle.text = ""
                setValue.text = ""
            }
            else -> {
                riseTitle.setText(R.string.study_label_sunrise)
                riseValue.text = card.sunriseText
                setTitle.setText(R.string.study_label_sunset)
                setValue.text = card.sunsetText
            }
        }
    }
}

/**
 * The moon disc, its phase name and its two times.
 *
 * Same label-over-value pairing as the sun card; the two share
 * study_detail_sun_arc_moon_text_* so the columns line up between the stacked cards.
 */
private class StudyMoonBinder(root: View) {
    private val disc: StudyMoonPhaseView = root.findViewById(R.id.moon_icon)
    private val phase: TextView = root.findViewById(R.id.moon_state)
    private val firstTitle: TextView = root.findViewById(R.id.moon_first_title)
    private val firstValue: TextView = root.findViewById(R.id.moon_first_value)
    private val secondTitle: TextView = root.findViewById(R.id.moon_second_title)
    private val secondValue: TextView = root.findViewById(R.id.moon_second_value)
    private val verticalGuideline: Guideline? = root.findViewById(R.id.moon_vertical_guideline)

    fun bind(card: StudyDetailMoonCardState) {
        // one parameter, as in the original: 0 and 1 are new, 0.5 is full, and the
        // waxing/waning half of the month is already folded into it
        disc.phaseProgress = card.phaseProgress
        phase.text = card.phaseText

        val hasRise = bindPair(firstTitle, firstValue, R.string.study_label_moonrise, card.moonriseText)
        val hasSet = bindPair(secondTitle, secondValue, R.string.study_label_moonset, card.moonsetText)

        // The right column is normally populated now - moonrise and moonset are computed
        // in the gateway rather than fetched, because they are astronomy rather than
        // forecast data (see StudyGatewayMoon.riseSet).
        //
        // The reflow is kept for the two days in every lunar month where one of the two
        // events genuinely does not happen in the local day, and for a provider that
        // sends neither: the guideline moves to the full width and the disc centres,
        // which is the same mechanism rather than a second layout.
        verticalGuideline?.setGuidelinePercent(if (hasRise || hasSet) HALF else FULL)
    }

    /** a provider that does not supply the time leaves the pair out rather than showing "--" */
    private fun bindPair(title: TextView, value: TextView, labelRes: Int, text: String): Boolean {
        val known = text.isNotEmpty() && text != StudyTemperatureNotation.INVALID_TEXT
        title.visibility = if (known) View.VISIBLE else View.GONE
        value.visibility = if (known) View.VISIBLE else View.GONE
        if (known) {
            title.setText(labelRes)
            value.text = text
        }
        return known
    }

    private companion object {
        const val HALF = 0.5f
        const val FULL = 1f
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

/**
 * The 10dp gutter between index tiles.
 *
 * The original gets this from `detail_gap_between_cards`, applied between the AQI stub and
 * the grid in the layout and between the tiles themselves at the item level. An
 * ItemDecoration is used here rather than a margin on the tile layout because a margin
 * would also inset the outer edges, and the grid is already inset by the card list's own
 * padding - doubling it would push the tiles narrower than the cards above them.
 */
private class StudyIndexGridSpacing(resources: android.content.res.Resources) :
    RecyclerView.ItemDecoration() {

    private val gap = resources.getDimensionPixelSize(R.dimen.study_detail_gap_between_cards)

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1
        val column = position % spanCount
        // half the gutter on each inner edge, so the pair still spans the full width
        outRect.left = if (column == 0) 0 else gap / 2
        outRect.right = if (column == spanCount - 1) 0 else gap / 2
        if (position >= spanCount) outRect.top = gap
    }
}

/**
 * A fixed gap between every row after the first.
 *
 * The original expresses row rhythm as a dimension consumed by the adapter rather than as a
 * margin on the item layout, so the same row layout can sit in a tight list and a loose one.
 * detail_daily_item_vertical_gap has a _large sibling at 29.75dp for exactly that reason.
 */
private class StudyVerticalGap(
    resources: android.content.res.Resources,
    gapRes: Int,
) : RecyclerView.ItemDecoration() {

    private val gap = resources.getDimensionPixelSize(gapRes)

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position > 0) outRect.top = gap
    }
}
