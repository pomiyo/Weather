package dev.local.weatherstudy.app.detail.util

import androidx.recyclerview.widget.DiffUtil
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.util.DetailContentDiffUtilCallback
 *
 * Observed responsibility: diff the **card-type list**, not the card contents. Identity
 * and content comparison are the same test here, because the list holds singleton
 * objects — which is consistent with the view type being their hash. Content changes
 * inside a card are handled by the attach-time render, not by the diff.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailContentDiffUtilCallback(
    private val oldList: List<StudyDetailCardType>,
    private val newList: List<StudyDetailCardType>,
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
