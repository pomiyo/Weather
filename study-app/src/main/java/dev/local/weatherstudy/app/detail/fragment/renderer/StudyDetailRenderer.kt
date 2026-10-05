package dev.local.weatherstudy.app.detail.fragment.renderer

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
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
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailConfiguration
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState
import dev.local.weatherstudy.ui.common.resource.StudyDensityUnitConverter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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
    private val toolbar: View = root.findViewById(R.id.toolbar)
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

    /**
     * The window geometry, kept because it arrives from two directions.
     *
     * The configuration comes through the state; the system bar insets come through
     * [ViewCompat.setOnApplyWindowInsetsListener], and either can land first. Both are
     * stored and [applyWindowGeometry] runs from either, so the header height and the
     * content inset are never half applied.
     */
    private var configuration = StudyDetailConfiguration()
    private var insetTop = 0
    private var insetLeft = 0
    private var insetRight = 0
    private var insetBottom = 0

    /** which of the two item decorations is currently installed; null until the first */
    private var installedLargeScreenGap: Boolean? = null

    /** set while [render] moves the pager itself, so that move is not reported as a swipe */
    private var isSyncingPager = false

    /** `DetailMainViewSetup` — runs once */
    fun setUp(viewModel: StudyDetailViewModel) {
        layoutManager = StaggeredGridLayoutManager(
            viewModel.contentColumnSize,
            StaggeredGridLayoutManager.VERTICAL,
        ).also { cardList.layoutManager = it }

        cardList.adapter = adapter
        // detail_gap_between_cards (10dp) between every card.
        //
        // There was no decoration here at all, so the top-level cards sat flush against one
        // another: the daily card's bottom corner met the UV tile's top corner, the
        // precipitation tile met the sun card, and the moon card met the footer. Rounded
        // corners touching with no gap read as cards overlapping, which is exactly what it
        // looked like. The nested lists inside the Index and daily cards already had their
        // gaps; the outer list never did.
        installCardGap()
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
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            // Vertically the header runs under the status bar and the cutout; horizontally
            // only the system bars count. The original insets the content by the landscape
            // navigation bar on the right - reference-ui/home-landscape.png has the card
            // column stopping 48dp short of the window edge there - but NOT by the display
            // cutout on the left, where the city name sits at its usual margin with the
            // punch hole beside it.
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insetTop = bars.top
            insetLeft = systemBars.left
            insetRight = systemBars.right
            insetBottom = systemBars.bottom
            applyWindowGeometry()
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    /**
     * `PagerViewHolder.updateContentHorizontalPadding` + `renderToolBar` +
     * `updateAppBarLayoutAndScrollFlag` — everything about the screen that depends on the
     * size of the window rather than on the weather.
     *
     * ### The content column
     *
     * ```java
     * int pad = round((dpToPx(config.screenWidthDp) - viewModel.getContentWidthPx()) * 0.5f);
     * iconIllustContainer.setMargins(0, 0, pad, 0);
     * appBar.setMargins(pad, 0, pad, 0);
     * cardView.setPadding(isLargeScreen ? pad - gap / 2 : pad, 0, …, 0);
     * ```
     *
     * One number, applied in three places. In portrait `GetContentAreaWidth` returns the
     * screen width less twice `detail_content_portrait_padding`, so `pad` is 5dp and
     * nothing looks inset. In landscape it returns 62% of the width and `pad` becomes
     * ~158dp a side on this device: the same code produces the centred column, with no
     * landscape layout and no `-land` dimension anywhere.
     *
     * The illustration gets the padding on its RIGHT ONLY, because it is anchored
     * top|end — pushing it in from the left would move it off its anchor.
     *
     * The `- gap / 2` on a large screen is not a correction: with two columns the card
     * gap decoration adds half a gutter to each outer edge too, and this takes it back so
     * the pair still measures exactly the content width.
     *
     * ### The header
     *
     * `isPhoneLandscape` picks `detail_top_info_land_height` (56dp) over
     * `detail_top_info_height` (64dp) for the toolbar, and `isSmallImageArea` pins the
     * block to `detail_top_info_small_collapse_height` (84dp) with the scroll flags
     * cleared, which is how the original ends up with a header that does not collapse
     * because there is nothing left to collapse.
     */
    private fun applyWindowGeometry() {
        installCardGap()
        val resources = root.resources
        val listBottomPadding = resources.getDimensionPixelSize(R.dimen.study_detail_list_bottom_padding)
        val barMargin = resources.getDimensionPixelSize(R.dimen.study_card_margin)

        val toolbarHeight = resources.getDimensionPixelSize(
            if (configuration.isPhoneLandscape) {
                R.dimen.study_detail_top_info_land_height
            } else {
                R.dimen.study_detail_top_info_height
            },
        )
        val topInfoHeight = resources.getDimensionPixelSize(
            if (configuration.isSmallImageArea) {
                R.dimen.study_detail_top_info_small_collapse_height
            } else {
                R.dimen.study_detail_top_info_expand_height
            },
        )

        // A phone in landscape ignores the top inset.
        //
        // `SystemUIKt.setNormalSystemUi` installs an inset listener whose top padding is
        // `isPhoneAndLandScape ? 0 : insets.top`, and the detail screen behaves the same
        // way: reference-ui/home-landscape.png puts the first card at 150.4dp, which is
        // detail_top_info_land_height (59.7) + detail_top_info_small_collapse_height
        // (89.6) exactly, with nothing left over for a status bar.
        val topInset = if (configuration.isPhoneLandscape) 0 else insetTop

        toolbar.updateLayoutParams { height = toolbarHeight }
        // The toolbar is NOT inset with the rest of the header.
        //
        // In the original it is a sibling of the CoordinatorLayout, so the content
        // padding never reaches it and the city name stays at the window's own left edge
        // - reference-ui/home-landscape.png shows "Lahug" there while the temperature
        // beneath it starts 158dp in. Here the toolbar is drawn inside the collapsing
        // block, so the padding has to go on the PAGER instead of on the app bar, or the
        // city slides in with the header and lands on top of the condition line.
        toolbarContainer.updatePadding(top = topInset, left = insetLeft, right = insetRight)
        collapsibleToolbar.minimumHeight = toolbarHeight + topInset
        collapsibleToolbar.updateLayoutParams {
            height = toolbarHeight + topInfoHeight + topInset
        }
        collapsibleToolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags = if (configuration.isSmallImageArea) {
                0
            } else {
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            }
        }

        val sidePadding = contentSidePadding()
        val gap = resources.getDimensionPixelSize(R.dimen.study_detail_gap_between_cards)
        val listPadding =
            if (configuration.isLargeScreen) sidePadding - gap / 2 else sidePadding

        headerPager.updatePadding(
            top = topInset,
            left = sidePadding + insetLeft,
            right = sidePadding + insetRight,
        )
        illustration.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(right = sidePadding + insetRight)
        }
        cardList.updatePadding(
            left = max(0, listPadding) + insetLeft,
            right = max(0, listPadding) + insetRight,
            bottom = listBottomPadding + insetBottom,
        )
        bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = barMargin + insetBottom
        }
        if (configuration.isSmallImageArea) {
            illustration.visibility = View.GONE
            illustration.cancelAnimation()
        }
    }

    /**
     * `PagerViewHolder.initRecyclerViewItemDecoration` - one decoration or the other,
     * never both. Swapped when the window crosses into or out of a large screen, which
     * `setUp` cannot decide on its own because the first configuration has not arrived yet.
     */
    private fun installCardGap() {
        val wantsLargeScreen = configuration.isLargeScreen
        if (installedLargeScreenGap == wantsLargeScreen) return
        installedLargeScreenGap = wantsLargeScreen
        while (cardList.itemDecorationCount > 0) cardList.removeItemDecorationAt(0)
        cardList.addItemDecoration(
            if (wantsLargeScreen) {
                StudyLargeScreenCardGap(
                    cardList.resources
                        .getDimensionPixelSize(R.dimen.study_detail_large_view_holder_gap),
                )
            } else {
                StudyCardGap(cardList.resources)
            },
        )
    }

    /** `round((screenWidthPx - contentWidthPx) * 0.5f)`, with the pre-measure guard */
    private fun contentSidePadding(): Int {
        if (configuration.contentWidthPx <= 0) {
            // before the first configuration arrives, the portrait inset is the honest
            // default: it is what GetContentAreaWidth returns for a Normal screen
            return root.resources
                .getDimensionPixelSize(R.dimen.study_detail_content_portrait_padding)
        }
        val screenWidthPx = StudyDensityUnitConverter.dpToPx(
            configuration.screenWidthDp.toFloat(),
            root.context,
        )
        return max(0, ((screenWidthPx - configuration.contentWidthPx) * 0.5f).toInt())
    }

    /** `DetailContentRenderer` — runs per state emission */
    fun render(state: StudyDetailState) {
        if (state.configuration != configuration) {
            val wasSmallImageArea = configuration.isSmallImageArea
            configuration = state.configuration
            applyWindowGeometry()
            // the pages carry the illustration/icon choice and the temperature size, so a
            // change of image area has to rebind them even though their weather is identical
            if (wasSmallImageArea != configuration.isSmallImageArea) {
                headerAdapter.notifyItemRangeChanged(0, headerAdapter.itemCount)
            }
        }

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
        if (assetPath.isEmpty() || configuration.isSmallImageArea) {
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

/**
 * Corresponds conceptually to `…detail.util.DetailItemDecoration`.
 *
 * The gap between top-level detail cards - and, it turns out, the cards' side margins too:
 *
 * ```java
 * int gap = resources.getDimensionPixelSize(R.dimen.detail_gap_between_cards);
 * if (!isLast || !isFullSpan) outRect.bottom = gap;
 * int half = round(gap * 0.5f);
 * outRect.left = half;
 * outRect.right = half;
 * ```
 *
 * Session 4 correction. This set `bottom` only, and the horizontal half-gap just on the
 * inner edge of a two-column pair. The original puts HALF A GAP ON BOTH SIDES OF EVERY
 * CARD, unconditionally, which is where the card's side margin comes from - none of the
 * `detail_*_view_holder` layouts declare one, and `study_card_margin` was a measured
 * stand-in for it. With `detail_content_portrait_padding` (5.33dp) on the list and this
 * half gap (5.33dp) on the card, a card edge lands at 10.67dp, which is exactly where
 * reference-ui/home.png puts it: x=30px of 1080 at 450dpi.
 *
 * The bottom gap is skipped for a full-span LAST card so the list does not end with a
 * trailing space under the attribution.
 */
private class StudyCardGap(resources: android.content.res.Resources) :
    RecyclerView.ItemDecoration() {

    private val gap = resources.getDimensionPixelSize(R.dimen.study_detail_gap_between_cards)

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val isLast = parent.getChildAdapterPosition(view) ==
            (parent.adapter?.itemCount ?: 0) - 1
        val isFullSpan =
            (view.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan == true
        if (!isLast || !isFullSpan) outRect.bottom = gap

        val half = (gap * 0.5f).roundToInt()
        outRect.left = half
        outRect.right = half
    }
}

/**
 * Corresponds conceptually to `…detail.util.SpaceLargeScreenItemDecoration`.
 *
 * The large-screen alternative, installed instead of [StudyCardGap] when the window is
 * Large or Huge - `initRecyclerViewItemDecoration` picks between the two on
 * `state.isLargeScreen` and installs exactly one. It differs in both directions: the gap
 * goes on TOP rather than the bottom (so the first row clears the header rather than the
 * last row clearing the footer), it is `detail_large_view_holder_gap` rather than
 * `detail_gap_between_cards`, and it is applied to every card with no last-item case.
 */
private class StudyLargeScreenCardGap(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.top = space
        outRect.left = space / 2
        outRect.right = space / 2
    }
}
