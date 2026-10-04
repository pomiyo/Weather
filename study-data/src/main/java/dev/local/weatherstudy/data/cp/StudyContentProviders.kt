package dev.local.weatherstudy.data.cp

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import dev.local.weatherstudy.domain.repo.StudyWeatherProviderRepo
import dev.local.weatherstudy.domain.source.backend.StudyAuthorityProvider

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.cp.AbsWeatherContentProvider
 *
 * ### Observed responsibility and why it is abstract
 *
 * Samsung Weather exports **three** content providers (confirmed in the decoded
 * manifest):
 *
 * ```
 * com.sec.android.daemonapp.provider.WeatherContentProvider        ← legacy authority
 * com.sec.android.daemonapp.provider.SystemLevelContentProvider    ← signature|system
 * com.sec.android.daemonapp.provider.DangerousLevelContentProvider ← dangerous
 * ```
 *
 * Three providers over **one** dataset, differing only in the permission level required
 * to reach them. That is the whole design: the same rows are published at three trust
 * tiers, so a system app, a signed companion app and a third-party app each get a
 * different slice without the query code being duplicated. This base class holds the
 * query code; the subclasses declare the trust tier.
 *
 * Two consequences the reconstruction preserves:
 *
 * - **the cursor column names are public API.** External apps SELECT on them, which is
 *   why `StudyDbConstants` is a single source of truth and why the drifted column names
 *   in `TABLE_SETTING_INFO` were never renamed.
 * - **`query()` cannot suspend**, which is why the whole `StudyCursorDao` tier and the
 *   in-memory DAO tier exist. This is the call site that forces them.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
abstract class StudyAbsWeatherContentProvider : ContentProvider() {

    protected abstract val repo: StudyWeatherProviderRepo
    protected abstract val authorityProvider: StudyAuthorityProvider

    /** the trust tier this provider publishes at — see the class note */
    protected abstract val trustLevel: StudyProviderTrustLevel

    private val matcher: UriMatcher by lazy { buildMatcher(authorityProvider.getUriAuth()) }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        MATCH_WEATHER, MATCH_HOURLY, MATCH_DAILY, MATCH_INDEX, MATCH_SETTINGS -> MIME_DIR
        MATCH_WEATHER_BY_KEY, MATCH_HOURLY_BY_KEY, MATCH_DAILY_BY_KEY, MATCH_INDEX_BY_KEY -> MIME_ITEM
        else -> null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (!canRead(uri)) return null
        return when (matcher.match(uri)) {
            MATCH_WEATHER -> repo.getAll()
            MATCH_WEATHER_BY_KEY -> repo.getByKey(uri.lastPathSegment.orEmpty())
            MATCH_HOURLY -> repo.getHourly()
            MATCH_HOURLY_BY_KEY -> repo.getHourly(uri.lastPathSegment.orEmpty())
            MATCH_DAILY -> repo.getDaily()
            MATCH_DAILY_BY_KEY -> repo.getDaily(uri.lastPathSegment.orEmpty())
            MATCH_INDEX -> repo.getIndex()
            MATCH_INDEX_BY_KEY -> repo.getIndex(uri.lastPathSegment.orEmpty())
            MATCH_SETTINGS -> repo.getSettings()
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (!canWrite(uri) || values == null) return null
        val table = tableFor(matcher.match(uri)) ?: return null
        val id = repo.insert(table, values)
        return uri.buildUpon().appendPath(id.toString()).build()
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        if (!canWrite(uri) || values == null) return 0
        val table = tableFor(matcher.match(uri)) ?: return 0
        repo.update(table, values, selection, selectionArgs?.toList()?.toTypedArray())
        return 1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (!canWrite(uri)) return 0
        val table = tableFor(matcher.match(uri)) ?: return 0
        repo.delete(table, selection, selectionArgs?.toList()?.toTypedArray())
        return 1
    }

    override fun applyBatch(
        operations: ArrayList<ContentProviderOperation>,
    ): Array<ContentProviderResult> =
        repo.applyBatch(authorityProvider.getUriAuth(), operations)

    /**
     * Reconstruction of the per-tier read gate. The original relies on the manifest's
     * `android:permission` plus, for the system tier, a signature check — see
     * [StudySignatureCheckContentProvider].
     */
    protected open fun canRead(uri: Uri): Boolean = true

    protected open fun canWrite(uri: Uri): Boolean =
        trustLevel != StudyProviderTrustLevel.DANGEROUS

