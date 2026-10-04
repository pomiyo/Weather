package dev.local.weatherstudy.app.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.local.weatherstudy.app.R
import kotlinx.coroutines.launch

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.main.MainFragment
 * com.sec.android.daemonapp.app.main.MainNavigator / MainNaviDelegation
 *
 * The nav graph's **start destination, with no UI of its own**. It observes
 * [StudyMainViewModel]'s side effects and performs the navigation. That is the whole
 * class — which is why the original's `MainFragment` is small while `MainViewModel` and
 * `MainStateProvider` are not.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@AndroidEntryPoint
class StudyMainFragment : Fragment(R.layout.study_main_fragment) {

    private val viewModel: StudyMainViewModel by viewModels()
    private val navigator = StudyMainNavigator()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.container.sideEffectFlow.collect { effect ->
                when (effect) {
                    is StudyMainSideEffect.Navigate ->
                        navigator.navigate(findNavController(), effect.destination)
                    StudyMainSideEffect.RequestLocationPermission -> Unit
                    StudyMainSideEffect.Finish -> requireActivity().finish()
                }
            }
        }
    }
}

/**
 * Corresponds conceptually to `…main.MainNavigator` / `MainNaviDelegation`.
 *
 * Observed responsibility: turn a [StudyMainDestination] into a global action. It is a
 * separate class in the original so the ViewModel never touches a `NavController`.
 */
class StudyMainNavigator {
    fun navigate(
        navController: androidx.navigation.NavController,
        destination: StudyMainDestination,
    ) {
        val actionId = when (destination) {
            StudyMainDestination.Eula -> R.id.action_global_to_eula
            StudyMainDestination.PermissionNotice,
            StudyMainDestination.AppPermission,
            StudyMainDestination.LocationPermission,
            -> R.id.action_global_to_app_permission
            StudyMainDestination.GetCurrentLocation -> R.id.action_global_to_get_current
            StudyMainDestination.Locations -> R.id.action_global_to_location
            is StudyMainDestination.Detail -> R.id.action_global_to_detail
            StudyMainDestination.DataMigration,
            StudyMainDestination.AppUpdate,
            StudyMainDestination.None,
            -> return
        }
        val args = (destination as? StudyMainDestination.Detail)?.let {
            Bundle().apply { putString(ARG_LOCATION_KEY, it.locationKey) }
        }
        runCatching { navController.navigate(actionId, args) }
    }

    private companion object {
        const val ARG_LOCATION_KEY = "location_key"
    }
}

