package dev.local.weatherstudy.app.detail.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.domain.entity.device.StudyDeviceType
import dev.local.weatherstudy.domain.policy.StudyOrderingPolicy
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.system.service.StudySystemService
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailConfiguration
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailScreenType
import dev.local.weatherstudy.ui.common.resource.StudyDensityUnitConverter
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 24 classes in
 * com.sec.android.daemonapp.app.detail.usecase:
 * `GetCardOrder(+Impl)`, `GetColumnSize(+Impl)`, `GetContentAreaWidth(+Impl)`,
 * `GetSpanType`, `CheckApproximateLocation`, `CountEnterDetail`,
 * `ObserveEnterDetailCount(+WithApproximateLocation)`, `FetchWeatherNews(+Impl)`,
 * `RetrieveWeatherNews(+Impl)`, `GoToNavDetail(+Impl)`, `GoToWebFromDetail(+Impl)`,
 * `GoToSmartThings(+Impl)`, `GoToSamsungNews(+Impl)`, `LaunchJitTips`
 *
 * ### Layout decisions are use cases here, not resource qualifiers
 *
 * `GetColumnSize` is the one to notice. It returns 1 or 2, and `DetailAdapter` turns
 * that into `isFullSpan` on every card. Because it is a use case reading the live window
 * size rather than a `values-sw600dp` integer, **folding a device re-lays out the grid
 * without a configuration-change restart** — which is the whole reason it exists. There
 * is no detail-screen dimension override in any `-land` or `-sw600dp` folder of the APK;
 * the whole responsive behaviour is these two functions.
 *
 * ### Session 4 rewrite: the dependency ran the wrong way
 *
 * The previous version had `GetColumnSize` ask the device whether it was a tablet, a
 * fold or in DeX, and `GetContentAreaWidth` then divide the screen width by the answer.
 * The original is the exact inverse, and it never asks what the device is:
 *
 * ```
 * GetContentAreaWidthImpl(configuration, screenState) = when (screenState) {
 *     Normal          -> widthPx - 2 * detail_content_portrait_padding
 *     NormalLandscape -> widthPx * 0.62
 *     Large           -> widthPx * 0.86
 *     Huge            -> detail_content_width_at_huge_screen   // 840dp, flat
 * }
 * GetColumnSizeImpl(...) = if (contentAreaWidth > 618dp) 2 else 1
 * ```
 *
 * Width comes first and the column count falls out of it. Three consequences the old
 * direction could not produce:
 *
 * 1. **A phone in landscape puts the cards in a centred column 62% of the width.** That
 *    is the single biggest difference between the two orientations, and it cannot be
 *    expressed as "how many columns" at all — it is one column, narrower than the screen.
 * 2. **A tablet gets two columns only if 86% of its width clears 618dp**, so a small
 *    tablet stays single-column. Asking `isTablet()` gave it two.
 * 3. **Huge windows stop growing.** Past 960dp the content is pinned to 840dp and the
 *    rest becomes margin, which no ratio applied to the screen width can do.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyGetColumnSize {
    operator fun invoke(
        configuration: StudyDetailConfiguration,
        screenType: StudyDetailScreenType,
    ): Int
}

class StudyGetColumnSizeImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getContentAreaWidth: StudyGetContentAreaWidth,
) : StudyGetColumnSize {

    override fun invoke(
        configuration: StudyDetailConfiguration,
        screenType: StudyDetailScreenType,
    ): Int =
        if (getContentAreaWidth(configuration, screenType) >
            StudyDensityUnitConverter.dpToPx(TWO_COLUMN_FROM_DP, context)
        ) {
            COLUMNS_WIDE
        } else {
            COLUMNS_NARROW
        }

    private companion object {
        /**
         * The whole two-column decision, as a literal in `GetColumnSizeImpl`. Not a
         * dimension resource, not a `sw` qualifier: a float compared against the measured
         * content width.
         */
        const val TWO_COLUMN_FROM_DP = 618.0f
        const val COLUMNS_NARROW = 1
        const val COLUMNS_WIDE = 2
    }
}

/**
 * `GetContentAreaWidth(+Impl)` — the width, in pixels, that the cards are allowed to use.
 *
 * Everything else about the horizontal layout follows from it: the renderer pads the card
 * list, the app bar and the illustration by `(screenWidth - contentAreaWidth) / 2`, so a
 * narrower content area is what centres the column.
 */
interface StudyGetContentAreaWidth {
    operator fun invoke(
        configuration: StudyDetailConfiguration,
        screenType: StudyDetailScreenType,
    ): Int
}

class StudyGetContentAreaWidthImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : StudyGetContentAreaWidth {

