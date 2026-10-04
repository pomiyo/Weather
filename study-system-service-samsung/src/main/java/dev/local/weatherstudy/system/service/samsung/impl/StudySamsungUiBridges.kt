package dev.local.weatherstudy.system.service.samsung.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.Window
import android.widget.RemoteViews
import android.widget.TextView
import dev.local.weatherstudy.system.service.StudyListViewService
import dev.local.weatherstudy.system.service.StudyRemoteViewsService
import dev.local.weatherstudy.system.service.StudyShortcutService
import dev.local.weatherstudy.system.service.StudySipService
import dev.local.weatherstudy.system.service.StudySmartTipService
import dev.local.weatherstudy.system.service.StudyViewService
import dev.local.weatherstudy.system.service.StudyWindowService
import dev.local.weatherstudy.system.service.samsung.StudySamsungApiUnavailable
import javax.inject.Inject

/**
 * Educational reconstruction (stubs — see [StudySamsungApiUnavailable]).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.system.service.sep.impl.SepViewService
 * …SepWindowService, …SepListViewService, …SepRemoteViewsService, …SepSipService,
 * …SepSmartTipService
 *
 * Original dependency: the `sem*` extension methods One UI adds to ordinary framework
 * classes — `View.semSetRoundedCorners`, `View.semSetHoverPopupType`,
 * `TextView.semSetAccessibilityHighlightButtonEnabled`,
 * `Window.semSetDisplayCutoutBackgroundColor`, `SemWindowManager`,
 * `AbsListView.semSetLongPressMultiSelectionEnabled`, `RemoteViews.semSetAnimation`,
 * `SemSmartTipManager`, `SemInputMethodManager.semMinimizeSoftInput`.
 *
 * Why unavailable: they are additions to the framework classes themselves, so they
 * cannot be shimmed — only the device's `framework.jar` has them.
 *
 * Where used: the detail screen's card chrome and collapsing toolbar, the locations
 * list's drag-select, the widget refresh animation, the precise-location tip bubble,
 * and the keyboard handling in search.
 *
 * Replacement behaviour: throw, naming the method. In normal operation the DI graph
 * binds the AOSP family instead, where each of these is a documented no-op.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudySamsungViewServiceBridge @Inject constructor() : StudyViewService {
    override fun setRoundedCorners(view: View, corners: Int): Unit = throw u("View.semSetRoundedCorners")
    override fun setRoundedCornerColor(view: View, corners: Int, color: Int): Unit =
        throw u("View.semSetRoundedCornerColor")
    override fun setHoverPopupType(view: View, type: Int): Unit = throw u("View.semSetHoverPopupType")
    override fun dismissHoverPopup(view: View): Unit = throw u("View.semDismissHoverPopup")
    override fun requestAccessibilityFocus(view: View): Unit = throw u("View.semRequestAccessibilityFocus")
    override fun clearAccessibilityFocus(view: View): Unit = throw u("View.semClearAccessibilityFocus")
    override fun setAccessibilityHighlightButtonEnabled(textView: TextView, enable: Boolean): Unit =
        throw u("TextView.semSetAccessibilityHighlightButtonEnabled")
    override fun setDialogAnchorView(view: View, anchor: View?, gravity: Int): Unit =
        throw u("SemDialog anchor API")
    override fun setUpButtonVisibility(view: View, visible: Boolean): Boolean =
        throw u("SeslToolbar up-button API")
    override fun setUpButtonClickListener(view: View, listener: View.OnClickListener?): Unit =
        throw u("SeslToolbar up-button API")
    override fun setWindowModeBlur(
        view: View,
        blurRadius: Int,
        backgroundCornerRadius: Float,
        backgroundColor: Int,
    ): Unit = throw u("View.semSetBlurInfo")

    private fun u(api: String) = StudySamsungApiUnavailable(api)
}

/** Bridge for `SepWindowService`. */
class StudySamsungWindowServiceBridge @Inject constructor() : StudyWindowService {
    override fun getScreenWidth(): Int = throw u("SemWindowManager")
    override fun getScreenHeight(): Int = throw u("SemWindowManager")
    override fun getScreenInfo(): Point = throw u("SemWindowManager")
    override fun getMultiWindowMode(): Int = throw u("SemWindowManager multi-window mode")
    override fun isMultiWindowModeNone(): Boolean = throw u("SemWindowManager multi-window mode")
    override fun setMultiWindowEnabled(window: Window, enabled: Boolean): Unit =
        throw u("Window.semSetMultiWindowEnabled")
    override fun getResizeFullScreenWindowOnSoftInputFlag(): Int =
        throw u("SemWindowManager soft-input resize flag")
    override fun setDisplayCutoutBackgroundColor(window: Window, color: Int): Unit =
        throw u("Window.semSetDisplayCutoutBackgroundColor")
    override fun setNavigationBarIconColor(window: Window, dark: Boolean): Unit =
        throw u("Window.semSetNavigationBarIconColor")
    override fun addExtensionFlags(window: Window, flags: Int): Unit =
        throw u("Window.semAddExtensionFlags")
    override fun dismissKeyguard(activity: Activity): Unit = throw u("SemKeyguardManager")
    override fun requestSystemKeyEvent(
        keyCode: Int,
        component: ComponentName,
        request: Boolean,
    ): Boolean = throw u("SemWindowManager.requestSystemKeyEvent")
    override fun setPendingIntentAfterUnlock(
        context: Context,
        intent: android.app.PendingIntent,
        fillIn: android.content.Intent?,
    ): Unit = throw u("SemKeyguardManager pending-intent-after-unlock")

