package dev.local.weatherstudy.app.detail.adapter.header

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.resource.StudyWeatherIcons

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.adapter.DetailPagerAdapter
 * (+ `DetailTopInfoViewHolder`)
 *
 * Observed responsibility: the collapsing header is a pager with **one page per saved
 * city**. Swiping it is how the user changes location; the card list below is a single
 * list that re-renders for whichever page is selected, rather than one list per page.
 *
 * Each page binds a `StudyDetailTopInfoState` and nothing else — the page knows nothing
 * of cards, which is what lets the header collapse independently of them.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailHeaderAdapter :
    ListAdapter<StudyDetailItemState, StudyDetailHeaderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyDetailHeaderViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.study_detail_header_page, parent, false),
        )

    override fun onBindViewHolder(holder: StudyDetailHeaderViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<StudyDetailItemState>() {
            override fun areItemsTheSame(a: StudyDetailItemState, b: StudyDetailItemState) = a.key == b.key
            override fun areContentsTheSame(a: StudyDetailItemState, b: StudyDetailItemState) =
                a.topInfo == b.topInfo
        }
    }
}

/** Corresponds conceptually to `…detail.adapter.DetailTopInfoViewHolder`. */
class StudyDetailHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val pin: ImageView = itemView.findViewById(R.id.header_current_pin)
    private val city: TextView = itemView.findViewById(R.id.header_city)
    private val icon: ImageView = itemView.findViewById(R.id.header_icon)
    private val temperature: TextView = itemView.findViewById(R.id.header_temperature)
    private val condition: TextView = itemView.findViewById(R.id.header_condition)
    private val highLow: TextView = itemView.findViewById(R.id.header_high_low)
    private val feelsLike: TextView = itemView.findViewById(R.id.header_feels_like)

    fun bind(item: StudyDetailItemState) {
        val top = item.topInfo
        pin.visibility = if (top.isCurrentLocation) View.VISIBLE else View.GONE
        city.text = top.cityName
        icon.setImageResource(StudyWeatherIcons.iconRes(top.iconNum))
        temperature.text = top.temperature
        condition.text = top.weatherText
        highLow.text = top.highLow
        feelsLike.text = itemView.context.getString(R.string.study_feels_like, top.feelsLike)
    }
}
