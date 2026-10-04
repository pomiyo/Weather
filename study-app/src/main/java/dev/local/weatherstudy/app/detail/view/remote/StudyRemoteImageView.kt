package dev.local.weatherstudy.app.detail.view.remote

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.detail.view.remote.CoilImageView
 * com.sec.android.daemonapp.app.detail.view.remote.RemoteImageView / IRemoteImageView
 *
 * The original has an interface plus two implementations because the same image loading
 * has to work in a normal View hierarchy **and** in RemoteViews (widgets), where Coil
 * cannot attach to a view — so the widget path loads to a Bitmap instead. That is also
 * why `NewsBitmapImageWorker` exists.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface IStudyRemoteImageView {
    fun loadUrl(url: String, placeholderRes: Int = 0)
    fun clear()
}

class StudyRemoteImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), IStudyRemoteImageView {

    private var currentUrl: String? = null

    /**
     * The original uses Coil here. The reconstruction keeps the seam and does not fetch:
     * no remote image is loaded, because the reconstruction has no real content feed.
     */
    override fun loadUrl(url: String, placeholderRes: Int) {
        currentUrl = url
        if (placeholderRes != 0) setImageResource(placeholderRes) else setImageDrawable(null)
    }

    override fun clear() {
        currentUrl = null
        setImageDrawable(null)
    }
}
