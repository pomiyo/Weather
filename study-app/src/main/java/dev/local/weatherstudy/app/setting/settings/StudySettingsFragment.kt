package dev.local.weatherstudy.app.setting.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import dev.local.weatherstudy.network.gateway.StudyGatewayService
import dev.local.weatherstudy.network.gateway.StudyGatewayServiceStore
import dev.local.weatherstudy.sync.usecase.StudyStartForegroundRefresh
import dev.local.weatherstudy.sync.usecase.StudyForegroundRefreshRequest
import kotlinx.coroutines.flow.MutableStateFlow
import android.widget.Toast
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
import dev.local.weatherstudy.app.common.view.navigateBackOrRestart
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.type.StudyAutoRefresh
import dev.local.weatherstudy.domain.type.StudySettingValue
import dev.local.weatherstudy.domain.usecase.StudyUpdateAutoRefreshInterval
import dev.local.weatherstudy.domain.usecase.StudyUpdateTempScale
import dev.local.weatherstudy.logger.analytics.tracking.StudySettingTracking
import dev.local.weatherstudy.sync.usecase.StudyStartBackgroundRefresh
import dev.local.weatherstudy.ui.common.usecase.notation.StudyAutoRefreshNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTemperatureNotation
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.local.weatherstudy.ui.common.R as UiR

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.setting.settings.SettingsFragment
 * (+ `SettingsViewModel`, `SettingsState`, `SettingsIntent` and the per-row
 * preference classes — 60-odd classes in `app.setting.settings`)
 *
 * Observed responsibility: the entry of the nested settings graph. Two rows change a
 * stored setting, three navigate deeper.
 *
 * - **Temperature unit** writes `COL_SETTING_TEMP_SCALE`. Nothing is converted at the
 *   time: every screen formats from the stored Celsius value through the notation layer,
 *   so they all follow the change the next time they render.
 * - **Auto refresh** writes the interval AND re-arms the periodic work, because a setting
 *   WorkManager has not been told about changes nothing.
 *
 * The original draws these with the SESL preference fork; the reconstruction inflates
 * plain rows so it does not depend on an unpublished library.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudySettingsFragment : Fragment(R.layout.study_settings_fragment) {

    private val viewModel: StudySettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        toolbar.setTitle(R.string.study_settings_title)
        toolbar.back.setOnClickListener { findNavController().navigateBackOrRestart() }

        val groups = view.findViewById<ViewGroup>(R.id.settings_rows)

        val display = groups.addCard()
        val unit = display.addRow(R.string.study_settings_unit) { pickUnit() }
        val refresh = display.addRow(R.string.study_settings_auto_refresh) { pickInterval() }
        // A DEVIATION, and a deliberate one. The original routes between its five backends
        // by country code (ForecastProvider.dispatchByCountryCode) and never offers the
        // user a choice. This reconstruction cannot reach any of those, so the same
        // mechanism points at public keyless services instead - and being able to swap the
        // source of the numbers is what makes a disagreement with the original readable.
        val service = display.addRow(R.string.study_settings_service) { pickService() }

        val content = groups.addCard()
        content.addRow(R.string.study_settings_activities) {
            // not an action of the nested settings graph: the destination sits in the parent
            navigate(R.id.lifestyle_setting)
        }.setText(R.string.study_settings_activities_summary)
        content.addRow(R.string.study_settings_permissions) {
            navigate(R.id.action_settings_to_permission)
        }.setText(R.string.study_settings_permissions_summary)

        val about = groups.addCard()
        about.addRow(R.string.study_settings_terms) { navigate(R.id.action_settings_to_eula) }
            .visibility = View.GONE
        about.addRow(R.string.study_settings_about) { navigate(R.id.action_settings_to_about) }
            .setText(R.string.study_settings_about_summary)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                unit.setText(
                    if (state.tempScale == StudyTemperatureNotation.SCALE_FAHRENHEIT) {
                        R.string.study_settings_unit_fahrenheit
                    } else {
                        R.string.study_settings_unit_celsius
                    },
                )
                refresh.text = state.intervalText
                service.text = state.service.label
            }
        }
    }

    private fun pickUnit() {
        val scales = intArrayOf(StudyTemperatureNotation.SCALE_CELSIUS, StudyTemperatureNotation.SCALE_FAHRENHEIT)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.study_settings_unit)
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.study_settings_unit_celsius),
                    getString(R.string.study_settings_unit_fahrenheit),
                ),
                scales.indexOf(viewModel.state.value.tempScale).coerceAtLeast(0),
            ) { dialog, which ->
                viewModel.setTempScale(scales[which])
                dialog.dismiss()
            }
            .show()
            .anchorLow()
    }

    private fun pickInterval() {
        val intervals = StudySettingsViewModel.INTERVALS
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.study_settings_auto_refresh)
            .setSingleChoiceItems(
                intervals.map(viewModel::describe).toTypedArray(),
                intervals.indexOf(viewModel.state.value.interval).coerceAtLeast(0),
            ) { dialog, which ->
                viewModel.setInterval(intervals[which])
                dialog.dismiss()
            }
            .show()
            .anchorLow()
    }

    private fun pickService() {
        val services = StudyGatewayService.entries
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.study_settings_service)
            .setSingleChoiceItems(
                services.map { it.label }.toTypedArray(),
                services.indexOf(viewModel.state.value.service).coerceAtLeast(0),
            ) { dialog, which ->
                viewModel.setService(services[which])
                dialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.study_settings_service_switched, services[which].label),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            .show()
            .anchorLow()
    }

    private fun navigate(actionId: Int) {
        runCatching { findNavController().navigate(actionId) }
    }

    /** a rounded group; rows added to it are separated by hairlines */
    private fun ViewGroup.addCard(): ViewGroup {
        val card = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundResource(UiR.drawable.study_oneui_card)
            clipToOutline = true
        }
        addView(
            card,
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = (GROUP_GAP_DP * resources.displayMetrics.density).toInt() },
        )
        return card
    }

    /** inflate one row and hand back its summary line, which is what callers update */
    private fun ViewGroup.addRow(titleRes: Int, onClick: () -> Unit): TextView {
        val density = resources.displayMetrics.density
        if (childCount > 0) {
            val divider = View(context).apply {
                setBackgroundColor(context.getColor(UiR.color.study_oneui_divider))
            }
            addView(
                divider,
                ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, density.toInt().coerceAtLeast(1))
                    .apply { marginStart = (DIVIDER_INSET_DP * density).toInt(); marginEnd = marginStart },
            )
        }
        val row = LayoutInflater.from(context).inflate(R.layout.study_settings_row, this, false)
        row.findViewById<TextView>(R.id.row_title).setText(titleRes)
        row.setOnClickListener { onClick() }
        addView(row)
        return row.findViewById(R.id.row_summary)
    }

    /** One UI anchors its dialogs low, within thumb reach */
    private fun AlertDialog.anchorLow(): AlertDialog = apply {
        window?.setGravity(android.view.Gravity.BOTTOM)
    }

    private companion object {
        const val GROUP_GAP_DP = 12
        const val DIVIDER_INSET_DP = 24
    }

}

