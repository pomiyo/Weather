package dev.local.weatherstudy.app.common.location.addlabel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.common.R
import dev.local.weatherstudy.domain.type.StudyLocationsLabelType

/**
 * Educational reconstruction of
 * com.samsung.android.weather.app.common.location.addlabel.LocationsAddLabelTypeListAdapter
 * and `LocationsAddLabelTypeViewHolder`
 *
 * The label picker in the add-label dialog. Fixed list, so a plain adapter over the
 * `StudyLocationsLabelType` constants rather than a diffed one.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
class StudyLocationsAddLabelTypeListAdapter(
    private val onPick: (Int) -> Unit,
) : RecyclerView.Adapter<StudyLocationsAddLabelTypeViewHolder>() {

    private val types = listOf(
        StudyLocationsLabelType.HOME,
        StudyLocationsLabelType.WORK,
        StudyLocationsLabelType.SCHOOL,
        StudyLocationsLabelType.CUSTOM,
    )

    var selected: Int = StudyLocationsLabelType.NONE
        set(value) { field = value; notifyDataSetChanged() }

    override fun getItemCount() = types.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLocationsAddLabelTypeViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.study_locations_add_label_type_view_holder, parent, false),
            onPick,
        )

    override fun onBindViewHolder(holder: StudyLocationsAddLabelTypeViewHolder, position: Int) =
        holder.bind(types[position], types[position] == selected)
}

/** `LocationsAddLabelTypeViewHolder`. */
class StudyLocationsAddLabelTypeViewHolder(
    itemView: View,
    private val onPick: (Int) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    private val label: TextView = itemView.findViewById(R.id.label_type_name)
    private val tick: View = itemView.findViewById(R.id.label_type_tick)

    fun bind(labelType: Int, isSelected: Boolean) {
        label.setText(
            when (labelType) {
                StudyLocationsLabelType.HOME -> R.string.study_label_home
                StudyLocationsLabelType.WORK -> R.string.study_label_work
                StudyLocationsLabelType.SCHOOL -> R.string.study_label_school
                else -> R.string.study_label_custom
            },
        )
        tick.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        itemView.setOnClickListener { onPick(labelType) }
    }
}
