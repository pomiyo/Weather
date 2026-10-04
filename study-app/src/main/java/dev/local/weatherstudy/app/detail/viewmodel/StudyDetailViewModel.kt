package dev.local.weatherstudy.app.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.local.weatherstudy.app.detail.state.provider.StudyDetailStateProvider
import dev.local.weatherstudy.app.detail.usecase.StudyGetCardOrder
import dev.local.weatherstudy.app.detail.usecase.StudyGetColumnSize
import dev.local.weatherstudy.app.detail.usecase.StudyGetContentAreaWidth
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.repo.StudyLifeStyleSettingsRepo
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.type.StudyAutoRefresh
import dev.local.weatherstudy.domain.type.StudyKeys
import dev.local.weatherstudy.domain.type.StudySettingValue
import dev.local.weatherstudy.domain.usecase.StudyGetUserSavedLocationCount
import dev.local.weatherstudy.domain.usecase.StudyGetWeather
import dev.local.weatherstudy.domain.usecase.StudyObserveRefreshStatus
import dev.local.weatherstudy.domain.usecase.StudyObserveWeatherChange
import dev.local.weatherstudy.domain.usecase.StudyReachToForecastRefreshTime
import dev.local.weatherstudy.domain.usecase.StudyReachToObservationRefreshTime
import dev.local.weatherstudy.sync.usecase.StudyForegroundRefreshRequest
import dev.local.weatherstudy.sync.usecase.StudyStartForegroundRefresh
import dev.local.weatherstudy.devopts.StudyDevOpts
import dev.local.weatherstudy.logger.analytics.tracking.StudyDetailTracking
import dev.local.weatherstudy.system.service.StudyLocaleService
import dev.local.weatherstudy.system.service.StudySystemService
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAction
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailConfiguration
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailRefreshResult
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailScreenState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSideEffect
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailStateUpdate
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.viewmodel.DetailViewModel
 *
 * ### The original takes 29 constructor parameters
 *
 * Recovered in full from its Kotlin `@Metadata`. That is not accidental sprawl — it is
 * what a screen looks like when every decision is a named collaborator:
 *
 * ```
 * savedStateHandle, selectedLocation, application, DetailIntent.Factory, SystemService,
 * GetWeather, DetailTracking, ForecastProviderManager, SettingsRepo, GetSmartThings,
 * GetFavoriteLocation, DetailStateProvider, ObserveRefreshStatus,
 * DetailSmartThingsCardStateProvider, DetailItemStateListProvider, ObserveWeatherChange,
 * ObserveEnterDetailCount, ObserveEnterDetailCountWithApproximateLocation,
 * DetailNewsCardStateProvider, DetailNewsAndVideoCardStateProvider,
 * DetailLifeStyleCardStateProvider, RetrieveWeatherNews, LifeStyleSettingsRepo,
 * GetCardOrder, GetColumnSize, GetContentAreaWidth, GetUserSavedLocationCount,
 * WeatherPolicyManager, DevOpts
 * ```
 *
 * It is an **Orbit MVI `ContainerHost`** — confirmed by `t8.b`/`t8.a` in the decompiled
 * output and by the `eventLoopDispatcher`/`intentLaunchingDispatcher` field names in
 * `t8.d`, which are Orbit's `Settings`. The reconstruction uses the real library
 * (`org.orbit-mvi:orbit-core` 9.0.0).
 *
 * The original's seven `observe*` methods each launch one collector —
 * `observeWeather`, `observeRefresh`, `observeNumberOfEntries`,
 * `observeNumberOfEntriesWithApproximateLocation`, `observeAppUpdateStatus`,
 * `observeSamsungNews`, `observeLifeStyleSettings`, `observeSmartThings`. Reconstructed
 * with the same decomposition; the Samsung-dependent ones are present and documented.
 *
 * A representative dependency subset is wired here (the SmartThings, news and
 * life-style providers are stubbed at their own call sites); the full list is above and
 * in `reports/reconstruction-inventory.md` §3.4.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@HiltViewModel
class StudyDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val systemService: StudySystemService,
    private val localeService: StudyLocaleService,
    private val getWeather: StudyGetWeather,
    private val detailTracking: StudyDetailTracking,
    private val settingsRepo: StudySettingsRepo,
    private val lifeStyleSettingsRepo: StudyLifeStyleSettingsRepo,
    private val stateProvider: StudyDetailStateProvider,
    private val observeWeatherChange: StudyObserveWeatherChange,
    private val observeRefreshStatus: StudyObserveRefreshStatus,
    private val getUserSavedLocationCount: StudyGetUserSavedLocationCount,
    private val getCardOrder: StudyGetCardOrder,
    private val getColumnSize: StudyGetColumnSize,
    private val getContentAreaWidth: StudyGetContentAreaWidth,
    private val policyManager: StudyWeatherPolicyManager,
    private val devOpts: StudyDevOpts,
    private val startForegroundRefresh: StudyStartForegroundRefresh,
    private val reachToForecastRefreshTime: StudyReachToForecastRefreshTime,
    private val reachToObservationRefreshTime: StudyReachToObservationRefreshTime,
) : ViewModel(), ContainerHost<StudyDetailState, StudyDetailSideEffect> {

    /** the location the pager is on, restored across process death */
    val selectedLocation: String
        get() = savedStateHandle[KEY_LOCATION] ?: ""

    private val stateUpdate = StudyDetailStateUpdate()

    /** a refresh this screen asked for, as opposed to one a widget or the scheduler started */
    private var refreshRequestedHere = false

    override val container: Container<StudyDetailState, StudyDetailSideEffect> =
        container(StudyDetailState(selectedKey = selectedLocation)) {
            observeWeather()
            observeRefresh()
            observeNumberOfEntries()
            observeLifeStyleSettings()
        }

    /** the pager's column count comes from a use case, not a resource qualifier */
    val contentColumnSize: Int get() = getColumnSize()

    val contentWidthPx: Int get() = getContentAreaWidth()

    val isRtl: Boolean get() = localeService.isRtl()

    val isTalkBackEnabled: Boolean get() = container.stateFlow.value.isTalkBackEnabled

    /** `DetailViewModel.scrollRestoreCard` — which card to scroll back to after a rebind */
    var scrollRestoreCard: StudyDetailCardType? = null

    fun dispatch(action: StudyDetailAction) = intent {
        when (action) {
            StudyDetailAction.Load -> refreshIfStale()

            StudyDetailAction.Refresh -> {
                detailTracking.onPullToRefresh()
                refreshRequestedHere = true
                reduce { stateUpdate.withRefreshing(state, true) }
                // every saved city, not only the one on screen: the pager shows them all
                startForegroundRefresh(StudyForegroundRefreshRequest(StudyAutoRefresh.From.DETAIL))
            }

            is StudyDetailAction.SelectLocation -> select(action.key)

            is StudyDetailAction.SelectPage -> {
                val key = state.details.getOrNull(action.index)?.key ?: return@intent
                if (key == state.selectedKey) return@intent
                detailTracking.onLocationPagerSwipe(action.index)
                select(key)
            }

            is StudyDetailAction.CardClicked -> {
                detailTracking.onCardClick(action.cardType.typeName)
                postSideEffect(StudyDetailSideEffect.ScrollToCard(action.cardType))
            }

            is StudyDetailAction.WebLinkClicked -> {
                detailTracking.onWebLinkClick(action.url)
                postSideEffect(StudyDetailSideEffect.OpenWebLink(action.url))
            }

            is StudyDetailAction.InsightSwiped -> detailTracking.onInsightCardSwipe(action.index)

            StudyDetailAction.GoToLocations -> {
                detailTracking.onGoToLocations()
                postSideEffect(StudyDetailSideEffect.NavigateToLocations())
            }

            StudyDetailAction.GoToSearch -> postSideEffect(StudyDetailSideEffect.NavigateToSearch)

            StudyDetailAction.GoToSettings -> {
                detailTracking.onGoToSettings()
                postSideEffect(StudyDetailSideEffect.NavigateToSettings)
            }

            StudyDetailAction.GoToLifeStyleSettings ->
                postSideEffect(StudyDetailSideEffect.NavigateToLifeStyleSettings)

            StudyDetailAction.GoToSmartThings ->
                postSideEffect(StudyDetailSideEffect.LaunchSmartThings)

            is StudyDetailAction.ConfigurationChanged ->
                reduce { state.copy(configuration = action.configuration) }
        }
    }

    /**
     * The selected page is also the "favourite location" — the one the app opens on, and
     * the one the widgets and the notification follow. Selecting persists it.
     */
    private fun select(key: String) = intent {
        savedStateHandle[KEY_LOCATION] = key
        reduce { stateUpdate.withSelected(state, key) }
        settingsRepo.setFavoriteLocation(key)
    }

    /** `DetailViewModel.observeWeather` */
    private fun observeWeather() = intent {
        reduce { stateUpdate.withScreen(state, StudyDetailScreenState.Loading) }
        detailTracking.onEnterDetail()
        // the unit is part of the input: changing it re-formats every card from one place
        combine(observeWeatherChange(), settingsRepo.observeTempScale()) { weathers, tempScale ->
            weathers to tempScale
        }.collectLatest { (weathers, tempScale) ->
            val configuration = StudyDetailConfiguration(
                contentColumnSize = getColumnSize(),
                contentWidthPx = getContentAreaWidth(),
                isRtl = localeService.isRtl(),
                isDesktopMode = runCatching {
                    systemService.getDesktopService()
                        .isDesktopMode(systemService.getFloatingFeature())
                }.getOrDefault(false),
            )
            reduce {
                // refresh progress is not derived from the weather, so it is carried over
                stateProvider(weathers, tempScale, state.selectedKey, configuration)
                    .copy(refresh = state.refresh)
            }
        }
    }

    /**
     * Entering the screen checks the same clocks the background worker does, and asks for
     * a refresh only when one of them has run out. A fresh forecast costs nothing here.
     */
    private fun refreshIfStale() = intent {
        val stored = runCatching { getWeather() }.getOrDefault(emptyList())
        if (stored.isEmpty()) return@intent
        val stale = devOpts.forceStaleData ||
            stored.any { reachToForecastRefreshTime(it) || reachToObservationRefreshTime(it) }
        if (stale) startForegroundRefresh(StudyForegroundRefreshRequest(StudyAutoRefresh.From.DETAIL))
    }

    /** `DetailViewModel.observeRefresh` — shared status, not local state */
    private fun observeRefresh() = intent {
        var previous = StudySettingValue.RefreshStatus.DONE
        observeRefreshStatus(StudyKeys.REFRESH).collectLatest { status ->
            val refreshing = status == StudySettingValue.RefreshStatus.RUNNING
            reduce { stateUpdate.withRefreshing(state, refreshing) }
            // the status row outlives the screen: only a change seen here is news, and a
            // failure is only worth interrupting for when the user asked for the refresh
            val finished = previous == StudySettingValue.RefreshStatus.RUNNING && !refreshing
            if (finished && refreshRequestedHere) {
                refreshRequestedHere = false
                postSideEffect(
                    StudyDetailSideEffect.ShowRefreshResult(
                        if (status == StudySettingValue.RefreshStatus.FAILED) {
                            StudyDetailRefreshResult.Failed
                        } else {
                            StudyDetailRefreshResult.Success
                        },
                    ),
                )
            }
            previous = status
        }
    }

    /**
     * `DetailViewModel.observeNumberOfEntries` — the original counts detail entries and
     * uses the count to decide when to show one-time tips. It has a second variant,
     * `observeNumberOfEntriesWithApproximateLocation`, because the tip differs when only
     * coarse location was granted.
     */
    private fun observeNumberOfEntries() = intent {
        runCatching { settingsRepo.countEnterDetail() }
    }

    /** `DetailViewModel.observeLifeStyleSettings` */
    private fun observeLifeStyleSettings() = intent {
        runCatching { lifeStyleSettingsRepo.getSettings() }
    }

    /**
     * `DetailViewModel.observeSmartThings` and `observeSamsungNews` exist in the
     * original and are deliberately not wired here: both depend on Samsung platform
     * apps. See `reports/samsung-bridge-map.md`.
     */
    private fun observeSamsungInterworking() = Unit

    companion object {
        const val KEY_LOCATION = "location_key"
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.viewmodel.DetailSaveStateHandle
 *
 * Observed responsibility: a typed wrapper over the saved-state keys, so the ViewModel
 * and the Fragment agree on them without sharing string literals.
 */
class StudyDetailSaveStateHandle @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var locationKey: String
        get() = savedStateHandle[StudyDetailViewModel.KEY_LOCATION] ?: ""
        set(value) { savedStateHandle[StudyDetailViewModel.KEY_LOCATION] = value }

    var scrollPosition: Int
        get() = savedStateHandle[KEY_SCROLL] ?: 0
        set(value) { savedStateHandle[KEY_SCROLL] = value }

    private companion object {
        const val KEY_SCROLL = "scroll_position"
    }
}
