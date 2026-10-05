package dev.local.weatherstudy.ui.common.detail.state

import dev.local.weatherstudy.domain.entity.weather.StudyWeather

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.ui.common.detail.state.DetailState
 * …DetailItemState, …DetailScreenState, …DetailRefreshState, …DetailTopInfoState,
 * …DetailBackgroundState, …DetailBadgeState, …DetailIndicatorState (64 state classes
 * in that package)
 *
 * ### The Orbit MVI state — one immutable tree per render
 *
 * `DetailViewModel` is an Orbit `ContainerHost<DetailState, DetailSideEffect>`. This is
 * the `State` half: everything the detail screen draws, as one value. The 64 classes in
 * the original's `detail.state` package are its parts, and `:study-app`'s 29
 * `*StateProvider` classes are what build them.
 *
 * The reconstruction keeps the top-level shape and the card-state hierarchy; the
 * per-card leaf states are in [StudyDetailCardState].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyDetailState(
    val screen: StudyDetailScreenState = StudyDetailScreenState.Loading,
    val selectedKey: String = "",
    val details: List<StudyDetailItemState> = emptyList(),
    val refresh: StudyDetailRefreshState = StudyDetailRefreshState(),
    val configuration: StudyDetailConfiguration = StudyDetailConfiguration(),
    val isTalkBackEnabled: Boolean = false,
) {
    /** the page the pager is on — the original's `selectedDetail` */
    val selectedDetail: StudyDetailItemState?
        get() = details.firstOrNull { it.key == selectedKey } ?: details.firstOrNull()

    val selectedIndex: Int
        get() = details.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
}

/**
 * Corresponds conceptually to `…detail.state.DetailItemState`.
 *
 * Observed responsibility: **one saved location's worth of detail screen**. The detail
 * screen is a ViewPager2 of these, one page per city, and `cardSortedList` is what
 * `DetailAdapter.updateList` consumes.
 */
data class StudyDetailItemState(
    val key: String,
    val topInfo: StudyDetailTopInfoState = StudyDetailTopInfoState(),
    val background: StudyDetailBackgroundState = StudyDetailBackgroundState(),
    val indicator: StudyDetailIndicatorState = StudyDetailIndicatorState(),
    val badge: StudyDetailBadgeState = StudyDetailBadgeState(),
    val cardStates: Map<StudyDetailCardType, StudyDetailCardState> = emptyMap(),
    val cardSortedList: List<StudyDetailCardType> = emptyList(),
)

/** Corresponds conceptually to `…detail.state.DetailScreenState`. */
sealed interface StudyDetailScreenState {
    data object Loading : StudyDetailScreenState
    data object Empty : StudyDetailScreenState
    data object Content : StudyDetailScreenState
    data class Error(val throwable: Throwable) : StudyDetailScreenState
}

/**
 * Corresponds conceptually to `…detail.state.DetailScreenTypeState`.
 *
 * Session 4 rewrite. This was `{ PHONE, TABLET, FOLD_MAIN, FOLD_COVER, DESKTOP }`, which
 * is a vocabulary of DEVICES. The original's is a vocabulary of WINDOW SIZES, and it has
 * four values, computed by `DetailScreenStateProvider` from nothing but
 * `screenWidthDp` / `screenHeightDp`:
 *
 * ```
 * widthDp >= 960 && heightDp >= 411 -> Huge
 * widthDp >= 960 && heightDp <  411 -> NormalLandscape
 * widthDp in 589..959 && heightDp >= 411 -> Large
 * widthDp in 589..959 && heightDp <  411 -> NormalLandscape
 * else                              -> Normal
 * ```
 *
 * The difference matters. A tablet is not a case here; a *wide window* is, whether that
 * comes from a tablet, an unfolded fold, DeX or a resized multi-window. Routing the
 * decision through the window's own measurements is why the original needs no
 * `values-sw600dp` override for the detail screen at all — there is none in the APK.
 */
enum class StudyDetailScreenType { NORMAL, NORMAL_LANDSCAPE, LARGE, HUGE }

/**
 * Corresponds conceptually to `…detail.state.DetailRefreshState` and
 * `DetailRefreshResultState`.
 *
 * Observed responsibility: the pull-to-refresh spinner reflects **shared** app state
 * (`StudyStatusRepo`), so a refresh a widget started shows here too.
 */
