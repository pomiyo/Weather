package dev.local.weatherstudy.domain.repo

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.database.Cursor
import dev.local.weatherstudy.domain.entity.settings.StudyCorpAppInfo
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import dev.local.weatherstudy.domain.source.local.StudyAwayModeLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyLifeStyleSettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyProfileDataSource
import dev.local.weatherstudy.domain.source.local.StudySettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyThemeLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWeatherLocalCommandDataSource
import dev.local.weatherstudy.domain.source.local.StudyWeatherLocalObserveDataSource
import dev.local.weatherstudy.domain.source.local.StudyWeatherLocalQueryDataSource
import dev.local.weatherstudy.domain.source.remote.StudyWeatherRemoteDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the twelve interfaces in
 * com.samsung.android.weather.domain.repo
 *
 * ### The finding this file exists to preserve
 *
 * Most of Samsung Weather's repositories declare **no methods of their own**. They are
 * type unions over the data-source contracts:
 *
 * ```
 * interface WeatherRepo : WeatherRemoteDataSource, WeatherLocalQueryDataSource,
 *                         WeatherLocalCommandDataSource, WeatherLocalObserveDataSource,
 *                         AwayModeLocalDataSource     // ← and that is the whole body
 * interface SettingsRepo          : SettingsLocalDataSource
 * interface LifeStyleSettingsRepo : LifeStyleSettingsLocalDataSource
 * interface ProfileRepo           : ProfileDataSource
 * interface ThemeRepo             : WeatherRemoteDataSource, ThemeLocalDataSource
 * ```
 *
 * So the "repository" in this architecture is not a facade that merges local and
 * remote behind new methods — it is a *name for a capability set*. The merging logic
 * lives in the use cases (`FetchWeather`, `GetWeather`, `RefreshForecast`), not in the
 * repository. Collapsing these into one `WeatherRepository` with hand-written methods
 * would invert the design.
 *
 * Only five repositories declare their own methods: [StudyWidgetRepo], [StudyStatusRepo],
 * [StudyBadgeRepo], [StudyCorpAppRepo] and [StudyWeatherProviderRepo].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyWeatherRepo :
    StudyWeatherRemoteDataSource,
    StudyWeatherLocalQueryDataSource,
    StudyWeatherLocalCommandDataSource,
    StudyWeatherLocalObserveDataSource,
    StudyAwayModeLocalDataSource

/** Corresponds conceptually to `…repo.SettingsRepo` — a pure union. */
interface StudySettingsRepo : StudySettingsLocalDataSource

/** Corresponds conceptually to `…repo.LifeStyleSettingsRepo` — a pure union. */
interface StudyLifeStyleSettingsRepo : StudyLifeStyleSettingsLocalDataSource

/** Corresponds conceptually to `…repo.ProfileRepo` — a pure union. */
interface StudyProfileRepo : StudyProfileDataSource

/**
 * Corresponds conceptually to `…repo.ThemeRepo`.
 *
 * Observed responsibility: map-search themed places, cached locally and refreshed
 * remotely — the only repository that unions a remote source with a local one and
 * still adds nothing.
 */
interface StudyThemeRepo : StudyWeatherRemoteDataSource, StudyThemeLocalDataSource

/**
 * Corresponds conceptually to `…repo.WidgetRepo`.
 *
 * Observed responsibility: one of the five repositories that DO declare methods.
 * Note it is not a straight pass-through of [StudyWidgetLocalDataSource]: the names
 * differ (`updateGoDark` here vs `updateDarkMode` on the source, `observeWidgetInfo`
 * vs `observeWidget`), because the repository is the widget subsystem's vocabulary
 * while the source speaks the table's.
 */
interface StudyWidgetRepo {
    suspend fun addWidgetInfo(widgetInfo: StudyWidgetInfo): Long
    suspend fun deleteWidgetInfo(widgetId: Int): Int
    suspend fun deleteAllWidgetInfo(): Int
    suspend fun getWidgetCount(): Int
    suspend fun isExist(widgetId: Int): Boolean
    suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo?
    suspend fun getWidgetInfoList(): List<StudyWidgetInfo>
    fun observeWidgetInfo(widgetId: Int): Flow<StudyWidgetInfo?>
    fun observeWidgetInfoList(): Flow<List<StudyWidgetInfo>>

