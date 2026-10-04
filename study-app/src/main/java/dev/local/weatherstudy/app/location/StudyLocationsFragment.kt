package dev.local.weatherstudy.app.location

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.common.location.list.StudyLocationRow
import dev.local.weatherstudy.app.common.location.list.StudyLocationsConcatAdapter
import dev.local.weatherstudy.app.common.location.list.StudyLocationsCurrentButtonAdapter
import dev.local.weatherstudy.app.common.location.list.StudyLocationsDefaultListAdapter
import dev.local.weatherstudy.app.common.location.list.StudyLocationsDescriptionAdapter
import dev.local.weatherstudy.app.common.location.list.StudyLocationsPreciseButtonAdapter
import dev.local.weatherstudy.app.common.location.list.StudyLocationsSelectListAdapter
import dev.local.weatherstudy.app.common.view.StudyScreenToolbar
import dev.local.weatherstudy.app.common.view.applySystemBarPadding
import dev.local.weatherstudy.app.common.view.navigateBackOrRestart
import dev.local.weatherstudy.condition.conditions.checker.StudyCheckLocationPermission
import dev.local.weatherstudy.domain.entity.weather.displayName
import dev.local.weatherstudy.domain.entity.weather.isCurrentLocation
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.usecase.StudyObserveWeatherChange
import dev.local.weatherstudy.domain.usecase.StudyRemoveLocations
import dev.local.weatherstudy.logger.analytics.tracking.StudyLocationsTracking
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTemperatureNotation
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.local.weatherstudy.app.common.R as CommonR
import dev.local.weatherstudy.ui.common.R as UiR

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.location.LocationsFragment
 * (+ `LocationsViewModel`, `LocationsState`, and the 30-odd classes of
 * `app.common.location` it is assembled from)
 *
 * Observed responsibility: the saved-locations list, in two modes that are two different
 * `ConcatAdapter` compositions over the same five adapters:
 *
 * - **browsing** — "use current location" button, "switch to precise" button, the list,
 *   the footer. A tap opens that city; a long press enters selection.
 * - **selecting** — the checkbox list and the footer. The toolbar becomes a count and a
 *   delete action. The device-location entry cannot be selected, so it cannot be deleted.
 *
 * Switching modes swaps the composition rather than toggling views inside one adapter,
 * which is why the original has `LocationsDefaultListAdapter` and
 * `LocationsSelectListAdapter` as separate classes.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyLocationsFragment : Fragment(R.layout.study_locations_fragment) {

    private val viewModel: StudyLocationsViewModel by viewModels()

    @Inject lateinit var checkLocationPermission: StudyCheckLocationPermission

    private var shownSelecting: Boolean? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        val list = view.findViewById<RecyclerView>(R.id.list)
        val empty = view.findViewById<View>(R.id.empty_label)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.itemAnimator = null

        val adapters = StudyLocationsConcatAdapter(
            currentButton = StudyLocationsCurrentButtonAdapter(::addCurrentLocation),
            preciseButton = StudyLocationsPreciseButtonAdapter { openPermissions() },
            defaultList = StudyLocationsDefaultListAdapter(
                onClick = ::openLocation,
                onLongClick = viewModel::startSelecting,
            ),
            selectList = StudyLocationsSelectListAdapter(viewModel::toggle),
            description = StudyLocationsDescriptionAdapter(),
        )
        adapters.description.text = getString(CommonR.string.study_locations_footer)

        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = viewModel.stopSelecting()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        toolbar.back.setOnClickListener {
            if (viewModel.state.value.isSelecting) viewModel.stopSelecting()
            else findNavController().navigateBackOrRestart()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                backCallback.isEnabled = state.isSelecting
                if (shownSelecting != state.isSelecting) {
                    shownSelecting = state.isSelecting
                    list.adapter = if (state.isSelecting) adapters.selecting() else adapters.browsing()
                }

                if (state.isSelecting) {
                    toolbar.title.text = getString(R.string.study_locations_selected, state.selected.size)
                    toolbar.setPrimaryAction(UiR.drawable.study_ic_delete, R.string.study_locations_delete) {
                        viewModel.deleteSelected()
                    }
                } else {
                    toolbar.setTitle(R.string.study_locations_title)
                    toolbar.setPrimaryAction(UiR.drawable.study_ic_add, R.string.study_locations_add) {
                        runCatching { findNavController().navigate(R.id.action_location_to_search) }
                    }
                }

                adapters.defaultList.submitList(state.rows)
                adapters.selectList.submitList(state.rows)
                adapters.currentButton.visible = !state.hasCurrentLocation
                // offered only when the grant is coarse: the forecast is then for an area, not a point
                adapters.preciseButton.visible = state.hasCurrentLocation &&
                    checkLocationPermission.hasForeground() && !checkLocationPermission.hasPrecise()
                empty.visibility = if (state.rows.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        shownSelecting = null
        super.onDestroyView()
    }

    private fun openLocation(key: String) {
        viewModel.open(key) {
            runCatching {
                findNavController().navigate(
                    R.id.action_locations_to_detail,
                    Bundle().apply { putString(ARG_LOCATION_KEY, key) },
                )
            }
        }
    }

    /** the same two-step route the startup chain takes: the grant first, then the fix */
    private fun addCurrentLocation() {
        viewModel.onAddCurrentLocation()
        val action = if (checkLocationPermission.hasForeground()) {
            R.id.action_global_to_get_current
        } else {
            R.id.action_global_to_app_permission
        }
        runCatching { findNavController().navigate(action) }
    }

    /** pushed, not a global action, so the permission screen opens as a viewer with a way back */
    private fun openPermissions() {
        runCatching { findNavController().navigate(R.id.app_permission) }
    }

    private companion object {
        const val ARG_LOCATION_KEY = "location_key"
    }
}

/** Corresponds conceptually to `…app.location.state.LocationsState`. */
data class StudyLocationsState(
    val rows: List<StudyLocationRow> = emptyList(),
    val isSelecting: Boolean = false,
    val selected: Set<String> = emptySet(),
    val hasCurrentLocation: Boolean = false,
)

/** Corresponds conceptually to `…app.location.LocationsViewModel`. */
@HiltViewModel
class StudyLocationsViewModel @Inject constructor(
    observeWeatherChange: StudyObserveWeatherChange,
    private val settingsRepo: StudySettingsRepo,
    private val removeLocations: StudyRemoveLocations,
    private val temperatureNotation: StudyTemperatureNotation,
    private val tracking: StudyLocationsTracking,
) : ViewModel() {

    private val selection = MutableStateFlow<Set<String>?>(null)

    val state: StateFlow<StudyLocationsState> = combine(
        observeWeatherChange(),
        settingsRepo.observeTempScale(),
        selection,
    ) { weathers, tempScale, selected ->
        StudyLocationsState(
            rows = weathers.map { weather ->
                val condition = weather.currentObservation.condition
                StudyLocationRow(
                    key = weather.location.key,
                    cityName = weather.location.displayName,
                    temperatureText = temperatureNotation.format(condition.temperature, tempScale),
                    conditionText = condition.weatherText,
                    highLowText = temperatureNotation.formatHighLow(condition.maxTemp, condition.minTemp, tempScale),
                    iconNum = condition.iconNum,
                    isCurrentLocation = weather.location.isCurrentLocation,
                    label = weather.location.label,
                    isSelected = selected?.contains(weather.location.key) == true,
                )
            },
            isSelecting = selected != null,
            selected = selected.orEmpty(),
            hasCurrentLocation = weathers.any { it.location.isCurrentLocation },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StudyLocationsState())

    init {
        tracking.onEnter()
    }

    fun startSelecting(key: String) {
        val current = state.value.rows.firstOrNull { it.key == key }
        // a long press on the device-location entry still enters the mode; it just selects nothing
        selection.value = if (current?.isCurrentLocation == true) emptySet() else setOf(key)
    }

    fun stopSelecting() {
        selection.value = null
    }

    fun toggle(key: String, checked: Boolean) {
        val selected = selection.value ?: return
        selection.value = if (checked) selected + key else selected - key
    }

    fun deleteSelected() {
        val keys = selection.value.orEmpty().toList()
        selection.value = null
        if (keys.isEmpty()) return
        viewModelScope.launch {
            tracking.onDelete(keys.size)
            removeLocations(keys)
        }
    }

    /** opening a city makes it the favourite: the page the forecast opens on */
    fun open(key: String, onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepo.setFavoriteLocation(key)
            onDone()
        }
    }

    fun onAddCurrentLocation() = tracking.onAddCurrentLocation()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
