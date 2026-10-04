package dev.local.weatherstudy.app.common.location.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.common.R

/**
 * Educational reconstruction of the five locations adapters in
 * com.samsung.android.weather.app.common.location.list.*
 *
 * ### Why five adapters and not one with view types
 *
 * The original splits the locations screen into `LocationsCurrentButtonAdapter`,
 * `LocationsPreciseButtonAdapter`, `LocationsDefaultListAdapter`,
 * `LocationsSelectListAdapter` and `LocationsDescriptionAdapter`, concatenated.
 *
 * The reason is diffing: the "use current location" button and the precise-location prompt
 * appear and disappear independently of the city list. As separate adapters in a
 * `ConcatAdapter`, showing the prompt inserts one row without touching the city list's diff —
 * where a single adapter with view types would re-diff everything.
 *
 * `Default` vs `Select` is edit mode: the same cities, with checkboxes and no navigation.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
data class StudyLocationRow(
    val key: String,
    val cityName: String,
    val temperatureText: String = "",
    val conditionText: String = "",
    val highLowText: String = "",
    val iconNum: Int = 0,
    val isCurrentLocation: Boolean = false,
    val label: String = "",
    val isSelected: Boolean = false,
)

private fun <T> valueDiff() = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(a: T & Any, b: T & Any) = a == b
    override fun areContentsTheSame(a: T & Any, b: T & Any) = a == b
}

private fun ViewGroup.inflate(res: Int) = LayoutInflater.from(context).inflate(res, this, false)

/** `LocationsDefaultListAdapter` — the normal, navigable city list. */
class StudyLocationsDefaultListAdapter(
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit,
) : ListAdapter<StudyLocationRow, StudyLocationsDefaultListViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLocationsDefaultListViewHolder(
            parent.inflate(R.layout.study_locations_list_item), onClick, onLongClick)
    override fun onBindViewHolder(holder: StudyLocationsDefaultListViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `LocationsDefaultListViewHolder`. */
class StudyLocationsDefaultListViewHolder(
    itemView: View,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit,
) : RecyclerView.ViewHolder(itemView) {
    private val city: TextView = itemView.findViewById(R.id.location_city)
    private val temp: TextView = itemView.findViewById(R.id.location_temperature)
    private val cond: TextView = itemView.findViewById(R.id.location_condition)
    private val range: TextView = itemView.findViewById(R.id.location_high_low)
    private val pin: View = itemView.findViewById(R.id.location_current_pin)
    private val icon: android.widget.ImageView = itemView.findViewById(R.id.location_icon)

    fun bind(row: StudyLocationRow) {
        city.text = if (row.label.isNotEmpty()) row.label else row.cityName
        temp.text = row.temperatureText
        cond.text = row.conditionText
        range.text = row.highLowText
        icon.setImageResource(
            dev.local.weatherstudy.ui.common.resource.StudyWeatherIcons.iconRes(row.iconNum),
        )
        pin.visibility = if (row.isCurrentLocation) View.VISIBLE else View.GONE
        itemView.setOnClickListener { onClick(row.key) }
        itemView.setOnLongClickListener { onLongClick(row.key); true }
    }
}

/** `LocationsSelectListAdapter` — edit mode: checkboxes, no navigation. */
class StudyLocationsSelectListAdapter(
    private val onToggle: (String, Boolean) -> Unit,
) : ListAdapter<StudyLocationRow, StudyLocationsSelectListViewHolder>(valueDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLocationsSelectListViewHolder(
            parent.inflate(R.layout.study_locations_list_select_item), onToggle)
    override fun onBindViewHolder(holder: StudyLocationsSelectListViewHolder, position: Int) =
        holder.bind(getItem(position))
}

/** `LocationsSelectListViewHolder`. */
class StudyLocationsSelectListViewHolder(
    itemView: View,
    private val onToggle: (String, Boolean) -> Unit,
) : RecyclerView.ViewHolder(itemView) {
    private val check: CheckBox = itemView.findViewById(R.id.location_check)
    private val city: TextView = itemView.findViewById(R.id.location_city)
    private val temp: TextView = itemView.findViewById(R.id.location_temperature)

    fun bind(row: StudyLocationRow) {
        city.text = if (row.label.isNotEmpty()) row.label else row.cityName
        temp.text = row.temperatureText
        check.setOnCheckedChangeListener(null)
        check.isChecked = row.isSelected
        // the device-location entry cannot be deleted, so it cannot be selected
        check.isEnabled = !row.isCurrentLocation
        check.setOnCheckedChangeListener { _, v -> onToggle(row.key, v) }
        itemView.setOnClickListener { check.isChecked = !check.isChecked }
    }
}

/** `LocationsCurrentButtonAdapter` — a one-row adapter so it can appear independently. */
class StudyLocationsCurrentButtonAdapter(
    private val onClick: () -> Unit,
) : RecyclerView.Adapter<StudyLocationsCurrentButtonViewHolder>() {
    var visible: Boolean = false
        set(value) { field = value; notifyDataSetChanged() }
    override fun getItemCount() = if (visible) 1 else 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLocationsCurrentButtonViewHolder(
            parent.inflate(R.layout.study_locations_current_location), onClick)
    override fun onBindViewHolder(holder: StudyLocationsCurrentButtonViewHolder, position: Int) =
        holder.bind()
}

/** `LocationsCurrentButtonViewHolder`. */
class StudyLocationsCurrentButtonViewHolder(itemView: View, onClick: () -> Unit) :
    RecyclerView.ViewHolder(itemView) {
    init { itemView.setOnClickListener { onClick() } }
    private val label: TextView = itemView.findViewById(R.id.current_location_label)
    fun bind() { label.setText(R.string.study_use_current_location) }
}

/**
 * `LocationsPreciseButtonAdapter` — the "switch to precise location" prompt.
 *
 * Shown only when `StudyCheckApproximateLocation` reports a coarse-only grant, which is why
 * the original has two separate enter-detail counters.
 */
class StudyLocationsPreciseButtonAdapter(
    private val onClick: () -> Unit,
) : RecyclerView.Adapter<StudyLocationsPreciseButtonViewHolder>() {
    var visible: Boolean = false
        set(value) { field = value; notifyDataSetChanged() }
    override fun getItemCount() = if (visible) 1 else 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLocationsPreciseButtonViewHolder(
            parent.inflate(R.layout.study_locations_precise_button), onClick)
    override fun onBindViewHolder(holder: StudyLocationsPreciseButtonViewHolder, position: Int) =
        holder.bind()
}

/** `LocationsPreciseButtonViewHolder`. */
class StudyLocationsPreciseButtonViewHolder(itemView: View, onClick: () -> Unit) :
    RecyclerView.ViewHolder(itemView) {
    init { itemView.setOnClickListener { onClick() } }
    private val label: TextView = itemView.findViewById(R.id.precise_button_label)
    fun bind() { label.setText(R.string.study_use_precise_location) }
}

/** `LocationsDescriptionAdapter` — the footer explaining the location cap. */
class StudyLocationsDescriptionAdapter :
    RecyclerView.Adapter<StudyLocationsDescriptionViewHolder>() {
    var text: String = ""
        set(value) { field = value; notifyDataSetChanged() }
    override fun getItemCount() = if (text.isEmpty()) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyLocationsDescriptionViewHolder(parent.inflate(R.layout.study_locations_footer))
    override fun onBindViewHolder(holder: StudyLocationsDescriptionViewHolder, position: Int) =
        holder.bind(text)
}

/** `LocationsDescriptionViewHolder`. */
class StudyLocationsDescriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val label: TextView = itemView.findViewById(R.id.footer_label)
    fun bind(text: String) { label.text = text }
}

/**
 * The concatenation, in the original's order.
 *
 * This is the whole point of the five-adapter split — each section updates on its own.
 */
class StudyLocationsConcatAdapter(
    val currentButton: StudyLocationsCurrentButtonAdapter,
    val preciseButton: StudyLocationsPreciseButtonAdapter,
    val defaultList: StudyLocationsDefaultListAdapter,
    val selectList: StudyLocationsSelectListAdapter,
    val description: StudyLocationsDescriptionAdapter,
) {
    /** normal mode */
    fun browsing() = ConcatAdapter(currentButton, preciseButton, defaultList, description)

    /** edit mode — the buttons go away, the list gains checkboxes */
    fun selecting() = ConcatAdapter(selectList, description)
}
