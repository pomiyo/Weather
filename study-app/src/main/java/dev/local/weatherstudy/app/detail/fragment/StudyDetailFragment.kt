package dev.local.weatherstudy.app.detail.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.detail.adapter.card.StudyDetailAdapter
import dev.local.weatherstudy.app.detail.adapter.header.StudyDetailHeaderAdapter
import dev.local.weatherstudy.app.detail.fragment.renderer.StudyDetailRenderer
import dev.local.weatherstudy.app.detail.view.StudyDetailSwipeRefresh
import dev.local.weatherstudy.app.detail.viewmodel.StudyDetailViewModel
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAction
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailRefreshResult
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSideEffect
import kotlinx.coroutines.launch

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.fragment.DetailFragment
 * (+ `DetailFragmentArgs`, `DetailFragmentDirections`, `DetailNavigator`,
 * `DetailKeyEventHelper`, and the five classes in `fragment/renderer/`)
 *
 * ### The renderer split
 *
 * The original does **not** put rendering in the Fragment. It has a `renderer` package:
 *
 * ```
 * DetailRenderer                     the entry point the Fragment calls
 * DetailContentRenderer              the card list
 * DetailMainViewSetup                one-time view wiring
 * DetailAppBarOffsetChangedListener  drives StudyCollapsibleToolbar.collapseProgress
 * AppBarAccessibilityDelegate        TalkBack on the collapsing header
 * ```
 *
 * So the Fragment's job is lifecycle, inflation, turning taps into actions, and
 * connecting the state flow to a renderer. Reconstructed with the same split.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyDetailFragment : Fragment(R.layout.study_detail_fragment) {

    private val viewModel: StudyDetailViewModel by viewModels()
    private val navigator = StudyDetailNavigator()

    private var renderer: StudyDetailRenderer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = StudyDetailAdapter(
            stateProvider = { viewModel.container.stateFlow.value },
            onAction = { cardType ->
                viewModel.dispatch(
                    // the closing card's one control is "refresh"
                    if (cardType == StudyDetailCardType.Indicator) StudyDetailAction.Refresh
                    else StudyDetailAction.CardClicked(cardType),
                )
            },
        )

        renderer = StudyDetailRenderer(
            root = view,
            adapter = adapter,
            headerAdapter = StudyDetailHeaderAdapter(),
            onPageSelected = { viewModel.dispatch(StudyDetailAction.SelectPage(it)) },
        ).also { it.setUp(viewModel) }

        view.findViewById<StudyDetailSwipeRefresh>(R.id.swipe_refresh)
            .setOnRefreshListener { viewModel.dispatch(StudyDetailAction.Refresh) }
        view.findViewById<View>(R.id.bottom_refresh)
            .setOnClickListener { viewModel.dispatch(StudyDetailAction.Refresh) }
        view.findViewById<View>(R.id.toolbar_locations)
            .setOnClickListener { viewModel.dispatch(StudyDetailAction.GoToLocations) }
        view.findViewById<View>(R.id.toolbar_settings)
            .setOnClickListener { viewModel.dispatch(StudyDetailAction.GoToSettings) }
        view.findViewById<View>(R.id.detail_message_action)
            .setOnClickListener { viewModel.dispatch(StudyDetailAction.GoToSearch) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.container.stateFlow.collect { state -> renderer?.render(state) } }
                launch { viewModel.container.sideEffectFlow.collect(::onSideEffect) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // coming back to the screen is a reason to check the three staleness clocks
        viewModel.dispatch(StudyDetailAction.Load)
    }

    private fun onSideEffect(effect: StudyDetailSideEffect) {
        when (effect) {
            is StudyDetailSideEffect.ShowRefreshResult -> when (effect.result) {
                StudyDetailRefreshResult.Failed -> toast(R.string.study_refresh_failed)
                StudyDetailRefreshResult.NoNetwork -> toast(R.string.study_refresh_no_network)
                StudyDetailRefreshResult.Success -> toast(R.string.study_refresh_done)
                StudyDetailRefreshResult.None -> Unit
            }
            else -> navigator.handle(findNavController(), effect)
        }
    }

    private fun toast(messageRes: Int) =
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        renderer = null
        super.onDestroyView()
    }
}

/**
 * Corresponds conceptually to `…detail.fragment.DetailNavigator`.
 *
 * Observed responsibility: turn a side effect into a nav action. Separate from the
 * Fragment so navigation is testable and the Fragment stays lifecycle-only.
 */
class StudyDetailNavigator {
    fun handle(
        navController: androidx.navigation.NavController,
        effect: StudyDetailSideEffect,
    ) {
        when (effect) {
            is StudyDetailSideEffect.NavigateToLocations ->
                runCatching { navController.navigate(R.id.action_detail_to_location) }
            StudyDetailSideEffect.NavigateToSearch ->
                runCatching { navController.navigate(R.id.action_detail_to_search) }
            StudyDetailSideEffect.NavigateToSettings ->
                runCatching { navController.navigate(R.id.action_detail_to_setting) }
            StudyDetailSideEffect.NavigateToLifeStyleSettings ->
                runCatching { navController.navigate(R.id.action_detail_to_lifestyle_setting) }
            // the Samsung-dependent and web effects are no-ops in the reconstruction
            else -> Unit
        }
    }
}