    suspend fun updateWeatherKey(widgetId: Int, key: String): Int
    suspend fun updateWidgetBGColor(widgetId: Int, color: Int): Int
    suspend fun updateWidgetBGTransparency(widgetId: Int, transparency: Float): Int
    suspend fun updateGoDark(widgetId: Int, mode: Int): Int
    suspend fun updateShowNews(widgetId: Int, show: Int): Int
    suspend fun updateWidgetRestoreMode(widgetId: Int, mode: Int): Int
    suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int
    suspend fun updateWidgetComponentInfo(widgetId: Int, order: Int, type: Int): Int
}

/**
 * Corresponds conceptually to `…repo.StatusRepo`.
 *
 * Observed responsibility: a tiny three-method store of "is operation <id> running,
 * and if it failed, why". It is what makes a refresh idempotent across surfaces —
 * a widget tick and a pull-to-refresh both check the same status row.
 */
interface StudyStatusRepo {
    fun getStatus(id: String): Flow<Int>
    fun getReason(id: String): Flow<Int>
    suspend fun setStatus(id: String, status: Int, reason: Int)
}

/** Corresponds conceptually to `…repo.BadgeRepo` — the launcher badge count. */
interface StudyBadgeRepo {
    suspend fun hasBadge(type: Int): Boolean
    suspend fun updateBadge(type: Int)
    suspend fun clearBadge(type: Int)
}

/**
 * Corresponds conceptually to `…repo.CorpAppRepo`.
 *
 * Observed responsibility: the allow-list of companion apps permitted to read weather
 * through the exported providers. `getVersion`/`updateVersion` are synchronous and
 * non-suspending in the original — the list is fetched remotely but consulted from a
 * ContentProvider call, which cannot suspend.
 */
interface StudyCorpAppRepo {
    suspend fun addInfo(info: StudyCorpAppInfo)
    suspend fun getInfo(packageName: String): StudyCorpAppInfo?
    fun getInfoList(): Flow<List<StudyCorpAppInfo>>
    suspend fun deleteInfo(packageName: String)
    suspend fun deleteAllInfo()
    fun getVersion(): Int
    fun updateVersion(version: Int)
}

/**
 * Corresponds conceptually to `…repo.WeatherProviderRepo`.
 *
 * Observed responsibility: the ONLY repository that speaks Android's ContentProvider
 * vocabulary — raw `Cursor`, `ContentValues`, `ContentProviderOperation`. It exists so
 * the three exported providers can serve queries without the domain's entity types
 * leaking into a cross-process boundary.
 *
 * Reconstructed with the same signatures, including the blunt ones: a `Cursor` return
 * is the point, not an accident.
 */
interface StudyWeatherProviderRepo {
    fun getAll(): Cursor
    fun getByKey(location: String): Cursor
    fun getHourly(): Cursor
    fun getHourly(location: String): Cursor
    fun getDaily(): Cursor
    fun getDaily(location: String): Cursor
    fun getIndex(): Cursor
    fun getIndex(location: String): Cursor
    fun getSettings(): Cursor

    fun insert(table: String, values: ContentValues): Long
    fun update(table: String, values: ContentValues, selection: String?, selectionArgs: Array<String>?)
    fun delete(table: String, selection: String?, selectionArgs: Array<String>?)
    fun applyBatch(provider: String, operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult>
}

/** Corresponds conceptually to `…repo.UserPolicyConsentRepo`. */
interface StudyUserPolicyConsentRepo :
    dev.local.weatherstudy.domain.source.policy.StudyUserPolicyConsentDataSource

/** Corresponds conceptually to `…repo.GeofenceRepo`. */
interface StudyGeofenceRepo :
    dev.local.weatherstudy.domain.source.location.StudyGeofenceDataSource
