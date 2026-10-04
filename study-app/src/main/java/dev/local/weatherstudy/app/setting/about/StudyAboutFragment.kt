package dev.local.weatherstudy.app.setting.about

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
 * com.sec.android.daemonapp.app.setting.about.AboutFragment
 *
 * AboutFragment (31 classes in app.setting.about) — version display and the Galaxy Store update check.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyAboutFragment : Fragment(R.layout.study_text_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        toolbar.setTitle(R.string.study_about_title)
        toolbar.back.setOnClickListener { findNavController().navigateBackOrRestart() }

        val context = requireContext()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()

        view.findViewById<TextView>(R.id.text_headline).setText(R.string.study_app_name)
        view.findViewById<TextView>(R.id.text_body).text =
            getString(R.string.study_about_version, version) + "\n\n" + getString(R.string.study_about_body)

        // the original's About screen checks Galaxy Store for an update here; a build that
        // is not distributed through a store has nothing to check, so the row links onward
        view.findViewById<android.widget.Button>(R.id.text_action).apply {
            visibility = View.VISIBLE
            setText(R.string.study_about_licences)
            setOnClickListener {
                runCatching { findNavController().navigate(R.id.action_about_to_licence) }
            }
        }
    }
}
