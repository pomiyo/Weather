package dev.local.weatherstudy.app.detail.fragment.renderer

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.appbar.AppBarLayout
import dev.local.weatherstudy.app.R
import dev.local.weatherstudy.app.detail.adapter.card.StudyDetailAdapter
import dev.local.weatherstudy.app.detail.adapter.card.viewholder.pageDots
import dev.local.weatherstudy.app.detail.adapter.header.StudyDetailHeaderAdapter
import dev.local.weatherstudy.app.detail.view.StudyCollapsibleToolbar
import dev.local.weatherstudy.app.detail.view.StudyDetailSwipeRefresh
import dev.local.weatherstudy.app.detail.viewmodel.StudyDetailViewModel
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailBackgroundState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailScreenState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState
import kotlin.math.abs

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.fragment.renderer.DetailRenderer
 * …DetailContentRenderer, …DetailMainViewSetup
 *
 * Observed responsibility: all of the detail screen's view manipulation, kept out of the
 * Fragment. [setUp] is the one-time wiring (`DetailMainViewSetup`); [render] is the
 * per-state pass (`DetailContentRenderer`).
 *
 * The layout manager's span count comes from `GetColumnSize` through the state, which is
 * what lets a fold change re-lay out without a configuration restart.
 *
 * ### One card list, many header pages
 *
 * The header pager has a page per city; the card list below is ONE list. Selecting a
 * page changes `selectedKey`, and this renderer hands the selected city's cards to the
 * adapter — whose holders re-render only where their state actually differs.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailRenderer(
    private val root: View,
    private val adapter: StudyDetailAdapter,
    private val headerAdapter: StudyDetailHeaderAdapter,
    private val onPageSelected: (Int) -> Unit,
) {
    private val cardList: RecyclerView = root.findViewById(R.id.card_list)
    private val swipeRefresh: StudyDetailSwipeRefresh = root.findViewById(R.id.swipe_refresh)
    private val appBar: AppBarLayout = root.findViewById(R.id.app_bar)
    private val collapsibleToolbar: StudyCollapsibleToolbar = root.findViewById(R.id.collapsible_toolbar)
    private val headerPager: ViewPager2 = root.findViewById(R.id.header_pager)
    private val illustration: LottieAnimationView = root.findViewById(R.id.icon_illust)
    private var shownIllustrationAsset: String = ""
    private val pageIndicator: TextView = root.findViewById(R.id.header_page_indicator)
    private val toolbarContainer: View = root.findViewById(R.id.toolbar_container)
    private val toolbarCity: TextView = root.findViewById(R.id.toolbar_city)
    private val bottomBar: View = root.findViewById(R.id.bottom_floating_area)
    private val updateTime: TextView = root.findViewById(R.id.bottom_update_time)
    private val messageLayout: View = root.findViewById(R.id.detail_message_layout)
    private val progress: View = root.findViewById(R.id.detail_progress)
    private val message: TextView = root.findViewById(R.id.detail_message)
    private val messageAction: Button = root.findViewById(R.id.detail_message_action)

    private var layoutManager: StaggeredGridLayoutManager? = null
    private var shownBackground: StudyDetailBackgroundState? = null
    private var pageCount = 0

    /** set while [render] moves the pager itself, so that move is not reported as a swipe */
    private var isSyncingPager = false

    /** `DetailMainViewSetup` — runs once */
    fun setUp(viewModel: StudyDetailViewModel) {
        layoutManager = StaggeredGridLayoutManager(
            viewModel.contentColumnSize,
            StaggeredGridLayoutManager.VERTICAL,
        ).also { cardList.layoutManager = it }

        cardList.adapter = adapter
        cardList.clipChildren = false
        cardList.clipToPadding = false
        // the hourly strip draws outside its bounds; nothing in the chain may clip
        cardList.setItemViewCacheSize(ITEM_VIEW_CACHE_SIZE)

        headerPager.adapter = headerAdapter
        headerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pageIndicator.text = pageDots(pageCount, position)
                if (!isSyncingPager) onPageSelected.invoke(position)
            }
        })

        setUpCollapse()
        setUpInsets()
        swipeRefresh.setColorSchemeColors(SPINNER_COLOR)
    }

    /**
     * The header collapses down to the toolbar. The toolbar is a child of the block that
     * scrolls away, so it is translated back by the same amount to stay pinned, while the
     * page content fades out and the collapsed title fades in.
     */
    private fun setUpCollapse() {
        collapsibleToolbar.onCollapseProgressChanged = { collapse ->
            // The header page is a MotionLayout, so collapsing is a scene transition rather
            // than a fade of the whole pager. Fading the pager (which the previous version
            // did) would hide the collapsed arrangement too, since both arrangements live
            // inside the same page.
            applyHeaderCollapse(collapse)
            pageIndicator.alpha = 1f - collapse
            // The toolbar city does NOT fade in on collapse: it is visible throughout, and
            // is the one element that does not move between the two header states.
        }
        appBar.addOnOffsetChangedListener(
            StudyDetailAppBarOffsetChangedListener(collapsibleToolbar) { },
        )
        appBar.addOnOffsetChangedListener { _, verticalOffset ->
            toolbarContainer.translationY = -verticalOffset.toFloat()
            // pull-to-refresh belongs to the fully expanded header only
            swipeRefresh.isHeaderExpanded = verticalOffset == 0
        }
    }

    /**
     * Pushes the collapse progress into every laid-out header page.
     *
     * ViewPager2 keeps the neighbouring pages alive off-screen, and a page that scrolls in
     * mid-collapse must already be in the right state - so this sets progress on all of
     * them rather than only the current one. Reaching them through the pager's inner
     * RecyclerView is the supported route; ViewPager2 exposes no page-view accessor.
     */
    private fun applyHeaderCollapse(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)

        // The page fades over the BACK HALF of the collapse, on top of the scene transition.
        //
        // At full collapse the original shows the city and nothing else - see
        // reference-ui/home-scroll-2.png, where the temperature, condition, high/low and
        // feels-like have all gone and only the pinned toolbar remains. The scene alone
        // does not produce that: it restacks the header into its collapsed arrangement but
        // keeps it visible, and because the toolbar is counter-translated to stay pinned
        // while its parent scrolls, the collapsed arrangement ends up drawn across the
        // toolbar and the status bar.
        //
        // Fading the page out over the second half gives the observed behaviour while
        // keeping the restacking visible through the first half, which is where it reads.
        val alpha = ((1f - clamped) / (1f - PAGE_FADE_START)).coerceIn(0f, 1f)

        // The illustration fades on the same curve.
        //
        // It is a sibling of the scrolling content rather than a child of the header, so it
        // does not move when the list scrolls - without this it would stay pinned in the
        // top-right corner and show through the translucent cards as they pass over it.
        // reference-ui/home-scroll-1.png settles the question: once the original is scrolled
        // the figure is completely gone, not dimmed behind the cards, so it is faded rather
        // than merely layered underneath.
        illustration.alpha = alpha

        val inner = headerPager.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until inner.childCount) {
            (inner.getChildAt(i) as? MotionLayout)?.let { page ->
                page.progress = clamped
                page.alpha = alpha
            }
        }
    }

    /** edge to edge: the header runs under the status bar, the list clears the gesture bar */
    private fun setUpInsets() {
        val resources = root.resources
        val expandedHeight = resources.getDimensionPixelSize(R.dimen.study_header_expanded_height)
        val listBottomPadding = resources.getDimensionPixelSize(R.dimen.study_detail_list_bottom_padding)
        val barMargin = resources.getDimensionPixelSize(R.dimen.study_card_margin)
        val toolbarHeight = TypedValue().let { value ->
            root.context.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true)
            TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            toolbarContainer.updatePadding(top = bars.top)
            headerPager.updatePadding(top = bars.top)
            collapsibleToolbar.minimumHeight = toolbarHeight + bars.top
            collapsibleToolbar.updateLayoutParams { height = expandedHeight + bars.top }
            cardList.updatePadding(bottom = listBottomPadding + bars.bottom)
            bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = barMargin + bars.bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    /** `DetailContentRenderer` — runs per state emission */
    fun render(state: StudyDetailState) {
        val spanCount = state.configuration.contentColumnSize.coerceAtLeast(1)
        layoutManager?.takeIf { it.spanCount != spanCount }?.spanCount = spanCount

        swipeRefresh.isRefreshing = state.refresh.isRefreshing

        when (val screen = state.screen) {
            StudyDetailScreenState.Content -> renderContent(state)
            StudyDetailScreenState.Loading -> renderMessage(R.string.study_loading, showProgress = true)
            StudyDetailScreenState.Empty ->
                renderMessage(R.string.study_empty_add_location, actionRes = R.string.study_empty_action_add)
            is StudyDetailScreenState.Error -> {
                renderMessage(R.string.study_refresh_failed)
                screen.throwable.message?.let { message.text = it }
            }
        }
    }

    private fun renderContent(state: StudyDetailState) {
        messageLayout.visibility = View.GONE
        appBar.visibility = View.VISIBLE
        cardList.visibility = View.VISIBLE
        // the update stamp and refresh control live in the last card, where they do not
        // cover content; the floating area stays in the layout, unused
        bottomBar.visibility = View.GONE

        pageCount = state.details.size
        headerAdapter.submitList(state.details) {
            // only once the pages exist can the pager be put on the selected one
            if (headerPager.currentItem != state.selectedIndex) {
                isSyncingPager = true
                headerPager.setCurrentItem(state.selectedIndex, false)
                isSyncingPager = false
            }
            pageIndicator.text = pageDots(pageCount, headerPager.currentItem)
        }

        val selected = state.selectedDetail ?: return
        toolbarCity.text = selected.topInfo.cityName
        updateTime.text = root.context.getString(R.string.study_updated_at, selected.topInfo.updateTimeText)
        renderBackground(selected.background)
        adapter.updateList(selected)
    }

    private fun renderMessage(textRes: Int, showProgress: Boolean = false, actionRes: Int = 0) {
        messageLayout.visibility = View.VISIBLE
        appBar.visibility = if (showProgress) View.INVISIBLE else View.VISIBLE
        cardList.visibility = View.GONE
        bottomBar.visibility = View.GONE
        progress.visibility = if (showProgress) View.VISIBLE else View.GONE
        message.setText(textRes)
        messageAction.visibility = if (actionRes != 0) View.VISIBLE else View.GONE
        if (actionRes != 0) messageAction.setText(actionRes)
        toolbarCity.text = ""
    }

    /**
     * The two artwork layers behind the screen.
     *
     * The original composes four: the painted background, the hero illustration, the
     * MotionLayout header and the translucent card list. This renders the first two; the
     * other two are view hierarchy and are already in place.
     *
     * Layer 1, the background, is one of eleven painted 900x900 images resolved by
     * `StudyBackgroundProvider`. It is NOT a gradient despite the resource name - the
     * two-stop GradientDrawable below is only the fallback for a tree without the local
     * study assets, and it is deliberately kept rather than removed so the project still
     * builds and runs for someone who clones it.
     *
     * Layer 2, the illustration, is a Lottie composition loaded from assets by path.
     * `setAnimation` throws nothing when the asset is missing - it fails asynchronously
     * onto the failure listener - so the view is hidden up front and only shown once the
     * composition has actually loaded.
     */
    private fun renderBackground(background: StudyDetailBackgroundState) {
        if (background == shownBackground) return
        shownBackground = background

        if (background.artworkResId != 0) {
            root.setBackgroundResource(background.artworkResId)
        } else if (background.gradientStartColor != 0) {
            root.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(background.gradientStartColor, background.gradientEndColor),
            )
        }

        renderIllustration(background.illustrationAsset)
    }

    private fun renderIllustration(assetPath: String) {
        if (assetPath.isEmpty()) {
            illustration.visibility = View.GONE
            illustration.cancelAnimation()
            return
        }
        if (assetPath == shownIllustrationAsset) return
        shownIllustrationAsset = assetPath

        illustration.visibility = View.GONE
        illustration.setFailureListener {
            // the local study assets are not present in this tree; the screen is still valid
            illustration.visibility = View.GONE
        }
        illustration.addLottieOnCompositionLoadedListener {
            illustration.visibility = View.VISIBLE
            illustration.playAnimation()
        }
        illustration.setAnimation(assetPath)
    }

    private companion object {
        const val ITEM_VIEW_CACHE_SIZE = 8
        const val TITLE_FADE_START = 0.6f
        /** the collapse fraction at which the header page starts to fade out */
        const val PAGE_FADE_START = 0.5f
        const val SPINNER_COLOR = 0xFF1C313A.toInt()
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.fragment.renderer.DetailAppBarOffsetChangedListener
 *
 * Observed responsibility: translate the AppBar's vertical offset into
 * [StudyCollapsibleToolbar.collapseProgress]. At full collapse the header swaps its
 * Lottie animation for the static WebP — see `StudyDetailImageType`.
 */
class StudyDetailAppBarOffsetChangedListener(
    private val toolbar: StudyCollapsibleToolbar,
    private val onCollapsedChanged: (Boolean) -> Unit,
) : AppBarLayout.OnOffsetChangedListener {

    private var wasCollapsed = false

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val range = appBarLayout.totalScrollRange.takeIf { it > 0 } ?: return
        val progress = (abs(verticalOffset).toFloat() / range).coerceIn(0f, 1f)
        toolbar.collapseProgress = progress

        val collapsed = progress >= COLLAPSE_THRESHOLD
        if (collapsed != wasCollapsed) {
            wasCollapsed = collapsed
            onCollapsedChanged(collapsed)
        }
    }

    private companion object {
        const val COLLAPSE_THRESHOLD = 0.95f
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.fragment.renderer.AppBarAccessibilityDelegate
 *
 * Observed responsibility: the collapsing header is one large custom view, so TalkBack
 * needs an explicit description assembled from the state — which is what
 * `StudyTtsInfoProvider` produces, and why `DetailViewModel` exposes
 * `isTalkBackEnabled`.
 */
class StudyAppBarAccessibilityDelegate(
    private val describe: () -> String,
) : View.AccessibilityDelegate() {

    override fun onInitializeAccessibilityNodeInfo(
        host: View,
        info: android.view.accessibility.AccessibilityNodeInfo,
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        info.contentDescription = describe()
    }
}

/**
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.DetailBottomBarToggler
 *
 * Observed responsibility: hide the bottom floating bar while scrolling, show it at rest.
 */
class StudyDetailBottomBarToggler(
    private val bottomBar: View,
) : RecyclerView.OnScrollListener() {

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        bottomBar.animate()
            .alpha(if (newState == RecyclerView.SCROLL_STATE_IDLE) 1f else 0f)
            .setDuration(FADE_DURATION_MS)
            .start()
    }

    private companion object {
        const val FADE_DURATION_MS = 150L
    }
}