data class StudyDetailRefreshState(
    val isRefreshing: Boolean = false,
    val result: StudyDetailRefreshResult = StudyDetailRefreshResult.None,
)

/** Corresponds conceptually to `…detail.state.DetailRefreshResultState`. */
sealed interface StudyDetailRefreshResult {
    data object None : StudyDetailRefreshResult
    data object Success : StudyDetailRefreshResult
    data object NoNetwork : StudyDetailRefreshResult
    data object Failed : StudyDetailRefreshResult
}

/**
 * Corresponds conceptually to `…detail.state.DetailConfiguration`,
 * `DetailIndexCardSizeState`, `DetailCardSpanState`.
 *
 * Observed responsibility: the layout decisions, all three of which come from use cases
 * rather than resources — `GetColumnSize`, `GetContentAreaWidth`, `GetSpanType`. That is
 * why a fold change re-lays out the grid without a configuration-change restart.
 *
 * ### The five raw fields are the original's
 *
 * `DetailConfiguration` is a copy of five fields of `android.content.res.Configuration`
 * and nothing else — `screenLayout`, `screenWidthDp`, `densityDpi`, `orientation`,
 * `smallestScreenWidthDp` — taken in `DetailFragment.setConfiguration`. It is a *value*,
 * which is what lets the fragment compare the new one against the one in state and skip
 * the re-order when nothing that matters changed.
 *
 * [screenType], [contentColumnSize] and [contentWidthPx] are DERIVED from those fields.
 * The original derives them on demand (`DetailViewModel.getContentColumnSize()` calls
 * `GetColumnSize(configuration, screenState)` every time it is read); they are cached
 * here instead, computed once per configuration change, because the reconstruction's
 * adapter and renderer read them off the state rather than off the ViewModel. Same
 * inputs, same formulas — see `StudyGetColumnSizeImpl`.
 */
data class StudyDetailConfiguration(
    val screenLayout: Int = 0,
    val screenWidthDp: Int = 0,
    val densityDpi: Int = 0,
    val orientation: Int = 0,
    val smallestScreenWidthDp: Int = 0,
    /** derived — `DetailScreenState.screenTypeState` */
    val screenType: StudyDetailScreenType = StudyDetailScreenType.NORMAL,
    /**
     * `AppUtils.isPhoneAndLandScape` — smallestScreenWidthDp <= 411, not multi-window,
     * orientation landscape. Picks `detail_top_info_land_height` for the toolbar.
     */
    val isPhoneLandscape: Boolean = false,
    /**
     * `AppUtils.isPhoneModeNLandscapeOrMultiWindow`, which is what
     * `DetailTopInfoImageTypeStateProvider` turns into
     * `AnimationIconOnly` vs `IllustrationAndAnimationIcon`: a window too small to carry
     * the hero illustration shows the 70dp animated icon instead, in a header that no
     * longer expands.
     */
    val isSmallImageArea: Boolean = false,
    /** derived — `GetColumnSize` */
    val contentColumnSize: Int = 1,
    /** derived — `GetContentAreaWidth`, in pixels */
    val contentWidthPx: Int = 0,
    val isRtl: Boolean = false,
    val isDesktopMode: Boolean = false,
) {
    /**
     * `DetailState.isLargeScreen` — Large or Huge, nothing else. NormalLandscape is a
     * *phone* in landscape and is explicitly not a large screen, which is why the bottom
     * floating bar keeps its full width there while the cards do not.
     */
    val isLargeScreen: Boolean
        get() = screenType == StudyDetailScreenType.LARGE ||
            screenType == StudyDetailScreenType.HUGE
}

/**
 * Corresponds conceptually to `…detail.state.DetailTopInfoState`,
 * `DetailTopInfoLocationInfoState`, `DetailTopInfoImageTypeState`,
 * `DetailAnimationIconState`, `DetailIconState`, `DetailIllustrationState`.
 *
 * Observed responsibility: the collapsing header — city, temperature, condition, and
 * which of the three icon representations to use (Lottie animation, static WebP, or the
 * larger illustration). The original keeps the choice in state, not in the View, because
 * it depends on policy and on whether the header is collapsed.
 */
