package dev.local.weatherstudy.app.detail.adapter.card.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.detail.view.StudyDetailCardConstraintLayout
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.adapter.card.viewholder.DetailCommonViewHolder
 *
 * ### The render-gating fields are the point
 *
 * The original stores `lastDataStateHashcode` and `lastDataSelectedLocationKey` on the
 * holder, and `DetailAdapter.onViewAttachedToWindow` compares them before calling
 * `render(...)`:
 *
 * ```kotlin
 * if (holder.lastDataStateHashcode == state.hashCode() &&
 *     holder.lastDataSelectedLocationKey == state.selectedKey) return
 * holder.render(state, state.selectedDetail)
 * ```
 *
 * So binding happens on **attach**, not in `onBindViewHolder`, and only when the state
 * a holder last rendered has actually changed. `onBindViewHolder` does one thing: set
 * `isFullSpan`. Reproducing that split is what keeps a 20-card staggered grid from
 * re-binding everything on every state emission.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
abstract class StudyDetailCommonViewHolder(
    itemView: View,
    protected val onAction: (StudyDetailCardType) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    abstract val cardType: StudyDetailCardType

    /** what this holder last drew — the adapter compares against it before re-rendering */
    var lastDataStateHashcode: Int = 0
        private set

    var lastDataSelectedLocationKey: String = ""
        private set

    protected val cardRoot: StudyDetailCardConstraintLayout?
        get() = itemView as? StudyDetailCardConstraintLayout

    /** every card layout carries the same title view; the indicator card has none */
    protected val title: android.widget.TextView? = itemView.findViewById(R.id.card_title)

    init {
        // Every card is clickable.
        //
        // The behaviour was already plumbed - onAction dispatches CardClicked, which the
        // ViewModel turns into tracking plus a ScrollToCard side effect - but nothing ever
        // called it, because no listener was attached to the card root. Only the news and
        // radar inner items had one. That is why the screen felt inert: the cards carry
        // selectableItemBackground as their foreground, but a View with no click listener is
        // not clickable, so the ripple never fired either.
        //
        // cardType is abstract and set by the subclass, so it is read inside the lambda at
        // click time rather than here, where it is not yet initialised.
        //
        // A subclass that needs its own behaviour (the content cards, the radar play button)
        // sets its listener after this one and wins.
        itemView.setOnClickListener { onAction(cardType) }
    }

    fun render(state: StudyDetailState, item: StudyDetailItemState?) {
        lastDataStateHashcode = state.hashCode()
        lastDataSelectedLocationKey = state.selectedKey
        onRender(state, item)
    }

    protected abstract fun onRender(state: StudyDetailState, item: StudyDetailItemState?)

    protected fun setTitle(titleRes: Int) {
        title?.setText(titleRes)
    }

    /**
     * The part of a render every card shares: find this card's state, apply visibility
     * and the span preference, and hand the state back only when there is something to draw.
     */
    protected inline fun <reified S : dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardState> card(
        state: StudyDetailState,
        item: StudyDetailItemState?,
    ): S? {
        val cardState = item?.cardStates?.get(cardType) as? S
        val visible = cardState?.isVisible == true
        itemView.visibility = if (visible) View.VISIBLE else View.GONE
        cardRoot?.prefersFullSpan = state.configuration.contentColumnSize == 1
        return cardState?.takeIf { visible }
    }

    protected fun inflate(parent: ViewGroup, layoutRes: Int): View =
        LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
}

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.adapter.card.viewholder.DetailViewHolderFactory
 * com.sec.android.daemonapp.app.detail.adapter.card.viewholder.DetailViewHolderFactoryKt
 *
 * ### The 20-branch hashCode dispatch
 *
 * The original's `createViewHolder(viewType)` is a chain of
 * `if (viewType == DetailCardType.<Variant>.INSTANCE.hashCode())`, one per card type.
 * It is not a `when` on an enum and not an ordinal — the view type IS the sealed
 * object's identity hash. Reconstructed exactly, because changing it to a `when` would
 * hide the mechanism `StudyDetailAdapter.getItemViewType` relies on.
 *
 * The layout each branch inflates is recovered from the decompiled factory; note the
 * three card types that share `detail_four_contents_view_holder.xml`.
 */
