package dev.local.weatherstudy.app.detail.adapter.card.inner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.ui.common.detail.state.*

/**
 * Educational reconstruction of the 10 inner adapters in
 * com.sec.android.daemonapp.app.detail.adapter.card.inner
 *
 * One per nested list inside a detail card. They are `ListAdapter`s with a value-equality
 * DiffUtil — the item states are data classes, so `areContentsTheSame` is just `==`, and a
 * state emission that changes one hour re-binds one row.
 *
 * Note this is a DIFFERENT dispatch from the outer adapter: here the view type is constant
 * per adapter, and only `StudyIndexInnerAdapter` varies it (by index type, to pick the right
 * Canvas view). The outer adapter keys on a sealed object's hashCode instead.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
private fun <T> valueDiff() = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(a: T & Any, b: T & Any) = a == b
    override fun areContentsTheSame(a: T & Any, b: T & Any) = a == b
}

private fun ViewGroup.inflate(res: Int) =
    LayoutInflater.from(context).inflate(res, this, false)

/** `HourlyInnerAdapter`. */
class StudyHourlyInnerAdapter(
    private val isRtl: Boolean = false,
    private val showDebugPoints: Boolean = false,
) : ListAdapter<StudyDetailHourlyItemState, StudyHourlyInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyHourlyInnerViewHolder(
            parent.inflate(R.layout.study_detail_hourly_inner_view_holder), isRtl, showDebugPoints,
        )
    override fun onBindViewHolder(holder: StudyHourlyInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/**
 * `IndexInnerAdapter` / `LargeIndexInnerAdapter`.
 *
 * The only inner adapter with real view types: the index row's layout depends on which
 * measurement it is, because four of the seven carry their own Canvas view.
 */
class StudyIndexInnerAdapter :
    ListAdapter<StudyDetailIndexItemState, StudyIndexInnerViewHolder>(valueDiff()) {

    override fun getItemViewType(position: Int) = getItem(position).indexType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyIndexInnerViewHolder {
        val t = dev.local.weatherstudy.domain.type.StudyIndexType
        return when (viewType) {
            t.UV -> StudyUvIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_uv_inner_view_holder))
            t.HUMIDITY -> StudyHumidityIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_humidity_inner_view_holder))
            t.PRESSURE -> StudyPressureIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_pressure_inner_view_holder))
            t.WIND -> StudyWindIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_wind_inner_view_holder))
            t.VISIBILITY -> StudyPlainIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_visibility_inner_view_holder))
            t.DEW_POINT -> StudyPlainIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_dew_point_inner_view_holder))
            else -> StudyIndexInnerViewHolder(
                parent.inflate(R.layout.study_detail_index_inner_view_holder))
        }
    }

    override fun onBindViewHolder(holder: StudyIndexInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `BottomIndexInnerAdapter`. */
class StudyBottomIndexInnerAdapter :
    ListAdapter<StudyDetailIndexItemState, StudyBottomIndexInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyBottomIndexInnerViewHolder(
            parent.inflate(R.layout.study_detail_bottom_index_inner_view_holder))
    override fun onBindViewHolder(holder: StudyBottomIndexInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `AlertInnerAdapter`. */
class StudyAlertInnerAdapter(private val onClick: (String) -> Unit) :
    ListAdapter<StudyDetailAlertItemState, StudyAlertInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyAlertInnerViewHolder(parent.inflate(R.layout.study_detail_alert_inner_item), onClick)
    override fun onBindViewHolder(holder: StudyAlertInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `InsightInnerAdapter` — drives a ViewPager2, not a list. */
class StudyInsightInnerAdapter(private val onClick: (String) -> Unit) :
    ListAdapter<StudyDetailInsightItemState, StudyInsightInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyInsightInnerViewHolder(parent.inflate(R.layout.study_detail_insight_inner_item), onClick)
    override fun onBindViewHolder(holder: StudyInsightInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/**
 * `LifeStyleInnerAdapter`.
 *
 * Two view types: the activity rows, then a trailing settings row. The settings row is part
 * of the list rather than card chrome because the original lets it scroll with the content.
 */
class StudyLifeStyleInnerAdapter(
    private val onSettingsClick: () -> Unit,
) : RecyclerView.Adapter<StudyInnerViewHolder<*>>() {

    private var items: List<StudyDetailLifeStyleItemState> = emptyList()
    private var showSettingRow: Boolean = true

    fun submit(newItems: List<StudyDetailLifeStyleItemState>, showSetting: Boolean) {
        items = newItems
        showSettingRow = showSetting
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size + if (showSettingRow) 1 else 0

    override fun getItemViewType(position: Int) =
        if (position < items.size) TYPE_ITEM else TYPE_SETTING

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyInnerViewHolder<*> =
        if (viewType == TYPE_SETTING) {
            StudyLifeStyleInnerSettingViewHolder(
                parent.inflate(R.layout.study_detail_life_style_inner_item_setting), onSettingsClick)
        } else {
            StudyLifeStyleInnerItemViewHolder(
                parent.inflate(R.layout.study_detail_life_style_inner_item))
        }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: StudyInnerViewHolder<*>, position: Int) {
        if (position < items.size) {
            (holder as StudyInnerViewHolder<StudyDetailLifeStyleItemState>).bind(items[position])
        } else {
            (holder as StudyInnerViewHolder<Unit>).bind(Unit)
        }
    }

    private companion object { const val TYPE_ITEM = 0; const val TYPE_SETTING = 1 }
}

/** the per-time-slot strip inside one life-style row. */
class StudyLifeStyleByTimeAdapter :
    ListAdapter<StudyDetailLifeStyleByTimeState, StudyLifeStyleByTimeViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLifeStyleByTimeViewHolder(
            parent.inflate(R.layout.study_detail_life_style_inner_state_by_time_item))
    override fun onBindViewHolder(holder: StudyLifeStyleByTimeViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `LifeTipsInnerAdapter`. */
class StudyLifeTipsInnerAdapter :
    ListAdapter<StudyDetailLifeTipsItemState, StudyLifeTipsInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLifeTipsInnerViewHolder(parent.inflate(R.layout.study_detail_life_tips_inner_view_holder))
    override fun onBindViewHolder(holder: StudyLifeTipsInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `SmartThingsInnerAdapter` — stub data. */
class StudySmartThingsInnerAdapter :
    ListAdapter<StudyDetailSmartThingsItemState, StudySmartThingsInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudySmartThingsInnerViewHolder(parent.inflate(R.layout.study_detail_st_inner_view_holder))
    override fun onBindViewHolder(holder: StudySmartThingsInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** the precipitation columns. */
class StudyPrecipitationInnerAdapter :
    ListAdapter<StudyDetailPrecipitationItemState, StudyPrecipitationInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyPrecipitationInnerViewHolder(parent.inflate(R.layout.study_detail_precipitation_item))
    override fun onBindViewHolder(holder: StudyPrecipitationInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** the daily rows. */
class StudyDailyInnerAdapter :
    ListAdapter<StudyDetailDailyItemState, StudyDailyInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyDailyInnerViewHolder(parent.inflate(R.layout.study_detail_daily_inner_item))
    override fun onBindViewHolder(holder: StudyDailyInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/**
 * the news/video/today-stories tiles.
 *
 * One adapter serves all four content card types, because they differ only in how many
 * slots the layout has — which is why the original inflates one layout for three of them.
 */
class StudyContentInnerAdapter(
    private val tileLayout: Int,
    private val onClick: (String) -> Unit,
) : ListAdapter<StudyDetailContentItemState, StudyContentInnerViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyContentInnerViewHolder(parent.inflate(tileLayout), onClick)
    override fun onBindViewHolder(holder: StudyContentInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** Corresponds conceptually to `…adapter.card.inner.AirIndexInnerAdapter`. */
class StudyAirIndexInnerAdapter :
    ListAdapter<
        dev.local.weatherstudy.ui.common.detail.state.StudyDetailAirIndexItemState,
        StudyAirIndexInnerViewHolder,
        >(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyAirIndexInnerViewHolder(parent.inflate(R.layout.study_detail_air_index_inner_view_holder))

    override fun onBindViewHolder(holder: StudyAirIndexInnerViewHolder, position: Int) =
        holder.bind(getItem(position))
}
