package dev.local.weatherstudy.system.service.android.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.Window
import android.widget.RemoteViews
import android.widget.TextView
import dev.local.weatherstudy.system.service.StudyActivityService
import dev.local.weatherstudy.system.service.StudyEdgeManager
import dev.local.weatherstudy.system.service.StudyFoldStateListener
import dev.local.weatherstudy.system.service.StudyFoldStateService
import dev.local.weatherstudy.system.service.StudyListViewService
import dev.local.weatherstudy.system.service.StudyPackageService
import dev.local.weatherstudy.system.service.StudyRemoteViewsService
import dev.local.weatherstudy.system.service.StudyShortcutService
import dev.local.weatherstudy.system.service.StudySipService
import dev.local.weatherstudy.system.service.StudySmartTipService
import dev.local.weatherstudy.system.service.StudyViewService
import dev.local.weatherstudy.system.service.StudyWidgetService
import dev.local.weatherstudy.system.service.StudyWindowService
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the UI implementations in
 * com.samsung.android.weather.system.service.android.impl
 *
 * Observed responsibility: the AOSP fallbacks for One UI's View/Window extensions.
 * Most are **deliberate no-ops** — that is the design. Because the interface exists,
 * the detail Fragment can call `viewService.setRoundedCornerColor(...)` unconditionally
 * and simply get nothing on stock Android, instead of branching.
 *
 * Where a public equivalent does exist (screen size, keyguard dismissal, component
 * enable/disable, pinned shortcuts) it is implemented properly.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyAndroidViewService @Inject constructor() : StudyViewService {
    override fun setRoundedCorners(view: View, corners: Int) = Unit
    override fun setRoundedCornerColor(view: View, corners: Int, color: Int) = Unit
    override fun setHoverPopupType(view: View, type: Int) = Unit
    override fun dismissHoverPopup(view: View) = Unit

    override fun requestAccessibilityFocus(view: View) {
        view.performAccessibilityAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
            null,
        )
    }

    override fun clearAccessibilityFocus(view: View) {
        view.performAccessibilityAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
            null,
        )
    }

    override fun setAccessibilityHighlightButtonEnabled(textView: TextView, enable: Boolean) = Unit
    override fun setDialogAnchorView(view: View, anchor: View?, gravity: Int) = Unit
    override fun setUpButtonVisibility(view: View, visible: Boolean): Boolean = false
    override fun setUpButtonClickListener(view: View, listener: View.OnClickListener?) = Unit
    override fun setWindowModeBlur(
        view: View,
        blurRadius: Int,
        backgroundCornerRadius: Float,
        backgroundColor: Int,
    ) = Unit
}

/** Corresponds conceptually to `…android.impl.AndroidWindowService`. */
class StudyAndroidWindowService @Inject constructor(
    private val context: Context,
) : StudyWindowService {

    override fun getScreenWidth(): Int = context.resources.displayMetrics.widthPixels
    override fun getScreenHeight(): Int = context.resources.displayMetrics.heightPixels
    override fun getScreenInfo(): Point = Point(getScreenWidth(), getScreenHeight())

    override fun getMultiWindowMode(): Int = MULTI_WINDOW_NONE
    override fun isMultiWindowModeNone(): Boolean = true
    override fun setMultiWindowEnabled(window: Window, enabled: Boolean) = Unit
    override fun getResizeFullScreenWindowOnSoftInputFlag(): Int = 0

    override fun setDisplayCutoutBackgroundColor(window: Window, color: Int) = Unit

    override fun setNavigationBarIconColor(window: Window, dark: Boolean) {
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightNavigationBars = dark
    }

    override fun addExtensionFlags(window: Window, flags: Int) = Unit

    override fun dismissKeyguard(activity: Activity) {
        activity.setShowWhenLocked(true)
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE)
            as? android.app.KeyguardManager
        keyguard?.requestDismissKeyguard(activity, null)
    }

    override fun requestSystemKeyEvent(
        keyCode: Int,
        component: ComponentName,
        request: Boolean,
    ): Boolean = false

    override fun setPendingIntentAfterUnlock(
        context: Context,
        intent: android.app.PendingIntent,
        fillIn: android.content.Intent?,
    ) = Unit

    private companion object {
        const val MULTI_WINDOW_NONE = 0
    }
}

/** Corresponds conceptually to `…android.impl.AndroidListViewService`. */
class StudyAndroidListViewService @Inject constructor() : StudyListViewService {
    override fun setLongPressMultiSelectionEnabled(view: View, enabled: Boolean) = Unit
    override fun setLongPressMultiSelectionListener(view: View, listener: Any?) = Unit
    override fun setMultiSelectionListener(view: View, listener: Any?) = Unit
    override fun setDragBlockEnabled(view: View, enabled: Boolean) = Unit
    override fun setCtrlKeyPressed(view: View, pressed: Boolean) = Unit
    override fun setBottomColor(view: View, color: Int) = Unit
    override fun pointToNearPosition(view: View, x: Float, y: Float): Int = -1
}