    private fun u(api: String) = StudySamsungApiUnavailable(api)
}

/** Bridge for `SepListViewService`. */
class StudySamsungListViewServiceBridge @Inject constructor() : StudyListViewService {
    override fun setLongPressMultiSelectionEnabled(view: View, enabled: Boolean): Unit =
        throw u("AbsListView.semSetLongPressMultiSelectionEnabled")
    override fun setLongPressMultiSelectionListener(view: View, listener: Any?): Unit =
        throw u("AbsListView.semSetLongPressMultiSelectionListener")
    override fun setMultiSelectionListener(view: View, listener: Any?): Unit =
        throw u("AbsListView.semSetMultiSelectedListener")
    override fun setDragBlockEnabled(view: View, enabled: Boolean): Unit =
        throw u("AbsListView.semSetDragBlockEnabled")
    override fun setCtrlKeyPressed(view: View, pressed: Boolean): Unit =
        throw u("AbsListView.semSetCtrlKeyPressed")
    override fun setBottomColor(view: View, color: Int): Unit = throw u("AbsListView.semSetBottomColor")
    override fun pointToNearPosition(view: View, x: Float, y: Float): Int =
        throw u("AbsListView.semPointToNearPosition")

    private fun u(api: String) = StudySamsungApiUnavailable(api)
}

/** Bridge for `SepRemoteViewsService` — the widget refresh animation. */
class StudySamsungRemoteViewsServiceBridge @Inject constructor() : StudyRemoteViewsService {
    override fun setAnimation(views: RemoteViews, imageViewId: Int, resId: Int, start: Boolean): Unit =
        throw StudySamsungApiUnavailable("RemoteViews.semSetAnimation")
}

/** Bridge for `SepSipService`. */
class StudySamsungSipServiceBridge @Inject constructor() : StudySipService {
    override fun hasDeviceKeyboard(inputManager: Any?): Boolean =
        throw StudySamsungApiUnavailable("SemInputManager.semIsDeviceKeyboard")
    override fun isAccessoryKeyboard(inputMethodManager: Any?): Boolean =
        throw StudySamsungApiUnavailable("SemInputMethodManager accessory-keyboard query")
    override fun minimizeSoftInput(
        inputMethodManager: Any?,
        windowToken: android.os.IBinder?,
        height: Int,
    ): Boolean = throw StudySamsungApiUnavailable("SemInputMethodManager.semMinimizeSoftInput")
}

/** Bridge for `SepSmartTipService`. */
class StudySamsungSmartTipServiceBridge @Inject constructor() : StudySmartTipService {
    override fun showSmartTip(anchor: View, message: CharSequence): Boolean = throw u()
    override fun dismissSmartTip(): Unit = throw u()
    override fun releaseInstance(): Unit = throw u()
    override fun setDirection(direction: Int): Unit = throw u()
    override fun setExpanded(expanded: Boolean): Unit = throw u()
    override fun setTargetPosition(x: Int, y: Int): Unit = throw u()
    override fun setTipBgColor(color: Int): Unit = throw u()
    override fun setBorderColor(color: Int): Unit = throw u()
    override fun setMessageTextColor(color: Int): Unit = throw u()
    override fun setActionTextColor(color: Int): Unit = throw u()
    override fun setStateChangeListener(listener: Any?): Unit = throw u()

    private fun u() = StudySamsungApiUnavailable("SemSmartTipManager")
}

/** Bridge for `SepShortcutService`. */
class StudySamsungShortcutServiceBridge @Inject constructor() : StudyShortcutService {
    override fun hasShortcut(): Boolean = throw u()
    override fun isRequestPinShortcutSupported(homeOnlyMode: Boolean): Boolean = throw u()
    override fun addShortcut(
        component: ComponentName,
        labelRes: Int,
        iconRes: Int,
        homeOnlyMode: Boolean,
    ): Unit = throw u()

    private fun u() = StudySamsungApiUnavailable("Samsung launcher home-only pin API")
}
