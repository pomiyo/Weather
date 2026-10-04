package dev.local.weatherstudy.ui.common.detail.state

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.ui.common.detail.state.DetailCardType
 *
 * Observed responsibilities:
 * - the 20 card kinds the detail RecyclerView can show, as a **sealed hierarchy of
 *   objects** (the original is an abstract class with 20 nested `object` subclasses)
 * - it doubles as the adapter's **view type**: `DetailAdapter.getItemViewType` returns
 *   `cards[position].hashCode()`, and the view-holder factory compares that int against
 *   `DetailCardType.<Variant>.INSTANCE.hashCode()` in a 20-branch chain
 *
 * Using object identity as the view type rather than an enum ordinal is unusual and is
 * reconstructed as-is — see `reports/weather-card-map.md` §1.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
sealed class StudyDetailCardType {
    data object Indicator : StudyDetailCardType()
    data object Alert : StudyDetailCardType()
    data object Hourly : StudyDetailCardType()
    data object Precipitation : StudyDetailCardType()
    data object Daily : StudyDetailCardType()
    data object Insight : StudyDetailCardType()
    data object SmartThings : StudyDetailCardType()
    data object Radar : StudyDetailCardType()
    data object Video : StudyDetailCardType()
    data object TodayStoriesAndVideo : StudyDetailCardType()
    data object News : StudyDetailCardType()
    data object NewsAndVideo : StudyDetailCardType()
    data object AirIndex : StudyDetailCardType()
    data object Index : StudyDetailCardType()
    data object Sun : StudyDetailCardType()
    data object Moon : StudyDetailCardType()
    data object SunAndMoon : StudyDetailCardType()
    data object BottomIndex : StudyDetailCardType()
    data object LifeTips : StudyDetailCardType()
    data object LifeStyle : StudyDetailCardType()

    /** the stable name used by `StudyOrderingPolicy` and `GetCardOrder` */
    val typeName: String get() = this::class.simpleName.orEmpty()

    companion object {
        /**
         * All 20, in the policy's default order.
         *
         * Lazy on purpose: touching any one variant initialises this sealed class, and so
         * its companion, BEFORE that variant's own instance exists. An eager list built
         * here would capture `null` for whichever variant triggered the load.
         */
        val ALL: List<StudyDetailCardType> by lazy {
            listOf(
                Alert, Insight, Hourly, Precipitation, Daily, AirIndex, Index,
                SunAndMoon, Sun, Moon, BottomIndex, LifeStyle, LifeTips, Radar,
                SmartThings, NewsAndVideo, News, Video, TodayStoriesAndVideo, Indicator,
            )
        }

        fun fromName(name: String): StudyDetailCardType? = ALL.firstOrNull { it.typeName == name }
    }
}

/**
 * Corresponds conceptually to `…detail.state.DetailCardSpanState` and
 * `DetailCardVisibleState`.
 *
 * Observed responsibility: whether a card spans the full grid width. It is read in
 * `onBindViewHolder` and written onto
 * `StaggeredGridLayoutManager.LayoutParams.isFullSpan` — the only thing that method does.
 */
data class StudyDetailCardSpanState(
    val cardType: StudyDetailCardType,
    val isFullSpan: Boolean,
)

/** Corresponds conceptually to `…detail.state.DetailCardVisibleState`. */
data class StudyDetailCardVisibleState(
    val cardType: StudyDetailCardType,
    val isVisible: Boolean,
)