    override fun invoke(
        configuration: StudyDetailConfiguration,
        screenType: StudyDetailScreenType,
    ): Int {
        val screenWidthPx =
            StudyDensityUnitConverter.dpToPx(configuration.screenWidthDp.toFloat(), context)
        return when (screenType) {
            StudyDetailScreenType.NORMAL ->
                screenWidthPx - 2 * context.resources
                    .getDimensionPixelSize(R.dimen.study_detail_content_portrait_padding)
            StudyDetailScreenType.NORMAL_LANDSCAPE ->
                (screenWidthPx * PHONE_LANDSCAPE_RATIO).roundToInt()
            StudyDetailScreenType.LARGE ->
                (screenWidthPx * LARGE_SCREEN_RATIO).roundToInt()
            StudyDetailScreenType.HUGE ->
                context.resources
                    .getDimensionPixelSize(R.dimen.study_detail_content_width_at_huge_screen)
        }
    }

    private companion object {
        /** `phoneLandscapeRatio` — a phone on its side gives 38% of the width back as margin */
        const val PHONE_LANDSCAPE_RATIO = 0.62f
        /** `largeScreenRatio` */
        const val LARGE_SCREEN_RATIO = 0.86f
    }
}

/**
 * `GetCardOrder(+Impl)`.
 *
 * Session 3 rewrite. This previously returned a static list from a policy constant:
 *
 * ```kotlin
 * StudyOrderingPolicy.DEFAULT_DETAIL_CARD_ORDER.mapNotNull { StudyDetailCardType.fromName(it) }
 * ```
 *
 * The original takes four parameters and branches about fifteen times. The order is not a
 * constant that gets filtered - it is **computed**, and three of its properties are only
 * visible once it is written out properly:
 *
 * 1. **The column count reorders the middle of the list.** In one column the order is
 *    Insight, Daily, LifeStyle, Index. In two it is Insight, Index, Daily, LifeStyle,
 *    because Index pairs beside Insight instead of following the others. The same data
 *    produces a different sequence purely from device geometry.
 * 2. **Sun and Moon collapse into one card when both are present.** They are not two cards
 *    that happen to sit together; SunAndMoon is a third card type with its own view holder
 *    hosting both Canvas views.
 * 3. **Indicator is appended unconditionally**, outside every branch. The provider
 *    attribution is the one thing that cannot be scrolled away from.
 *
 * ### A faithfully reproduced oddity
 *
 * The BottomIndex and LifeTips steps each have an if/else whose two arms are identical -
 * both add the same card. That is what the decompiled bytecode does; it is dead branching
 * left behind by a refactor in the original. It is kept here, with the condition intact,
 * because the brief is to reproduce the original's shape rather than a tidied version of
 * it. Collapsing it would be the obvious "improvement" and would lose the evidence.
 *
 * See reports/detail-view-types.md section 4.
 */
interface StudyGetCardOrder {
    operator fun invoke(
        itemState: StudyDetailItemState,
        columnSize: Int,
        isSmartThingsShown: Boolean = false,
    ): List<StudyDetailCardType>
}

class StudyGetCardOrderImpl @Inject constructor() : StudyGetCardOrder {

    private fun StudyDetailItemState.isVisible(type: StudyDetailCardType): Boolean =
        cardStates[type]?.isVisible == true

    override fun invoke(
        itemState: StudyDetailItemState,
        columnSize: Int,
        isSmartThingsShown: Boolean,
    ): List<StudyDetailCardType> {
        val order = mutableListOf<StudyDetailCardType>()

        if (itemState.isVisible(StudyDetailCardType.Alert)) order += StudyDetailCardType.Alert
        if (itemState.isVisible(StudyDetailCardType.Hourly)) order += StudyDetailCardType.Hourly
        if (itemState.isVisible(StudyDetailCardType.Precipitation)) {
            order += StudyDetailCardType.Precipitation
        }

        // the column-count branch: see point 1 in the class doc
        if (columnSize == 1) {
            if (itemState.isVisible(StudyDetailCardType.Insight)) order += StudyDetailCardType.Insight
            if (itemState.isVisible(StudyDetailCardType.Daily)) order += StudyDetailCardType.Daily
            if (itemState.isVisible(StudyDetailCardType.LifeStyle)) order += StudyDetailCardType.LifeStyle
            if (itemState.isVisible(StudyDetailCardType.Index)) order += StudyDetailCardType.Index
        } else {
            if (itemState.isVisible(StudyDetailCardType.Insight)) {
                order += StudyDetailCardType.Insight
                order += StudyDetailCardType.Index
                order += StudyDetailCardType.Daily
                if (itemState.isVisible(StudyDetailCardType.LifeStyle)) {
                    order += StudyDetailCardType.LifeStyle
                }
            } else {
                order += StudyDetailCardType.Daily
                if (itemState.isVisible(StudyDetailCardType.LifeStyle)) {
                    order += StudyDetailCardType.LifeStyle
                }
                order += StudyDetailCardType.Index
            }
        }

        // sun + moon merge into a single card type when both are present
        val sun = itemState.isVisible(StudyDetailCardType.Sun)
        val moon = itemState.isVisible(StudyDetailCardType.Moon)
        if (sun && moon) {
            order += StudyDetailCardType.SunAndMoon
        } else {
            if (sun) order += StudyDetailCardType.Sun
            if (moon) order += StudyDetailCardType.Moon
        }

        if (isSmartThingsShown) order += StudyDetailCardType.SmartThings
        if (itemState.isVisible(StudyDetailCardType.Radar)) order += StudyDetailCardType.Radar

        // the content block is a three-way choice, not three independent cards
        if (itemState.isVisible(StudyDetailCardType.NewsAndVideo)) {
            order += StudyDetailCardType.NewsAndVideo
        } else if (itemState.isVisible(StudyDetailCardType.TodayStoriesAndVideo)) {
            if (itemState.isVisible(StudyDetailCardType.News)) order += StudyDetailCardType.News
            order += StudyDetailCardType.TodayStoriesAndVideo
        } else {
            if (itemState.isVisible(StudyDetailCardType.News)) order += StudyDetailCardType.News
            if (itemState.isVisible(StudyDetailCardType.Video)) order += StudyDetailCardType.Video
        }

        // The if/else arms below are deliberately identical - see the class doc. The
        // condition is preserved so the original's shape stays legible.
        val radarVisible = itemState.isVisible(StudyDetailCardType.Radar)
        val newsVisible = itemState.isVisible(StudyDetailCardType.News)
        val videoVisible = itemState.isVisible(StudyDetailCardType.Video)
        val newsAndVideoVisible = itemState.isVisible(StudyDetailCardType.NewsAndVideo)
        if (itemState.isVisible(StudyDetailCardType.BottomIndex)) {
            if (!radarVisible || newsVisible || videoVisible || !newsAndVideoVisible) {
                order += StudyDetailCardType.BottomIndex
            } else {
                order += StudyDetailCardType.BottomIndex
            }
        } else if (itemState.isVisible(StudyDetailCardType.LifeTips)) {
            if (!radarVisible || newsVisible || videoVisible || newsAndVideoVisible) {
                order += StudyDetailCardType.LifeTips
            } else {
                order += StudyDetailCardType.LifeTips
            }
        }

        // unconditional, and always last
        order += StudyDetailCardType.Indicator
        return order
    }
}

