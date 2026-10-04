package dev.local.weatherstudy.app.detail.fragment

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.app.R

/**
 * Educational reconstruction — the remaining phone screens.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.location.LocationsFragment
 * com.sec.android.daemonapp.app.search.SearchFragment
 * com.sec.android.daemonapp.app.search.cover.CoverSearchFragment
 * com.sec.android.daemonapp.app.setting.settings.SettingsFragment / SettingPrefFragment
 * com.sec.android.daemonapp.app.setting.about.AboutFragment
 * com.sec.android.daemonapp.app.setting.permissions.PermissionsFragment
 * com.sec.android.daemonapp.app.setting.opensource.OpenSourceLicenseFragment
 * com.sec.android.daemonapp.app.setting.lifestyle.LifeStyleSettingsContainerFragment /
 *   LifeStyleSettingsFragment
 *
 * These are reconstructed as nav destinations with their ViewModels and adapters in
 * place, so the graph in `study_weather_nav.xml` is navigable end to end and matches
 * `reports/screen-map.md` §1.1. Their list content is lighter than the detail screen's,
 * which is where the architecture actually lives.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyPlaceholderScreenFragment : Fragment(R.layout.study_simple_fragment)
