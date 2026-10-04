package dev.local.weatherstudy.system.service

import android.view.View
import android.view.Window

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the UI-facing half of
 * com.samsung.android.weather.system.service:
 * ViewService, WindowService, ListViewService, RemoteViewsService, SipService,
 * SmartTipService
 *
 * Observed responsibility: these wrap One UI's `sem*` View and Window extensions —
 * hover popups, rounded-corner tinting, accessibility highlight buttons, multi-window
 * control, display-cutout colouring, long-press multi-selection in lists, window blur.
 *
 * They are the clearest case of the pattern: every method here is a Samsung platform
 * call, and every one has a safe no-op AOSP fallback. Keeping them as interfaces means
 * the Fragments and ViewHolders can call them unconditionally, with no
 * `if (isSamsung)` branches anywhere in the UI code.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyViewService {
    fun setRoundedCorners(view: View, corners: Int)
    fun setRoundedCornerColor(view: View, corners: Int, color: Int)
    fun setHoverPopupType(view: View, type: Int)
    fun dismissHoverPopup(view: View)
    fun requestAccessibilityFocus(view: View)
    fun clearAccessibilityFocus(view: View)
    fun setAccessibilityHighlightButtonEnabled(textView: android.widget.TextView, enable: Boolean)
    fun setDialogAnchorView(view: View, anchor: View?, gravity: Int)
    fun setUpButtonVisibility(view: View, visible: Boolean): Boolean
    fun setUpButtonClickListener(view: View, listener: View.OnClickListener?)
    fun setWindowModeBlur(view: View, blurRadius: Int, backgroundCornerRadius: Float, backgroundColor: Int)
}

/** Corresponds conceptually to `…system.service.WindowService`. */
interface StudyWindowService {
    fun getScreenWidth(): Int
    fun getScreenHeight(): Int
    fun getScreenInfo(): android.graphics.Point
    fun getMultiWindowMode(): Int
    fun isMultiWindowModeNone(): Boolean
    fun setMultiWindowEnabled(window: Window, enabled: Boolean)
    fun getResizeFullScreenWindowOnSoftInputFlag(): Int
    fun setDisplayCutoutBackgroundColor(window: Window, color: Int)
    fun setNavigationBarIconColor(window: Window, dark: Boolean)
    fun addExtensionFlags(window: Window, flags: Int)
    fun dismissKeyguard(activity: android.app.Activity)
    fun requestSystemKeyEvent(keyCode: Int, component: android.content.ComponentName, request: Boolean): Boolean
    fun setPendingIntentAfterUnlock(
        context: android.content.Context,
        intent: android.app.PendingIntent,
        fillIn: android.content.Intent?,
    )
}

/**
 * Corresponds conceptually to `…system.service.ListViewService`.
 *
 * Observed responsibility: One UI's long-press multi-selection, which is how the
 * locations list implements drag-select. The AOSP fallback has no equivalent, so the
 * locations screen degrades to the checkbox mode.
 */
interface StudyListViewService {
    fun setLongPressMultiSelectionEnabled(view: View, enabled: Boolean)
    fun setLongPressMultiSelectionListener(view: View, listener: Any?)
    fun setMultiSelectionListener(view: View, listener: Any?)
    fun setDragBlockEnabled(view: View, enabled: Boolean)
    fun setCtrlKeyPressed(view: View, pressed: Boolean)
    fun setBottomColor(view: View, color: Int)
    fun pointToNearPosition(view: View, x: Float, y: Float): Int
}

/**
 * Corresponds conceptually to `…system.service.RemoteViewsService`.
 *
 * Observed responsibility: one method, and it is the reason the widgets can animate —
 * `setAnimation` on a RemoteViews ImageView is a Samsung extension. On AOSP the refresh
 * spinner in a widget simply does not spin.
 */
interface StudyRemoteViewsService {
    fun setAnimation(views: android.widget.RemoteViews, imageViewId: Int, resId: Int, start: Boolean)
}

/** Corresponds conceptually to `…system.service.SipService` — soft-input/keyboard. */
interface StudySipService {
    fun hasDeviceKeyboard(inputManager: Any?): Boolean
    fun isAccessoryKeyboard(inputMethodManager: Any?): Boolean
    fun minimizeSoftInput(inputMethodManager: Any?, windowToken: android.os.IBinder?, height: Int): Boolean
}

/**
 * Corresponds conceptually to `…system.service.SmartTipService`.
 *
 * Observed responsibility: One UI's "smart tip" bubble, used for the precise-location
 * hint on the detail screen (`PreciseLocationTips`). The whole builder surface is
 * Samsung-only; the AOSP fallback shows nothing, which is why the reconstruction also
 * keeps a plain `StudyPreciseLocationTips` View as the visible fallback.
 */
interface StudySmartTipService {
    fun showSmartTip(anchor: View, message: CharSequence): Boolean
    fun dismissSmartTip()
    fun releaseInstance()
    fun setDirection(direction: Int)
    fun setExpanded(expanded: Boolean)
    fun setTargetPosition(x: Int, y: Int)
    fun setTipBgColor(color: Int)
    fun setBorderColor(color: Int)
    fun setMessageTextColor(color: Int)
    fun setActionTextColor(color: Int)
    fun setStateChangeListener(listener: Any?)
}
