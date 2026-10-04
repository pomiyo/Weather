package dev.local.weatherstudy.app.main

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.common.view.StudyScreenToolbar
import dev.local.weatherstudy.app.common.view.applySystemBarPadding
import dev.local.weatherstudy.app.common.view.restartGraph
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyStatusRepo
import dev.local.weatherstudy.domain.type.StudyKeys
import dev.local.weatherstudy.domain.type.StudySettingValue
import dev.local.weatherstudy.sync.usecase.StudyStartCurrentLocationAddition
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.main.GetCurrentFragment
 *
 * Observed responsibility: the "finding your location" screen the condition chain routes
 * to when no location is saved yet. It does **not** fetch anything itself. It enqueues
 * `AddCurrentLocationWorker` and watches the shared status row the worker writes under
 * `StudyKeys.CURRENT` — the same row a widget's "add current location" tap would drive.
 * When the worker has saved a location, the screen re-enters the graph and the chain,
 * now satisfied, goes to the forecast.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyGetCurrentFragment : Fragment(R.layout.study_message_fragment) {

    private val viewModel: StudyGetCurrentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        toolbar.title.visibility = View.GONE
        toolbar.back.setOnClickListener { requireActivity().finish() }

        val progress = view.findViewById<View>(R.id.message_progress)
        val icon = view.findViewById<View>(R.id.message_icon)
        val title = view.findViewById<TextView>(R.id.message_title)
        val body = view.findViewById<TextView>(R.id.message_body)
        val buttons = view.findViewById<View>(R.id.message_buttons)
        val retry = view.findViewById<Button>(R.id.message_primary)
        val search = view.findViewById<Button>(R.id.message_secondary)

        retry.setText(R.string.study_locating_retry)
        retry.setOnClickListener { viewModel.start() }
        search.setText(R.string.study_permission_search)
        search.setOnClickListener {
            runCatching { findNavController().navigate(R.id.action_global_to_search) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                val failed = state == StudyGetCurrentState.FAILED
                progress.visibility = if (failed) View.GONE else View.VISIBLE
                icon.alpha = if (failed) 1f else ICON_BUSY_ALPHA
                buttons.visibility = if (failed) View.VISIBLE else View.INVISIBLE
                title.setText(if (failed) R.string.study_locating_failed_title else R.string.study_locating_title)
                body.setText(if (failed) R.string.study_locating_failed_body else R.string.study_locating_body)
                if (state == StudyGetCurrentState.DONE) findNavController().restartGraph()
            }
        }
    }
}

/** the pin stays visible inside the spinner ring, dimmed while the fix is pending */
private const val ICON_BUSY_ALPHA = 0.6f

enum class StudyGetCurrentState { LOCATING, FAILED, DONE }

/** Corresponds conceptually to the state the original's `GetCurrentFragment` observes. */
@HiltViewModel
class StudyGetCurrentViewModel @Inject constructor(
    private val startCurrentLocationAddition: StudyStartCurrentLocationAddition,
    private val statusRepo: StudyStatusRepo,
    private val settingsRepo: StudySettingsRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyGetCurrentState.LOCATING)
    val state: StateFlow<StudyGetCurrentState> = _state.asStateFlow()

    private var attempt: Job? = null

    init {
        start()
    }

    fun start() {
        attempt?.cancel()
        _state.value = StudyGetCurrentState.LOCATING
        attempt = viewModelScope.launch {
            // the status row persists: mark it running first so an old result is not read as this one's
            statusRepo.setStatus(StudyKeys.CURRENT, StudySettingValue.RefreshStatus.RUNNING, 0)
            startCurrentLocationAddition()

            val watchdog = launch {
                // the worker waits for a network; a screen cannot wait indefinitely
                delay(TIMEOUT_MILLIS)
                _state.value = StudyGetCurrentState.FAILED
            }
            statusRepo.getStatus(StudyKeys.CURRENT).collect { status ->
                when (status) {
                    StudySettingValue.RefreshStatus.FAILED -> {
                        watchdog.cancel()
                        _state.value = StudyGetCurrentState.FAILED
                    }
                    StudySettingValue.RefreshStatus.DONE ->
                        if (settingsRepo.getFavoriteLocation().isNotEmpty()) {
                            watchdog.cancel()
                            _state.value = StudyGetCurrentState.DONE
                        }
                }
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 45_000L
    }
}
