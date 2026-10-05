package dev.local.weatherstudy.app.detail.adapter.card

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.local.weatherstudy.app.detail.adapter.card.viewholder.StudyDetailCommonViewHolder
import dev.local.weatherstudy.app.detail.adapter.card.viewholder.StudyDetailViewHolderFactory
import dev.local.weatherstudy.app.detail.util.StudyDetailContentDiffUtilCallback
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.adapter.card.DetailAdapter
 *
 * ### Four things recovered from the decompiled adapter, all preserved
 *
 * 1. **`getItemViewType(position) = cards[position].hashCode()`** — the view type is the
 *    sealed object's identity hash, not an ordinal. `getItemId` returns the same value
 *    as a `Long`.
 * 2. **`onBindViewHolder` does not bind data.** Its entire body sets
 *    `StaggeredGridLayoutManager.LayoutParams.isFullSpan = (contentColumnSize == 1)`.
 * 3. **data binding happens in `onViewAttachedToWindow`**, guarded by a hash comparison
 *    against what the holder last rendered — see `StudyDetailCommonViewHolder`.
 * 4. **`updateList` uses `DiffUtil` over the card-type list**, via
 *    `DetailContentDiffUtilCallback`, and the original reverses the lists before
 *    diffing (`t.q2(t.q2(cards))` in the decompiled output is a double reverse).
 *
 * Together these mean: changing the state re-renders only the attached holders whose
 * state actually changed, and adding or removing a card animates rather than redraws.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailAdapter(
    private val stateProvider: () -> StudyDetailState,
    private val onAction: (StudyDetailCardType) -> Unit,
    /** a tile's own link, which only the Index card has - see StudyIndexInnerViewHolder */
    private val onWebLink: (String) -> Unit = {},
) : RecyclerView.Adapter<StudyDetailCommonViewHolder>() {

    var cards: List<StudyDetailCardType> = emptyList()
        private set

    private val attached = mutableSetOf<StudyDetailCommonViewHolder>()

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = cards.size

    /** the sealed object's identity hash IS the view type — see the class note */
    override fun getItemViewType(position: Int): Int = cards[position].hashCode()

    override fun getItemId(position: Int): Long = cards[position].hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyDetailCommonViewHolder =
        StudyDetailViewHolderFactory(parent, onAction, onWebLink).createViewHolder(viewType)

    /**
     * Sets the full-span flag and nothing else. Data binding is in
     * [onViewAttachedToWindow] — this is the original's split, not an omission.
     */
    override fun onBindViewHolder(holder: StudyDetailCommonViewHolder, position: Int) {
        val params = holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
            ?: return
        params.isFullSpan = stateProvider().configuration.contentColumnSize == 1
        holder.itemView.layoutParams = params
    }

    /** render on attach, gated by what the holder last rendered */
    override fun onViewAttachedToWindow(holder: StudyDetailCommonViewHolder) {
        attached += holder
        renderIfChanged(holder, stateProvider())
    }

    override fun onViewDetachedFromWindow(holder: StudyDetailCommonViewHolder) {
        attached -= holder
    }

    private fun renderIfChanged(holder: StudyDetailCommonViewHolder, state: StudyDetailState) {
        if (holder.lastDataStateHashcode == state.hashCode() &&
            holder.lastDataSelectedLocationKey == state.selectedKey
        ) {
            return
        }
        holder.render(state, state.selectedDetail)
    }

    /** diffed card-list update, as in the original's `updateList(DetailItemState)` */
    fun updateList(detailState: StudyDetailItemState) {
        val newCards = detailState.cardSortedList.toList()
        val diff = DiffUtil.calculateDiff(
            StudyDetailContentDiffUtilCallback(cards, newCards),
        )
        cards = newCards
        diff.dispatchUpdatesTo(this)
        // a card that stays in the list is not rebound by the diff, so the holders already
        // on screen are offered the new state here; the same hash gate keeps this cheap
        val state = stateProvider()
        attached.forEach { renderIfChanged(it, state) }
    }
}
