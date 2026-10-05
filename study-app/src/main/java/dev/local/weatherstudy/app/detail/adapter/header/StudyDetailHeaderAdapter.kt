package dev.local.weatherstudy.app.detail.adapter.header

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.airbnb.lottie.LottieAnimationView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.RecyclerView
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.resource.StudyIconProvider
import dev.local.weatherstudy.ui.common.resource.StudyWeatherIcons

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.adapter.DetailPagerAdapter
 * (+ `DetailTopInfoViewHolder`)
 *
 * Observed responsibility: the collapsing header is a pager with **one page per saved
 * city**. Swiping it is how the user changes location; the card list below is a single
 * list that re-renders for whichever page is selected, rather than one list per page.
 *
 * Each page binds a `StudyDetailTopInfoState` and nothing else — the page knows nothing
 * of cards, which is what lets the header collapse independently of them.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailHeaderAdapter(
    /**
     * `AppUtils.isPhoneModeNLandscapeOrMultiWindow`, read through the state.
     *
     * A lambda rather than a constructor value because it changes without the list
     * changing: rotating the phone leaves every page's weather identical and alters only
     * how the page is drawn. The renderer flips it and rebinds.
     */
    private val isSmallImageArea: () -> Boolean = { false },
) : ListAdapter<StudyDetailItemState, StudyDetailHeaderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudyDetailHeaderViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.study_detail_header_page, parent, false),
            isSmallImageArea,
        )

    override fun onBindViewHolder(holder: StudyDetailHeaderViewHolder, position: Int) =
        holder.bind(getItem(position))

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<StudyDetailItemState>() {
            override fun areItemsTheSame(a: StudyDetailItemState, b: StudyDetailItemState) = a.key == b.key
            override fun areContentsTheSame(a: StudyDetailItemState, b: StudyDetailItemState) =
                a.topInfo == b.topInfo
        }
    }
}

/** Corresponds conceptually to `…detail.adapter.DetailTopInfoViewHolder`. */
class StudyDetailHeaderViewHolder(
    itemView: View,
    private val isSmallImageArea: () -> Boolean = { false },
) : RecyclerView.ViewHolder(itemView) {
    // city and pin are the toolbar's, not this page's - see study_detail_header_page.xml
    private val icon: LottieAnimationView = itemView.findViewById(R.id.header_icon)
    private val temperature: TextView = itemView.findViewById(R.id.header_temperature)
    private val condition: TextView = itemView.findViewById(R.id.header_condition)
    private val highLow: TextView = itemView.findViewById(R.id.header_high_low)
    private val feelsLike: TextView = itemView.findViewById(R.id.header_feels_like)

    // The collapsed arrangement uses its own views rather than re-constraining the ones
    // above - see study_scene_detail_header.xml. Both sets therefore have to be bound with
    // the same text, because either may be the visible one depending on scroll position.
    private val highLowCollapse: TextView = itemView.findViewById(R.id.header_high_low_collapse)
    private val feelsLikeCollapse: TextView = itemView.findViewById(R.id.header_feels_like_collapse)

    /**
     * The header page is a MotionLayout; [progress] 0 is expanded, 1 is collapsed.
     *
     * Driving it directly is what lets the whole header be a continuous function of scroll
     * offset rather than a flip at a threshold.
     */
    private val motion: MotionLayout? = itemView as? MotionLayout

    fun setCollapseProgress(progress: Float) {
        // In a small image area the header does not collapse at all - it IS the collapsed
        // arrangement, at a fixed detail_top_info_small_collapse_height - so the scroll
        // offset has nothing to drive. See StudyDetailRenderer.applyWindowGeometry.
        motion?.progress = if (isSmallImageArea()) 1f else progress.coerceIn(0f, 1f)
    }

    /**
     * A page in a small image area is the END ConstraintSet, permanently.
     *
     * `PagerViewHolder.updateAppBarLayoutAndScrollFlag` pins the app bar to
     * `detail_top_info_small_collapse_height` and shows `weather_expand_icon` whenever the
     * state is `AnimationIconOnly`, and `renderAppBar` swaps the temperature to
     * `SecNum_400_White_50dp` in the same breath. The arrangement that leaves - 70dp icon
     * at the end, high/low above feels-like beside a smaller temperature, no condition
     * line - is exactly this scene's collapsed set, which is why there is no third layout
     * for landscape anywhere in the APK.
     */
    fun bind(item: StudyDetailItemState) {
        val top = item.topInfo
        val smallImageArea = isSmallImageArea()
        temperature.setTextAppearance(
            if (smallImageArea) {
                R.style.Study_TextAppearance_Detail_SecNum_400_White_50dp
            } else {
                R.style.Study_TextAppearance_Detail_SecNum_400_White_70dp
            },
        )
        motion?.progress = if (smallImageArea) 1f else motion?.progress ?: 0f
        // the hero illustration is not drawn in a small image area, so the icon is
        bindIcon(top.iconNum, if (smallImageArea) "" else item.background.illustrationAsset)
        temperature.text = top.temperature
        condition.text = top.weatherText
        highLow.text = top.highLow
        highLowCollapse.text = top.highLow
        val feelsLikeText = itemView.context.getString(R.string.study_feels_like, top.feelsLike)
        feelsLike.text = feelsLikeText
        feelsLikeCollapse.text = feelsLikeText
    }

    /**
     * The header icon is animated in the original and static here only as a fallback.
     *
     * Lottie reports a missing asset asynchronously through the failure listener rather
     * than throwing, so the static vector is set FIRST and the animation allowed to replace
     * it once its composition has loaded. That ordering is what makes the no-assets case
     * render the old icon instead of an empty 70dp hole.
     */
    private fun bindIcon(iconNum: Int, illustrationAsset: String) {
        val context = itemView.context

        // The icon and the illustration are ALTERNATIVES, not layers.
        //
        // Cropping reference-ui/home.png settles this: the expanded header's top-right
        // carries the illustration only - sun, clouds, lens flare, figure - and no separate
        // 70dp condition icon anywhere on top of it. Drawing both put a rain animation
        // across the figure's umbrella.
        //
        // The original's evidence for the same thing is in the MotionScene: both icon views
        // carry motion:visibilityMode="ignore", which exists precisely so the scene does NOT
        // manage their visibility and code can switch them against the illustration.
        if (illustrationAsset.isNotEmpty()) {
            icon.cancelAnimation()
            icon.visibility = View.GONE
            return
        }

        icon.visibility = View.VISIBLE
        icon.setFailureListener {
            icon.cancelAnimation()
            icon.setImageResource(StudyIconProvider.getWhiteResource(context, iconNum))
        }
        icon.setImageResource(StudyIconProvider.getWhiteResource(context, iconNum))
        icon.setAnimation(StudyIconProvider.getAnimationAsset(context, iconNum))
        icon.addLottieOnCompositionLoadedListener { icon.playAnimation() }
    }
}