    private fun tableFor(match: Int): String? = when (match) {
        MATCH_WEATHER, MATCH_WEATHER_BY_KEY -> TABLE_WEATHER
        MATCH_HOURLY, MATCH_HOURLY_BY_KEY -> TABLE_HOURLY
        MATCH_DAILY, MATCH_DAILY_BY_KEY -> TABLE_DAILY
        MATCH_INDEX, MATCH_INDEX_BY_KEY -> TABLE_INDEX
        MATCH_SETTINGS -> TABLE_SETTINGS
        else -> null
    }

    private fun buildMatcher(authority: String) = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(authority, PATH_WEATHER, MATCH_WEATHER)
        addURI(authority, "$PATH_WEATHER/*", MATCH_WEATHER_BY_KEY)
        addURI(authority, PATH_HOURLY, MATCH_HOURLY)
        addURI(authority, "$PATH_HOURLY/*", MATCH_HOURLY_BY_KEY)
        addURI(authority, PATH_DAILY, MATCH_DAILY)
        addURI(authority, "$PATH_DAILY/*", MATCH_DAILY_BY_KEY)
        addURI(authority, PATH_INDEX, MATCH_INDEX)
        addURI(authority, "$PATH_INDEX/*", MATCH_INDEX_BY_KEY)
        addURI(authority, PATH_SETTINGS, MATCH_SETTINGS)
    }

    protected companion object {
        const val PATH_WEATHER = "weather"
        const val PATH_HOURLY = "hourly"
        const val PATH_DAILY = "daily"
        const val PATH_INDEX = "index"
        const val PATH_SETTINGS = "settings"

        const val MATCH_WEATHER = 1
        const val MATCH_WEATHER_BY_KEY = 2
        const val MATCH_HOURLY = 3
        const val MATCH_HOURLY_BY_KEY = 4
        const val MATCH_DAILY = 5
        const val MATCH_DAILY_BY_KEY = 6
        const val MATCH_INDEX = 7
        const val MATCH_INDEX_BY_KEY = 8
        const val MATCH_SETTINGS = 9

        const val TABLE_WEATHER = "TABLE_WEATHER_INFO"
        const val TABLE_HOURLY = "TABLE_HOURLY_INFO"
        const val TABLE_DAILY = "TABLE_DAILY_INFO"
        const val TABLE_INDEX = "TABLE_LIFE_INDEX_INFO"
        const val TABLE_SETTINGS = "TABLE_SETTING_INFO"

        const val MIME_DIR = "vnd.android.cursor.dir/weatherstudy"
        const val MIME_ITEM = "vnd.android.cursor.item/weatherstudy"
    }
}

/**
 * Corresponds conceptually to the manifest's three permission tiers.
 *
 * Observed responsibility: names which provider subclass a caller reached, so the base
 * class can gate writes without knowing the subclass.
 */
enum class StudyProviderTrustLevel {
    /** `WeatherContentProvider` — the legacy authority, read-mostly */
    LEGACY,

    /** `SystemLevelContentProvider` — `signature|system`, full access */
    SYSTEM,

    /** `DangerousLevelContentProvider` — a `dangerous` runtime permission, read-only */
    DANGEROUS,
}

/**
 * Educational reconstruction (stub — role-preserving, per the project's STEP 22/23).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.cp.SignatureCheckContentProvider
 *
 * Original dependency: a **platform signature check**. The system-level provider verifies
 * that the calling package is signed with the same certificate as the platform (or
 * appears in the corporate-app allow-list with a matching certificate — see
 * `StudyCorpAppEntity.certificate`).
 *
 * Why unavailable: the check depends on the app itself being platform-signed, which is
 * the single largest reason the original application is not reconstructable even though
 * its architecture is (see `reports/rebuild-feasibility.md`). It cannot be reproduced on
 * a debug-signed build, and the project's boundaries forbid signature patching.
 *
 * Where used: `SystemLevelContentProvider`'s `canRead`/`canWrite`.
 *
 * Replacement behaviour: **deny by default**, and say why. Returning `true` here would
 * silently publish the dataset to any caller, which is exactly the wrong lesson.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
abstract class StudySignatureCheckContentProvider : StudyAbsWeatherContentProvider() {

    override fun canRead(uri: Uri): Boolean = isCallerTrusted()

    override fun canWrite(uri: Uri): Boolean = isCallerTrusted()

    /**
     * The original compares the caller's signing certificate against the platform's.
     * The reconstruction cannot be platform-signed, so this is always false — a
     * documented denial rather than a silent allow.
     */
    protected open fun isCallerTrusted(): Boolean = false

    /**
     * Where the original's allow-list check happens: a caller may also be trusted by
     * appearing in `TABLE_CORP_APP_INFO` with a matching certificate. Reconstructed as
     * a seam; the certificate comparison is the part that cannot run here.
     */
    protected fun isAllowListed(context: Context, callingPackage: String?): Boolean = false
}
