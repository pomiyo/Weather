package dev.local.weatherstudy.app.setting.lifestyle

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import dev.local.weatherstudy.app.common.view.StudyScreenToolbar
import dev.local.weatherstudy.app.common.view.applySystemBarPadding
import dev.local.weatherstudy.app.common.view.navigateBackOrRestart
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.app.R

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.setting.lifestyle.LifeStyleSettingsContainerFragment
 *
 * LifeStyleSettingsContainerFragment (30 classes in app.setting.lifestyle) — which activity rows the LifeStyle card shows; the setting lives in its own Room table, separate from the content.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyLifeStyleSettingsContainerFragment : Fragment(R.layout.study_text_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        toolbar.setTitle(R.string.study_activities_title)
        toolbar.back.setOnClickListener { findNavController().navigateBackOrRestart() }

        // the activity list is a provider capability (`supportLifeStyle`); with a provider
        // that does not have it there are no toggles to show, and the screen says so
        view.findViewById<TextView>(R.id.text_headline).visibility = View.GONE
        view.findViewById<TextView>(R.id.text_body).setText(R.string.study_activities_body)
    }
}
