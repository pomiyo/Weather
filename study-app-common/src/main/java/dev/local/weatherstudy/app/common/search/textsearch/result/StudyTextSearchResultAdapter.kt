package dev.local.weatherstudy.app.common.search.textsearch.result

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.common.R
import dev.local.weatherstudy.domain.entity.weather.StudyLocation

/**
 * Educational reconstruction of
 * com.samsung.android.weather.app.common.search.textsearch.result.TextSearchResultAdapter
 * and `TextSearchResultViewHolder`
 *
 * The city-search result list. Diffs on the provider's location key, so retyping a query that
 * returns overlapping results re-binds only what changed.
 *
 * `isDisputedArea` suppresses the country name — see `StudyProviderAReviseDisputedArea`, which
 * is where that flag is set during parsing.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
class StudyTextSearchResultAdapter(
    private val onPick: (StudyLocation) -> Unit,
) : ListAdapter<StudyLocation, StudyTextSearchResultViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyTextSearchResultViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.study_search_result_item, parent, false),
            onPick,
        )

    override fun onBindViewHolder(holder: StudyTextSearchResultViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<StudyLocation>() {
            override fun areItemsTheSame(a: StudyLocation, b: StudyLocation) = a.key == b.key
            override fun areContentsTheSame(a: StudyLocation, b: StudyLocation) = a == b
        }
    }
}

/** `TextSearchResultViewHolder`. */
class StudyTextSearchResultViewHolder(
    itemView: View,
    private val onPick: (StudyLocation) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    private val city: TextView = itemView.findViewById(R.id.result_city)
    private val region: TextView = itemView.findViewById(R.id.result_region)

    fun bind(location: StudyLocation) {
        city.text = location.cityName
        region.text = listOfNotNull(
            location.stateName.takeIf { it.isNotEmpty() },
            // a disputed territory shows city and state only
            location.countryName.takeIf { it.isNotEmpty() && !location.isDisputedArea },
        ).joinToString(", ")
        region.visibility = if (region.text.isEmpty()) View.GONE else View.VISIBLE
        itemView.setOnClickListener { onPick(location) }
    }
}