data class StudyDetailTopInfoState(
    val cityName: String = "",
    val isCurrentLocation: Boolean = false,
    val temperature: String = "",
    val weatherText: String = "",
    val highLow: String = "",
    val feelsLike: String = "",
    val iconNum: Int = 0,
    val imageType: StudyDetailImageType = StudyDetailImageType.ANIMATION,
    val updateTimeText: String = "",
)

/** Corresponds conceptually to `…detail.state.DetailTopInfoImageTypeState`. */
enum class StudyDetailImageType {
    /** Lottie, the default on the expanded header */
    ANIMATION,

    /** static WebP, used when animations are reduced or the header is collapsed */
    STATIC,

    /** the large illustration variant */
    ILLUSTRATION,
}

/**
 * Corresponds conceptually to `…detail.state.DetailBackgroundState`.
 *
 * Observed responsibility: the per-condition gradient behind the screen. The same
 * decision drives the 11 themed splash activities — see
 * `reports/screen-map.md` §3.
 */
data class StudyDetailBackgroundState(
    val conditionCode: Int = 0,
    val isDay: Boolean = true,
    val gradientStartColor: Int = 0,
    val gradientEndColor: Int = 0,
    /**
     * The painted artwork behind the screen - one of the eleven `detail_bg_gradient_*`
     * images, resolved by `StudyBackgroundProvider.getBackground(iconNum, isDay)`.
     *
     * 0 when the local study assets are absent, in which case the gradient pair above is
     * drawn instead. The original has no such fallback: the artwork always ships.
     */
    val artworkResId: Int = 0,
    /**
     * The hero Lottie animation - one of the 37 compositions in the `illust` asset set,
     * resolved by
     * `StudyIllustrationProvider.resolve(iconNum, temperature)`.
     *
     * Empty when the local study assets are absent. Carried as an asset PATH rather than a
     * resource id because Lottie loads it from assets, which is also how the original does
     * it - `DetailIllustrationState` holds a string too.
     */
    val illustrationAsset: String = "",
    /** intrinsic aspect ratio of [illustrationAsset]; see StudyIllustrationProvider */
    val illustrationAspectRatio: Float = 1f,
)

/**
 * Corresponds conceptually to `…detail.state.DetailIndicatorState` and
 * `DetailIndicatorSourceVisibleState`.
 *
 * Observed responsibility: the provider attribution at the bottom ("Weather data
 * provided by …"), which is a contractual requirement rather than decoration — hence
 * its own card type and its own state.
 */
data class StudyDetailIndicatorState(
    val providerName: String = "",
    val providerLogoType: StudyIndicatorLogoType = StudyIndicatorLogoType.TEXT,
    val isSourceVisible: Boolean = true,
    val feedbackUrl: String = "",
    val privacyUrl: String = "",
)

/** `detail_indicator_with_image_logo.xml` vs `detail_indicator_with_text_logo.xml`. */
enum class StudyIndicatorLogoType { TEXT, IMAGE }

/** Corresponds conceptually to `…detail.state.DetailBadgeState`. */
data class StudyDetailBadgeState(
    val showUpdateBadge: Boolean = false,
    val showSettingsBadge: Boolean = false,
)

/** Corresponds conceptually to `…detail.state.DetailStateUpdate` — the reducer helper. */
class StudyDetailStateUpdate {
    fun withScreen(state: StudyDetailState, screen: StudyDetailScreenState) =
        state.copy(screen = screen)

    fun withDetails(state: StudyDetailState, details: List<StudyDetailItemState>) =
        state.copy(
            details = details,
            screen = if (details.isEmpty()) StudyDetailScreenState.Empty else StudyDetailScreenState.Content,
            selectedKey = state.selectedKey.takeIf { key -> details.any { it.key == key } }
                ?: details.firstOrNull()?.key.orEmpty(),
        )

    fun withRefreshing(state: StudyDetailState, refreshing: Boolean) =
        state.copy(refresh = state.refresh.copy(isRefreshing = refreshing))

    fun withSelected(state: StudyDetailState, key: String) = state.copy(selectedKey = key)
}

/** Reconstruction helper: the domain aggregate a page was built from. */
data class StudyDetailSource(val weather: StudyWeather)
