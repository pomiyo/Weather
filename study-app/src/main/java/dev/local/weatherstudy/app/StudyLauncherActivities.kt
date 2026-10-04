package dev.local.weatherstudy.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.app.AppLauncherActivity
 *
 * ### Declared `android:enabled="false"` — and that is the point
 *
 * Samsung Weather normally has **no app icon**. The original declares this launcher
 * activity disabled, and the app is reached through widgets, the lock screen,
 * notifications and the `…action.DETAIL` deep link instead. It can be enabled at runtime
 * via `StudyPackageService.setComponentEnable` — which is why that method exists on the
 * platform-service interface.
 *
 * `reports/HANDOFF.md` §5 records this as a trap: the absence of an icon is not evidence
 * the app is not installed.
 *
 * Kept disabled in the reconstruction's manifest for fidelity; see
 * [StudyDevLauncherActivity] for how the project is still launchable.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyAppLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, StudyMainActivity::class.java))
        finish()
    }
}

/**
 * Reconstruction-only — **no counterpart in the original**.
 *
 * Because [StudyAppLauncherActivity] is faithfully disabled, the project would have no
 * launchable entry point from Android Studio. This always-enabled alias provides one.
 * Declared as a deliberate deviation in `reports/screen-map.md` §5.
 */
@AndroidEntryPoint
class StudyDevLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, StudyMainActivity::class.java))
        finish()
    }
}

/**
 * Corresponds conceptually to `com.samsung.android.weather.app.AppSearchableActivity`.
 *
 * The target for the system's global search (`res/xml/searchable.xml` in the original).
 * It forwards into the nav graph's search destination.
 */
@AndroidEntryPoint
class StudyAppSearchableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, StudyMainActivity::class.java))
        finish()
    }
}
