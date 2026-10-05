package dev.local.weatherstudy.app.detail.fragment

import android.content.res.Configuration
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
import dev.local.weatherstudy.app.common.util.StudyAppUtils
import dev.local.weatherstudy.app.detail.adapter.card.StudyDetailAdapter
import dev.local.weatherstudy.app.detail.adapter.header.StudyDetailHeaderAdapter
import dev.local.weatherstudy.app.detail.fragment.renderer.StudyDetailRenderer
import dev.local.weatherstudy.app.detail.state.provider.StudyDetailScreenStateProvider
import dev.local.weatherstudy.app.detail.usecase.StudyGetColumnSize
import dev.local.weatherstudy.app.detail.usecase.StudyGetContentAreaWidth
import dev.local.weatherstudy.app.detail.view.StudyDetailSwipeRefresh
import dev.local.weatherstudy.app.detail.viewmodel.StudyDetailViewModel
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAction
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailConfiguration
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailRefreshResult
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSideEffect
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    /**
     * The three injected collaborators `DetailFragment` uses for exactly one thing:
     * turning an `android.content.res.Configuration` into the screen's layout decisions.
     *
     * The original injects the screen state provider and the card order into the Fragment
     * and keeps `GetColumnSize` / `GetContentAreaWidth` on the ViewModel, which recomputes
     * them on every read. Both live here instead, because this reconstruction caches the
     * two derived numbers on the configuration value rather than recomputing - see
     * [StudyDetailConfiguration].
     */
    @Inject lateinit var screenStateProvider: StudyDetailScreenStateProvider

    @Inject lateinit var getColumnSize: StudyGetColumnSize

    @Inject lateinit var getContentAreaWidth: StudyGetContentAreaWidth

    private var renderer: StudyDetailRenderer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // First, before anything renders: the window's geometry decides the column count,
        // the content width, the toolbar height and whether the header carries the hero
        // illustration at all.
        setConfiguration(resources.configuration)

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
            headerAdapter = StudyDetailHeaderAdapter {
                // the header's own image-area question, read live off the state
                viewModel.container.stateFlow.value.configuration.isSmallImageArea
            },
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

    /**
     * `DetailFragment.onConfigurationChanged` -> `setConfiguration`.
     *
     * The Activity is not declared with `android:configChanges`, so a rotation normally
     * recreates it and the new instance measures itself in [onViewCreated]. This path is
     * for the changes that arrive WITHOUT a recreation - unfolding, entering or leaving
     * split screen, a DeX window being dragged - which is the case the whole
     * use-case-rather-than-resource-qualifier design exists to serve.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setConfiguration(newConfig)
    }

    /**
     * `DetailFragment.setConfiguration`.
     *
     * Copies the five Configuration fields the original copies, classifies the window,
     * and derives the two layout numbers from it. Dispatching an unchanged value is
     * harmless - the ViewModel compares before it reduces.
     */
    private fun setConfiguration(configuration: Configuration) {
        val isMultiWindow = activity?.isInMultiWindowMode == true
        val screenType = screenStateProvider(configuration)
        val base = StudyDetailConfiguration(
            screenLayout = configuration.screenLayout,
            screenWidthDp = configuration.screenWidthDp,
            densityDpi = configuration.densityDpi,
            orientation = configuration.orientation,
            smallestScreenWidthDp = configuration.smallestScreenWidthDp,
            screenType = screenType,
            isPhoneLandscape = StudyAppUtils.isPhoneAndLandscape(configuration, isMultiWindow),
            isSmallImageArea = StudyAppUtils.isPhoneModeNLandscapeOrMultiWindow(
                configuration = configuration,
                isMultiWindow = isMultiWindow,
            ),
        )
        viewModel.dispatch(
            StudyDetailAction.ConfigurationChanged(
                base.copy(
                    contentWidthPx = getContentAreaWidth(base, screenType),
                    contentColumnSize = getColumnSize(base, screenType),
                ),
            ),
        )
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
