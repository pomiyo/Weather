package dev.local.weatherstudy.ui.common.detail.state

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.ui.common.detail.state.DetailIntent (+ its assisted
 * `DetailIntent.Factory`, visible in the APK as `DetailIntent_Factory_Impl` and
 * `C1470DetailIntent_Factory`)
 * com.samsung.android.weather.ui.common.detail.state.DetailSideEffect
 *
 * ### The Orbit MVI contract
 *
 * `DetailViewModel` is `ContainerHost<DetailState, DetailSideEffect>`, and `DetailIntent`
 * is injected into it through an **assisted factory** — which is the detail worth
 * preserving. The intent class needs the container to reduce into, and the container
 * belongs to the ViewModel, so Dagger cannot build the intent on its own: the ViewModel
 * supplies the container at construction time and Dagger supplies the use cases.
 *
 * That is why `DetailIntent` is not simply a sealed class of actions. The sealed-class
 * part is [StudyDetailAction]; `DetailIntent` is the *reducer* that handles them.
 *
 * ```
 * Fragment ──dispatch(action)──► StudyDetailIntent ──reduce──► StudyDetailState
 *                                      │
 *                                      └──postSideEffect──► StudyDetailSideEffect
 * ```
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
sealed interface StudyDetailAction {
    data object Load : StudyDetailAction
    data object Refresh : StudyDetailAction
    data class SelectLocation(val key: String) : StudyDetailAction
    data class SelectPage(val index: Int) : StudyDetailAction
    data class CardClicked(val cardType: StudyDetailCardType) : StudyDetailAction
    data class WebLinkClicked(val url: String) : StudyDetailAction
    data class InsightSwiped(val index: Int) : StudyDetailAction
    data object GoToLocations : StudyDetailAction
    data object GoToSearch : StudyDetailAction
    data object GoToSettings : StudyDetailAction
    data object GoToLifeStyleSettings : StudyDetailAction
    data object GoToSmartThings : StudyDetailAction
    data class ConfigurationChanged(val configuration: StudyDetailConfiguration) : StudyDetailAction
}

/**
 * Corresponds conceptually to `…detail.state.DetailSideEffect`.
 *
 * Observed responsibility: the one-shot half of the MVI contract — navigation, toasts,
 * dialogs, scroll commands. These are a `Flow`, not state, because replaying them on
 * configuration change would navigate twice.
 */
sealed interface StudyDetailSideEffect {
    data class NavigateToLocations(val fromWidgetId: Int = -1) : StudyDetailSideEffect
    data object NavigateToSearch : StudyDetailSideEffect
    data object NavigateToSettings : StudyDetailSideEffect
    data object NavigateToLifeStyleSettings : StudyDetailSideEffect
    data class OpenWebLink(val url: String) : StudyDetailSideEffect
    data object LaunchSmartThings : StudyDetailSideEffect
    data object LaunchSamsungNews : StudyDetailSideEffect
    data class ShowRefreshResult(val result: StudyDetailRefreshResult) : StudyDetailSideEffect
    data class ScrollToCard(val cardType: StudyDetailCardType) : StudyDetailSideEffect
    data object ShowPreciseLocationTip : StudyDetailSideEffect
}
