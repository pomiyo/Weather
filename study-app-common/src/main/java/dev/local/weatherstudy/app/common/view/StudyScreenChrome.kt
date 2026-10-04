package dev.local.weatherstudy.app.common.view

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import dev.local.weatherstudy.app.common.R

/**
 * Educational reconstruction — reconstruction-only helpers.
 *
 * Corresponds conceptually to: what the original gets for free from the SESL
 * `Toolbar` / `AppBarLayout` fork and `SeslRoundedCorner`. Those artifacts are
 * unpublished, so the plain screens draw a title row of their own and handle the system
 * bars themselves.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyScreenToolbar(root: View) {
    val back: ImageButton = root.findViewById(R.id.screen_back)
    val title: TextView = root.findViewById(R.id.screen_title)
    private val primary: ImageButton = root.findViewById(R.id.screen_action_primary)
    private val secondary: ImageButton = root.findViewById(R.id.screen_action_secondary)

    fun setTitle(titleRes: Int) = title.setText(titleRes)

    fun setPrimaryAction(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) =
        primary.bindAction(iconRes, descriptionRes, onClick)

    fun setSecondaryAction(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) =
        secondary.bindAction(iconRes, descriptionRes, onClick)

    fun clearPrimaryAction() { primary.visibility = View.GONE }

    fun clearSecondaryAction() { secondary.visibility = View.GONE }

    private fun ImageButton.bindAction(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) {
        setImageResource(iconRes)
        contentDescription = context.getString(descriptionRes)
        visibility = View.VISIBLE
        setOnClickListener { onClick() }
    }
}

/** Edge to edge: pad a screen's root so its content clears the status and gesture bars. */
fun View.applySystemBarPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime(),
        )
        view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Back from a screen. A screen reached by a global action has nothing beneath it — the
 * graph was popped on the way in — so "back" re-runs the start destination instead of
 * leaving an empty task.
 */
fun NavController.navigateBackOrRestart() {
    if (previousBackStackEntry != null) popBackStack() else restartGraph()
}

/** Re-enter the graph at its start destination, which re-runs the startup condition chain. */
fun NavController.restartGraph() {
    navigate(
        graph.startDestinationId,
        null,
        NavOptions.Builder().setPopUpTo(graph.id, true).build(),
    )
}
