package dev.local.weatherstudy.app.detail.usecase

import dev.local.weatherstudy.domain.entity.device.StudyDeviceType
import dev.local.weatherstudy.domain.policy.StudyOrderingPolicy
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.system.service.StudySystemService
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
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
 * Observed responsibility: the order is read from the **policy** package, then filtered.
 * The original lets a user setting and the provider's capabilities both narrow it, which
 * is why it is a use case and not a constant list in the adapter.
 */
interface StudyGetCardOrder {
    suspend operator fun invoke(): List<StudyDetailCardType>
}

class StudyGetCardOrderImpl @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyGetCardOrder {
    override suspend fun invoke(): List<StudyDetailCardType> =
        StudyOrderingPolicy.DEFAULT_DETAIL_CARD_ORDER.mapNotNull { StudyDetailCardType.fromName(it) }
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