/**
 * Corresponds conceptually to `…android.impl.AndroidRemoteViewsService`.
 *
 * No AOSP equivalent: RemoteViews cannot start an animation. The widget refresh spinner
 * therefore shows a static frame on stock Android.
 */
class StudyAndroidRemoteViewsService @Inject constructor() : StudyRemoteViewsService {
    override fun setAnimation(views: RemoteViews, imageViewId: Int, resId: Int, start: Boolean) {
        views.setImageViewResource(imageViewId, resId)
    }
}

/** Corresponds conceptually to `…android.impl.AndroidActivityService`. */
class StudyAndroidActivityService @Inject constructor() : StudyActivityService {
    override fun isResumed(activity: Activity): Boolean = !activity.isFinishing && !activity.isDestroyed
}

/** Corresponds conceptually to `…android.impl.AndroidFoldStateService`. */
class StudyAndroidFoldStateService @Inject constructor() : StudyFoldStateService {
    override fun isFolded(): Boolean = false
    override fun isFlipCoverScreen(context: Context): Boolean = false
    override fun registerFoldStateListener(listener: StudyFoldStateListener, handler: android.os.Handler?) = Unit
    override fun unregisterFoldStateListener(listener: StudyFoldStateListener) = Unit
}

/** Corresponds conceptually to `…android.impl.AndroidEdgeManager`. */
class StudyAndroidEdgeManager @Inject constructor() : StudyEdgeManager {
    override fun isEdgeEnabled(context: Context): Boolean = false
}

/** Corresponds conceptually to `…android.impl.AndroidWidgetService`. */
class StudyAndroidWidgetService @Inject constructor() : StudyWidgetService {
    override fun getAppWidgetColumnSpan(): String = ""
    override fun getAppWidgetRowSpan(): String = ""
}

/** Corresponds conceptually to `…android.impl.AndroidPackageService`. */
class StudyAndroidPackageService @Inject constructor(
    private val context: Context,
) : StudyPackageService {

    override fun getSystemFeatureLevel(packageName: String): Int = 0

    override fun isSecureFolderEnabled(userId: Int): Boolean = false

    /** this is how the disabled launcher activity is turned on; see StudyAppLauncherActivity */
    override fun setComponentEnable(componentName: ComponentName, enable: Boolean, flag: Int) {
        val state = if (enable) {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(componentName, state, flag)
    }
}

/** Corresponds conceptually to `…android.impl.AndroidShortcutService`. */
class StudyAndroidShortcutService @Inject constructor(
    private val context: Context,
) : StudyShortcutService {

    override fun hasShortcut(): Boolean =
        androidx.core.content.pm.ShortcutManagerCompat.getDynamicShortcuts(context).isNotEmpty()

    override fun isRequestPinShortcutSupported(homeOnlyMode: Boolean): Boolean =
        androidx.core.content.pm.ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    override fun addShortcut(
        component: ComponentName,
        labelRes: Int,
        iconRes: Int,
        homeOnlyMode: Boolean,
    ) {
        val info = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, component.className)
            .setShortLabel(context.getString(labelRes))
            .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, iconRes))
            .setIntent(android.content.Intent(android.content.Intent.ACTION_MAIN).setComponent(component))
            .build()
        androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }
}

/** Corresponds conceptually to `…android.impl.AndroidSipService`. */
class StudyAndroidSipService @Inject constructor() : StudySipService {
    override fun hasDeviceKeyboard(inputManager: Any?): Boolean = false
    override fun isAccessoryKeyboard(inputMethodManager: Any?): Boolean = false
    override fun minimizeSoftInput(
        inputMethodManager: Any?,
        windowToken: android.os.IBinder?,
        height: Int,
    ): Boolean = false
}

/** Corresponds conceptually to `…android.impl.AndroidSmartTipService` — all no-ops. */
class StudyAndroidSmartTipService @Inject constructor() : StudySmartTipService {
    override fun showSmartTip(anchor: View, message: CharSequence): Boolean = false
    override fun dismissSmartTip() = Unit
    override fun releaseInstance() = Unit
    override fun setDirection(direction: Int) = Unit
    override fun setExpanded(expanded: Boolean) = Unit
    override fun setTargetPosition(x: Int, y: Int) = Unit
    override fun setTipBgColor(color: Int) = Unit
    override fun setBorderColor(color: Int) = Unit
    override fun setMessageTextColor(color: Int) = Unit
    override fun setActionTextColor(color: Int) = Unit
    override fun setStateChangeListener(listener: Any?) = Unit
}
