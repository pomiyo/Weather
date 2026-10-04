package dev.local.weatherstudy.logger.diag

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction (stub — role-preserving, per the project's STEP 22/23).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.logger.diag.UserMonitorDataSource / UserMonitorDataSourceImpl
 * com.samsung.android.weather.logger.diag.model.UserActivity
 * and the 14 classes in `com.samsung.android.weather.logger.diag`
 *
 * Original dependency: Samsung **DiagMon** (diagnostic monitoring). The APK bundles a
 * 4.7 KB native library, `libDiagMonKey.so`, whose purpose the project's reports mark
 * as UNKNOWN (name-inferred only) — the most likely role is signing or keying the
 * diagnostic upload.
 *
 * Why unavailable: DiagMon uploads to Samsung-operated endpoints and requires a
 * registered service id. There is no public equivalent.
 *
 * Where used: `UserMonitorDataSource` records a timeline of user activity
 * (`UserActivity` is a data class, not an Activity) that the original uploads
 * periodically.
 *
 * Replacement behaviour: records to logcat and keeps nothing. Nothing is uploaded, and
 * the native library is not loaded.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyUserMonitorDataSource {
    fun record(activity: StudyUserActivity)
    fun flush()
}

/**
 * Corresponds conceptually to `…logger.diag.model.UserActivity`.
 *
 * Note: a `data class`, despite the name — not an Android `Activity`. It was one of the
 * two classes whose name made it look like an Activity during the inventory sweep.
 */
data class StudyUserActivity(
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detail: String = "",
)

/** The reconstruction's logcat-only implementation. */
@Singleton
class StudyLogcatUserMonitor @Inject constructor() : StudyUserMonitorDataSource {
    override fun record(activity: StudyUserActivity) {
        Log.d(TAG, "activity=${activity.name} detail=${activity.detail}")
    }

    /** the original would upload here; the reconstruction has nothing to send */
    override fun flush() = Unit

    private companion object {
        const val TAG = "StudyUserMonitor"
    }
}
