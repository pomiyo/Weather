package dev.local.weatherstudy.app.setting.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.common.view.StudyScreenToolbar
import dev.local.weatherstudy.app.common.view.applySystemBarPadding
import dev.local.weatherstudy.app.common.view.restartGraph
import dev.local.weatherstudy.condition.conditions.checker.StudyCheckLocationPermission
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.setting.permissions.PermissionsFragment
 * (+ the `PermissionNotice` / `LocationPermission` dialogs the condition chain raises)
 *
 * Observed responsibility: one destination with two entrances.
 *
 * - **from the startup chain**, when `LocationPermissionCondition` is not satisfied: it
 *   asks for the grant, and a grant re-enters the graph so the chain continues to
 *   "get current location". Declining is a real choice — the user can search for a city
 *   instead, and is then not asked again on every launch.
 * - **from Settings**: it shows the current state and links to the system settings page,
 *   which is the only place a grant can be withdrawn.
 *
 * The original holds its location grant by firmware policy (`GRANTED_BY_DEFAULT`), so its
 * version of this screen is mostly informational. A debug-signed reconstruction has to
 * ask like any other app, and that difference is the point of the screen.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyPermissionsFragment : Fragment(R.layout.study_message_fragment) {

    @Inject lateinit var checkLocationPermission: StudyCheckLocationPermission

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        askedThisVisit = true
        if (grants.values.any { it }) onGranted() else render()
    }

    private var askedThisVisit = false

    /** reached by a global action from the start destination, rather than pushed from Settings */
    private val isStartupGate: Boolean
        get() = findNavController().previousBackStackEntry == null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarPadding()

        val toolbar = StudyScreenToolbar(view)
        toolbar.setTitle(R.string.study_permission_screen_title)
        toolbar.back.setOnClickListener {
            if (isStartupGate) requireActivity().finish() else findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        // the grant may have changed in the system settings while this screen was away
        if (isStartupGate && checkLocationPermission.hasForeground()) onGranted() else render()
    }

    private fun render() {
        val view = view ?: return
        val granted = checkLocationPermission.hasForeground()
        val title = view.findViewById<TextView>(R.id.message_title)
        val body = view.findViewById<TextView>(R.id.message_body)
        val primary = view.findViewById<Button>(R.id.message_primary)
        val secondary = view.findViewById<Button>(R.id.message_secondary)

        title.setText(if (granted) R.string.study_permission_granted_title else R.string.study_permission_title)
        body.setText(if (granted) R.string.study_permission_granted_body else R.string.study_permission_body)

        // a refusal the system will no longer prompt for can only be undone in settings
        val mustUseSettings = granted || (askedThisVisit && !canAskAgain())
        primary.setText(
            if (mustUseSettings) R.string.study_permission_open_settings else R.string.study_permission_allow,
        )
        primary.setOnClickListener {
            if (mustUseSettings) openAppSettings() else requestLocation.launch(LOCATION_PERMISSIONS)
        }

        secondary.visibility = if (isStartupGate) View.VISIBLE else View.GONE
        secondary.setText(R.string.study_permission_search)
        secondary.setOnClickListener {
            runCatching { findNavController().navigate(R.id.action_global_to_search) }
        }
    }

    private fun onGranted() {
        if (isStartupGate) findNavController().restartGraph() else render()
    }

    private fun canAskAgain(): Boolean =
        LOCATION_PERMISSIONS.any { shouldShowRequestPermissionRationale(it) }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null),
            ),
        )
    }

    private companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
