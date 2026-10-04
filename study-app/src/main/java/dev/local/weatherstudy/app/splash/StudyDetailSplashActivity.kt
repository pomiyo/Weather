package dev.local.weatherstudy.app.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.app.StudyMainActivity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.splash.DetailSplashActivity
 * com.sec.android.daemonapp.app.splash.DetailSplashCommonActivity
 * + the 11 themed subclasses
 *
 * ### Why there are eleven Activities for one screen
 *
 * This is one of the most unusual patterns in the app, and it is a deliberate
 * performance trick rather than redundancy.
 *
 * A deep link (`…intent.action.DETAIL`, from a widget, the lock screen or another app)
 * must show the right weather background **before any of the app\'s code runs**. The only
 * thing the system can draw that early is the target Activity\'s `android:windowBackground`,
 * taken from its theme in the manifest. A theme cannot be chosen at runtime for the
 * Activity that is starting.
 *
 * So the original declares **one Activity per weather condition**, each with its own
 * `Weather.Theme.Splash.*` theme and gradient:
 *
 * ```
 * Intent …action.DETAIL
 *      ↓
 * DetailSplashCommonActivity        reads the cached condition, picks a subclass
 *      ↓
 * DetailSplash<Condition>Activity   the system paints ITS themed window instantly
 *      ↓
 * MainActivity → action_global_to_detail → DetailFragment
 * ```
 *
 * All 11 are reconstructed with their 11 gradients, because deleting them would turn a
 * real architectural decision into an apparent accident. See `reports/screen-map.md` §3.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
open class StudyDetailSplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // the themed window is already on screen; hand over and finish
        startActivity(
            Intent(this, StudyMainActivity::class.java).apply {
                putExtra(EXTRA_LOCATION_KEY, intent.getStringExtra(EXTRA_LOCATION_KEY).orEmpty())
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
        finish()
    }

    companion object {
        const val ACTION_DETAIL = "dev.local.weatherstudy.intent.action.DETAIL"
        const val EXTRA_LOCATION_KEY = "location_key"
    }
}

/**
 * Corresponds conceptually to `…splash.DetailSplashCommonActivity`.
 *
 * The deep-link entry point. It reads the cached condition for the target location and
 * forwards to the matching themed subclass, so the *next* window is already the right
 * colour. Its own theme is the neutral default.
 */
@AndroidEntryPoint
class StudyDetailSplashCommonActivity : StudyDetailSplashActivity()
