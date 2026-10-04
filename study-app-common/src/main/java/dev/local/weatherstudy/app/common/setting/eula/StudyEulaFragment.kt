package dev.local.weatherstudy.app.common.setting.eula

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.local.weatherstudy.app.common.R
import dev.local.weatherstudy.app.common.view.StudyScreenToolbar
import dev.local.weatherstudy.app.common.view.applySystemBarPadding
import dev.local.weatherstudy.app.common.view.restartGraph
import dev.local.weatherstudy.domain.usecase.StudyAgreeToUcl
import dev.local.weatherstudy.domain.usecase.StudyDisagreeToUcl
import dev.local.weatherstudy.domain.usecase.StudyWhetherToConsentUcl
import dev.local.weatherstudy.ui.common.resource.StudyGlobalEulaTextProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.app.common.setting.eula.EulaFragment
 * (+ `EulaViewModel`, and the regional `EulaTextProvider` it reads)
 *
 * Observed responsibility: the consent gate. The startup condition chain routes here when
 * `EulaCondition` is not satisfied; agreeing records the consent with its policy version
 * and **re-enters the graph**, so the chain runs again and moves on to its next
 * condition. The screen does not decide what comes after it.
 *
 * The same destination is reachable from Settings, where it is a viewer: once consent is
 * held there is nothing to agree to.
 *
 * It lives in the common module, not the phone app module, because the original shares
 * it with the widget-configuration flow.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyEulaFragment : Fragment(R.layout.study_eula_fragment) {

    private val viewModel: StudyEulaViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        toolbar.setTitle(R.string.study_eula_screen_title)
        toolbar.back.setOnClickListener { leave() }

        view.findViewById<TextView>(R.id.eula_title).text = viewModel.title
        val body = view.findViewById<TextView>(R.id.eula_body)
        val buttons = view.findViewById<View>(R.id.eula_buttons)

        view.findViewById<View>(R.id.eula_agree).setOnClickListener {
            viewModel.agree { findNavController().restartGraph() }
        }
        view.findViewById<View>(R.id.eula_decline).setOnClickListener {
            viewModel.decline { requireActivity().finish() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.needsConsent.collect { needsConsent ->
                buttons.visibility = if (needsConsent) View.VISIBLE else View.GONE
                body.text = if (needsConsent) {
                    viewModel.body
                } else {
                    viewModel.body + "\n\n" + getString(R.string.study_eula_agreed)
                }
            }
        }
    }

    /** without consent there is no app to go back to */
    private fun leave() {
        if (viewModel.needsConsent.value) requireActivity().finish()
        else findNavController().popBackStack()
    }
}

/** Corresponds conceptually to `…setting.eula.EulaViewModel`. */
@HiltViewModel
class StudyEulaViewModel @Inject constructor(
    private val whetherToConsentUcl: StudyWhetherToConsentUcl,
    private val agreeToUcl: StudyAgreeToUcl,
    private val disagreeToUcl: StudyDisagreeToUcl,
    textProvider: StudyGlobalEulaTextProvider,
) : ViewModel() {

    val title: String = textProvider.getTitle()
    val body: String = textProvider.getBody()

    private val _needsConsent = MutableStateFlow(true)
    val needsConsent: StateFlow<Boolean> = _needsConsent.asStateFlow()

    init {
        viewModelScope.launch { _needsConsent.value = whetherToConsentUcl() }
    }

    fun agree(onDone: () -> Unit) {
        viewModelScope.launch {
            agreeToUcl()
            onDone()
        }
    }

    fun decline(onDone: () -> Unit) {
        viewModelScope.launch {
            disagreeToUcl()
            onDone()
        }
    }
}
