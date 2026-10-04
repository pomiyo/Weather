package dev.local.weatherstudy.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.MainActivity
 *
 * The **single** Activity for the whole phone UI. The original is declared
 * `singleTop`, exported and searchable, with theme
 * `Weather.Theme.AppCompat.Weather` (a `Theme.AppCompat.DayNight.NoActionBar`
 * descendant with `windowLayoutInDisplayCutoutMode=shortEdges`), and its layout is one
 * `NavHostFragment` over `weather_nav.xml`.
 *
 * Everything else — detail, locations, search, settings, EULA, permissions — is a
 * destination in that one graph. External entry points (widget taps, deep links,
 * notifications) all land here and use a global action that replaces the back stack.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyMainActivity : AppCompatActivity(R.layout.study_main_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        // every screen draws its weather gradient behind the system bars and pads itself;
        // asking for it explicitly makes API 26-34 behave like 35, where it is enforced
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
    }
}
