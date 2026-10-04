package dev.local.weatherstudy.app.search

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
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
import dev.local.weatherstudy.app.common.search.textsearch.result.StudyTextSearchResultAdapter
import dev.local.weatherstudy.app.common.view.applySystemBarPadding
import dev.local.weatherstudy.app.common.view.navigateBackOrRestart
import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.usecase.StudyAddLocation
import dev.local.weatherstudy.domain.usecase.StudyAddLocationException
import dev.local.weatherstudy.domain.usecase.StudyExceedNumOfLocation
import dev.local.weatherstudy.domain.usecase.StudySearchLocations
import dev.local.weatherstudy.logger.analytics.tracking.StudySearchTracking
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.search.SearchFragment
 * (+ `app.common.search.textsearch.TextSearchFragment`, `TextSearchViewModel`,
 * `TextSearchState` and the result adapter)
 *
 * Observed responsibility: text search. The original's search destination hosts two
 * modes — this one, and a map search with themed places. The text mode is reconstructed
 * in full; the map mode is the project's one remaining stub (it needs the Maps SDK and
 * a 25-class tree of its own — see `reports/reconstruction-coverage.md` §5).
 *
 * The flow is three use cases in a row, none of them owned by this screen:
 * `SearchLocations` (query -> places), `ExceedNumOfLocation` (the ten-city limit), and
 * `AddLocation` (fetch the forecast for the pick, then save it). A place only becomes a
 * saved location once its forecast has actually been fetched.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudySearchFragment : Fragment(R.layout.study_search_fragment) {

    private val viewModel: StudySearchViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val field = view.findViewById<EditText>(R.id.search_field)
        val clear = view.findViewById<View>(R.id.search_clear)
        val list = view.findViewById<RecyclerView>(R.id.list)
        val progress = view.findViewById<View>(R.id.search_progress)
        val message = view.findViewById<TextView>(R.id.empty_label)

        val adapter = StudyTextSearchResultAdapter { location ->
            hideKeyboard(field)
            viewModel.pick(location)
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        view.findViewById<View>(R.id.screen_back).setOnClickListener {
            hideKeyboard(field)
            findNavController().navigateBackOrRestart()
        }
        clear.setOnClickListener { field.setText("") }
        field.doAfterTextChanged { text ->
            clear.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            viewModel.onQuery(text?.toString().orEmpty())
        }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(field)
                viewModel.onQuery(field.text.toString(), immediate = true)
            }
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
        if (savedInstanceState == null) showKeyboard(field)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                adapter.submitList(state.results)
                list.visibility = if (state.results.isEmpty()) View.GONE else View.VISIBLE
                progress.visibility = if (state.isBusy) View.VISIBLE else View.GONE
                list.alpha = if (state.adding != null) DIMMED else 1f
                val text = when {
                    state.adding != null -> getString(R.string.study_search_adding, state.adding)
                    state.isBusy -> ""
                    state.failed -> getString(R.string.study_search_failed)
                    state.query.length < StudySearchViewModel.MIN_QUERY_LENGTH ->
                        getString(R.string.study_search_prompt)
                    state.results.isEmpty() -> getString(R.string.study_search_no_results, state.query)
                    else -> ""
                }
                message.text = text
                message.visibility = if (text.isEmpty() || state.adding != null && state.results.isNotEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is StudySearchEvent.Added -> runCatching {
                        findNavController().navigate(
                            R.id.action_search_to_detail,
                            Bundle().apply { putString(ARG_LOCATION_KEY, event.key) },
                        )
                    }
                    is StudySearchEvent.AddFailed ->
                        toast(getString(R.string.study_search_add_failed, event.cityName))
                    StudySearchEvent.LimitReached ->
                        toast(getString(R.string.study_locations_limit, StudyExceedNumOfLocation.MAX_LOCATION_COUNT))
                }
            }
        }
    }

    private fun toast(text: String) = Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()

    private fun showKeyboard(field: EditText) {
        field.requestFocus()
        field.post {
            (field.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard(field: EditText) {
        (field.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(field.windowToken, 0)
    }

    private companion object {
        const val ARG_LOCATION_KEY = "location_key"
        const val DIMMED = 0.4f
    }
}

/** Corresponds conceptually to `…textsearch.state.TextSearchState`. */
data class StudySearchState(
    val query: String = "",
    val results: List<StudyLocation> = emptyList(),
    val isSearching: Boolean = false,
    val failed: Boolean = false,
    /** the city whose forecast is being fetched before it is saved */
    val adding: String? = null,
) {
    val isBusy: Boolean get() = isSearching || adding != null
}

sealed interface StudySearchEvent {
    data class Added(val key: String) : StudySearchEvent
    data class AddFailed(val cityName: String) : StudySearchEvent
    data object LimitReached : StudySearchEvent
}

/** Corresponds conceptually to `…textsearch.TextSearchViewModel`. */
@HiltViewModel
class StudySearchViewModel @Inject constructor(
    private val searchLocations: StudySearchLocations,
    private val addLocation: StudyAddLocation,
    private val exceedNumOfLocation: StudyExceedNumOfLocation,
    private val settingsRepo: StudySettingsRepo,
    private val tracking: StudySearchTracking,
) : ViewModel() {

    private val _state = MutableStateFlow(StudySearchState())
    val state: StateFlow<StudySearchState> = _state.asStateFlow()

    private val _events = Channel<StudySearchEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var search: Job? = null

    init {
        tracking.onEnter()
    }

    fun onQuery(text: String, immediate: Boolean = false) {
        val query = text.trim()
        if (query == _state.value.query && !immediate) return
        search?.cancel()
        _state.update { it.copy(query = query, failed = false) }
        if (query.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        search = viewModelScope.launch {
            // wait for a pause in typing: one request per word, not one per letter
            if (!immediate) delay(DEBOUNCE_MILLIS)
            _state.update { it.copy(isSearching = true) }
            tracking.onQuery(query.length)
            val found = runCatching { searchLocations(query).toList().flatten() }
            if (found.getOrNull()?.isEmpty() == true) tracking.onNoResults()
            _state.update {
                it.copy(
                    results = found.getOrDefault(emptyList()),
                    isSearching = false,
                    failed = found.isFailure,
                )
            }
        }
    }

    fun pick(location: StudyLocation) {
        if (_state.value.adding != null) return
        viewModelScope.launch {
            tracking.onResultSelected(_state.value.results.indexOf(location))
            if (exceedNumOfLocation()) {
                _events.send(StudySearchEvent.LimitReached)
                return@launch
            }
            _state.update { it.copy(adding = location.cityName) }
            val result = addLocation(location)
            _state.update { it.copy(adding = null) }

            val savedKey = result.getOrNull()?.location?.key
                // already saved is not an error to the user: open the one they have
                ?: location.key.takeIf { result.exceptionOrNull() is StudyAddLocationException.AlreadyExists }
            if (savedKey != null) {
                settingsRepo.setFavoriteLocation(savedKey)
                _events.send(StudySearchEvent.Added(savedKey))
            } else {
                _events.send(StudySearchEvent.AddFailed(location.cityName))
            }
        }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val DEBOUNCE_MILLIS = 350L
    }
}