class StudyDetailViewHolderFactory(
    private val parent: ViewGroup,
    private val onAction: (StudyDetailCardType) -> Unit,
    /** only the Index card uses it: its tiles carry their own links */
    private val onWebLink: (String) -> Unit = {},
) {
    fun createViewHolder(viewType: Int): StudyDetailCommonViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        fun view(layoutRes: Int) = inflater.inflate(layoutRes, parent, false)

        if (viewType == StudyDetailCardType.Alert.hashCode()) {
            return StudyAlertViewHolder(view(R.layout.study_detail_alert_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.Hourly.hashCode()) {
            return StudyHourlyViewHolder(view(R.layout.study_detail_hourly_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.Precipitation.hashCode()) {
            return StudyPrecipitationViewHolder(
                view(R.layout.study_detail_precipitation_view_holder), onAction,
            )
        }
        if (viewType == StudyDetailCardType.Daily.hashCode()) {
            return StudyDailyViewHolder(view(R.layout.study_detail_daily_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.Insight.hashCode()) {
            return StudyInsightViewHolder(view(R.layout.study_detail_insight_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.SmartThings.hashCode()) {
            return StudySmartThingsViewHolder(view(R.layout.study_detail_st_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.Radar.hashCode()) {
            return StudyRadarViewHolder(view(R.layout.study_detail_radar_view_holder), onAction)
        }
        // three card types, one layout — as in the original
        if (viewType == StudyDetailCardType.Video.hashCode()) {
            return StudyVideoViewHolder(view(R.layout.study_detail_four_contents_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.TodayStoriesAndVideo.hashCode()) {
            return StudyTodayStoryAndVideoViewHolder(
                view(R.layout.study_detail_four_contents_view_holder), onAction,
            )
        }
        if (viewType == StudyDetailCardType.NewsAndVideo.hashCode()) {
            return StudyNewsAndVideoViewHolder(
                view(R.layout.study_detail_four_contents_view_holder), onAction,
            )
        }
        if (viewType == StudyDetailCardType.News.hashCode()) {
            return StudyNewsViewHolder(view(R.layout.study_detail_two_contents_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.AirIndex.hashCode()) {
            return StudyAirIndexViewHolder(view(R.layout.study_detail_air_index_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.Index.hashCode()) {
            return StudyIndexViewHolder(
                view(R.layout.study_detail_index_view_holder), onAction, onWebLink,
            )
        }
        if (viewType == StudyDetailCardType.Sun.hashCode()) {
            return StudySunViewHolder(view(R.layout.study_detail_sun_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.Moon.hashCode()) {
            return StudyMoonViewHolder(view(R.layout.study_detail_moon_index_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.SunAndMoon.hashCode()) {
            return StudySunAndMoonViewHolder(
                view(R.layout.study_detail_sun_and_moon_view_holder), onAction,
            )
        }
        if (viewType == StudyDetailCardType.BottomIndex.hashCode()) {
            return StudyBottomIndexViewHolder(
                view(R.layout.study_detail_bottom_index_view_holder), onAction,
            )
        }
        if (viewType == StudyDetailCardType.LifeTips.hashCode()) {
            return StudyLifeTipsViewHolder(view(R.layout.study_detail_life_tips_view_holder), onAction)
        }
        if (viewType == StudyDetailCardType.LifeStyle.hashCode()) {
            return StudyLifeStyleViewHolder(
                view(R.layout.study_detail_life_style_view_holder), onAction,
            )
        }
        if (viewType == StudyDetailCardType.Indicator.hashCode()) {
            return StudyIndicatorViewHolder(view(R.layout.study_detail_indicator_view_holder), onAction)
        }
        error("unknown detail view type: $viewType")
    }
}