/** Corresponds conceptually to `…setting.settings.state.SettingsState`. */
data class StudySettingsState(
    val service: StudyGatewayService = StudyGatewayService.DEFAULT,
    val tempScale: Int = StudyTemperatureNotation.SCALE_CELSIUS,
    val interval: Int = StudySettingValue.AutoRefreshInterval.EVERY_3HOUR,
    val intervalText: String = "",
)

/** Corresponds conceptually to `…setting.settings.SettingsViewModel`. */
@HiltViewModel
class StudySettingsViewModel @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
    private val updateTempScale: StudyUpdateTempScale,
    private val updateAutoRefreshInterval: StudyUpdateAutoRefreshInterval,
    private val startBackgroundRefresh: StudyStartBackgroundRefresh,
    private val startForegroundRefresh: StudyStartForegroundRefresh,
    private val services: StudyGatewayServiceStore,
    private val autoRefreshNotation: StudyAutoRefreshNotation,
    private val tracking: StudySettingTracking,
) : ViewModel() {

    /** the active service, mirrored into a flow so the row updates the moment it changes */
    private val activeService = MutableStateFlow(services.service)

    val state: StateFlow<StudySettingsState> = combine(
        activeService,
        settingsRepo.observeTempScale(),
        settingsRepo.observeAutoRefresh(),
        settingsRepo.observeAutoRefreshInterval(),
    ) { service, tempScale, autoRefresh, interval ->
        // "off" is stored as a flag beside the interval, and shown as the first interval choice
        val effective = if (autoRefresh == StudySettingValue.OFF) {
            StudySettingValue.AutoRefreshInterval.NONE
        } else {
            interval
        }
        StudySettingsState(
            service = service,
            tempScale = tempScale,
            interval = effective,
            intervalText = describe(effective),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StudySettingsState())

    init {
        tracking.onEnter()
    }

    fun describe(interval: Int): String = autoRefreshNotation.format(interval)

    fun setTempScale(scale: Int) {
        viewModelScope.launch {
            tracking.onTempScaleChanged(scale)
            updateTempScale(scale)
        }
    }

    /**
     * Switching the service refetches everything, immediately.
     *
     * The stored forecast is per location, not per service, so leaving the cache in place
     * would show one service's numbers under the other's attribution until the next
     * scheduled refresh - the one state that would make the comparison lie. The refresh is
     * the same foreground one pull-to-refresh uses, so every saved city is re-fetched, not
     * just the visible one.
     */
    fun setService(service: StudyGatewayService) {
        viewModelScope.launch {
            services.service = service
            activeService.value = service
            startForegroundRefresh(StudyForegroundRefreshRequest(StudyAutoRefresh.From.SETTING))
        }
    }

    fun setInterval(interval: Int) {
        viewModelScope.launch {
            tracking.onAutoRefreshChanged(interval)
            if (interval == StudySettingValue.AutoRefreshInterval.NONE) {
                settingsRepo.setAutoRefresh(StudySettingValue.OFF)
            } else {
                settingsRepo.setAutoRefresh(StudySettingValue.ON)
                updateAutoRefreshInterval(interval)
            }
            // cancels the periodic work for "manual only", re-arms it with the new period otherwise
            startBackgroundRefresh(StudyAutoRefresh.From.SETTING)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val INTERVALS = listOf(
            StudySettingValue.AutoRefreshInterval.NONE,
            StudySettingValue.AutoRefreshInterval.EVERY_HOUR,
            StudySettingValue.AutoRefreshInterval.EVERY_3HOUR,
            StudySettingValue.AutoRefreshInterval.EVERY_6HOUR,
            StudySettingValue.AutoRefreshInterval.EVERY_12HOUR,
            StudySettingValue.AutoRefreshInterval.EVERY_24HOUR,
        )
    }
}