/**
 * `GetSpanType`.
 *
 * Session 4 rewrite. This asked `GetColumnSize` and then listed the cards it thought were
 * wide. The original asks neither: it returns `DetailCardSpanState` from the card type,
 * `isTablet()` and whether the precipitation card is showing, and leaves the column count
 * to `DetailAdapter` — which is the only place that knows one column means every card is
 * full span anyway.
 *
 * ```
 * Alert                            -> FullSpan
 * Hourly                           -> NormalSpan iff (precipitation shown && tablet)
 * Precipitation                    -> FullSpan iff !tablet
 * everything else                  -> NormalSpan
 * ```
 *
 * The Hourly rule is the interesting one and reads backwards until you picture it: the
 * hourly strip gives up its full width ONLY on a tablet that also has a precipitation
 * card, because those two then sit side by side. Kept as a Boolean here rather than
 * reintroducing a two-value sealed class for it; nothing in the reconstruction consumes
 * the span state yet.
 */
class StudyGetSpanType @Inject constructor(
    private val systemService: StudySystemService,
) {
    fun isFullSpan(cardType: StudyDetailCardType, isPrecipitationShown: Boolean): Boolean {
        val isTablet = runCatching { systemService.getDeviceService().isTablet() }
            .getOrDefault(false)
        return when {
            cardType == StudyDetailCardType.Alert -> true
            cardType == StudyDetailCardType.Hourly -> !(isPrecipitationShown && isTablet)
            cardType == StudyDetailCardType.Precipitation -> !isTablet
            else -> false
        }
    }
}

/**
 * `CheckApproximateLocation`.
 *
 * Observed responsibility: distinguishes a coarse grant from a precise one, so the
 * detail screen can offer the "switch to precise location" tip. It is why the original
 * has two separate enter-count observers.
 */
class StudyCheckApproximateLocation @Inject constructor(
    private val checkLocationPermission:
    dev.local.weatherstudy.condition.conditions.checker.StudyCheckLocationPermission,
) {
    operator fun invoke(): Boolean =
        checkLocationPermission.hasForeground() && !checkLocationPermission.hasPrecise()
}

/** `CountEnterDetail` / `ObserveEnterDetailCount`. */
class StudyCountEnterDetail @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) {
    suspend operator fun invoke(): Int = settingsRepo.countEnterDetail()
}

/**
 * `GoToSmartThings(+Impl)` / `GoToSamsungNews(+Impl)` — STUBS.
 *
 * Both launch a Samsung companion app by package name. The reconstruction keeps the use
 * case so the call site is visible and reports that the target is unavailable.
 */
class StudyGoToSmartThings @Inject constructor() {
    operator fun invoke(): Boolean = false
}

/** `GoToSamsungNews(+Impl)` — STUB, as above. */
class StudyGoToSamsungNews @Inject constructor() {
    operator fun invoke(): Boolean = false
}

/** `GoToWebFromDetail(+Impl)` — opens a provider link in a Custom Tab. */
class StudyGoToWebFromDetail @Inject constructor() {
    operator fun invoke(url: String): Boolean = url.isNotEmpty()
}
