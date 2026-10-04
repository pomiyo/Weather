package dev.local.weatherstudy.app.splash

import dagger.hilt.android.AndroidEntryPoint

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.splash.DetailSplashSunnyNightActivity
 *
 * Declared in the manifest with `android:theme="@style/Study.Theme.Splash.SunnyNight"`, whose
 * `windowBackground` is `study_splash_gradient_*`. The class body is empty on purpose —
 * **the manifest theme is the entire contribution**. See [StudyDetailSplashActivity] for
 * why this is eleven classes.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyDetailSplashSunnyNightActivity : StudyDetailSplashActivity()
