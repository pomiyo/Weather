package dev.local.weatherstudy.app.detail.usecase

import dev.local.weatherstudy.domain.entity.device.StudyDeviceType
import dev.local.weatherstudy.domain.policy.StudyOrderingPolicy
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.system.service.StudySystemService
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import javax.inject.Inject

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
 * that into `isFullSpan` on every card. Because it is a use case reading the live device
 * state rather than a `values-sw600dp` integer, **folding a device re-lays out the grid
 * without a configuration-change restart** — which is the whole reason it exists.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyGetColumnSize {
    operator fun invoke(): Int
}

class StudyGetColumnSizeImpl @Inject constructor(
    private val systemService: StudySystemService,
) : StudyGetColumnSize {

    override fun invoke(): Int {
        val device = runCatching { systemService.getDeviceService() }.getOrNull()
        val floating = runCatching { systemService.getFloatingFeature() }.getOrNull()
        val folded = runCatching { systemService.getFoldStateService().isFolded() }
            .getOrDefault(false)

        val isWide = when {
            runCatching { device?.isTablet() == true }.getOrDefault(false) -> true
            runCatching { floating?.isFoldDevice() == true && !folded }.getOrDefault(false) -> true
            runCatching {
                systemService.getDesktopService().isDesktopMode(systemService.getFloatingFeature())
            }.getOrDefault(false) -> true
            else -> false
        }
        return if (isWide) COLUMNS_WIDE else COLUMNS_NARROW
    }

    private companion object {
        const val COLUMNS_NARROW = 1
        const val COLUMNS_WIDE = 2
    }
}

/** `GetContentAreaWidth(+Impl)`. */
interface StudyGetContentAreaWidth {
    operator fun invoke(): Int
}

class StudyGetContentAreaWidthImpl @Inject constructor(
    private val systemService: StudySystemService,
    private val getColumnSize: StudyGetColumnSize,
) : StudyGetContentAreaWidth {
    override fun invoke(): Int {
        val screenWidth = runCatching { systemService.getWindowService().getScreenWidth() }
            .getOrDefault(0)
        return if (screenWidth <= 0) 0 else screenWidth / getColumnSize()
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

/** `GetSpanType`. */
class StudyGetSpanType @Inject constructor(
    private val getColumnSize: StudyGetColumnSize,
) {
    fun isFullSpan(cardType: StudyDetailCardType): Boolean = when {
        getColumnSize() == 1 -> true
        // in two-column mode the wide cards still span both
        cardType == StudyDetailCardType.Hourly -> true
        cardType == StudyDetailCardType.Alert -> true
        cardType == StudyDetailCardType.Radar -> true
        cardType == StudyDetailCardType.Indicator -> true
        else -> false
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
