package dev.local.weatherstudy.persistence.dao

import dev.local.weatherstudy.domain.entity.content.StudyLifeStyleSettings
import dev.local.weatherstudy.domain.entity.settings.StudyCorpAppInfo
import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.dao.WeatherDao
 * com.samsung.android.weather.persistence.dao.SettingsDao (+ Query/Command/Observe)
 * com.samsung.android.weather.persistence.dao.WidgetDao
 * com.samsung.android.weather.persistence.CursorDao
 * com.samsung.android.weather.persistence.AbsSettingsDao
 *
 * ### The three-tier DAO family — the structural point of this module
 *
 * These interfaces are NOT Room DAOs. They are the persistence layer's own contracts,
 * and each has three implementations:
 *
 * ```
 *   interface (here)        Room             legacy raw SQLite      in-memory cache
 *   ────────────────        ────             ─────────────────      ───────────────
 *   StudyWeatherDao    ←  StudyWeatherRoomDao  ·  StudyWeatherDbDao  ·  StudyWeatherInMemoryDao
 *   StudySettingsDao   ←  StudySettingsRoomDao ·  StudySettingsDbDao ·  StudySettingsInMemoryDao
 *   StudyWidgetDao     ←  StudyWidgetRoomDao   ·  StudyWidgetDbDao   ·  StudyWidgetInMemoryDao
 *   StudyCursorDao     ←  StudyCursorRoomDao   ·  StudyCursorDbDao
 * ```
 *
 * Why three? The app has shipped since before Room existed, so the `*DbDao` family is
 * the legacy raw-SQLite path kept for data migration; the `*InMemoryDao` family is a
 * process-lifetime cache the widgets and content providers read so a RemoteViews update
 * does not touch disk. One repository serves all three because they share this contract.
 *
 * These interfaces speak **domain types**, not entities — the Room↔domain mapping
 * happens in the implementation (see `StudyWeatherEntityMapper`), which is why this
 * module depends on both `:study-domain` and `:study-database`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyWeatherDao {
    suspend fun getWeather(key: String): StudyWeather?
    suspend fun getWeathers(): List<StudyWeather>
    fun observeWeathers(): Flow<List<StudyWeather>>
    suspend fun getCount(key: String): Int
    suspend fun isExist(key: String): Boolean

    suspend fun saveWeather(weather: StudyWeather): Long
    suspend fun saveWeathers(weathers: List<StudyWeather>): List<Long>
    suspend fun updateWeather(weather: StudyWeather): Int
    suspend fun updateWeathers(weathers: List<StudyWeather>): Int
    suspend fun replaceWeathers(weathers: List<StudyWeather>): Int
    suspend fun deleteWeather(key: String): Int
    suspend fun deleteWeathersByKey(keys: List<String>): Int
    suspend fun deleteAll(): Int
    suspend fun updateOrder(keys: List<String>): Int
    suspend fun updateLabel(key: String, label: String, labelType: String): Int

    suspend fun addAwayLocation(location: StudyAwayModeLocation)
    suspend fun getAwayLocationByAwayKey(key: String): StudyAwayModeLocation?
    suspend fun getAwayLocationByHomeKey(key: String): StudyAwayModeLocation?
    suspend fun clearAwayLocations()
}

/**
 * Corresponds conceptually to `…persistence.dao.SettingsQueryDao`.
 *
 * Observed responsibility: the query third of the settings triad, mirroring
 * `StudySettingsQueryDataSource` in the domain. The split is repeated at this layer
 * because the in-memory implementation can serve queries without the disk one.
 */
interface StudySettingsQueryDao {
    suspend fun getTempScale(): Int
    suspend fun getAutoRefresh(): Int
    suspend fun getAutoRefreshInterval(): Int
    suspend fun getAutoRefreshNextTime(): Long
    suspend fun getFavoriteLocation(): String
    suspend fun getLastEdgeLocation(): String
    suspend fun getActiveCpType(): String
    suspend fun getHomeCpType(): String
    suspend fun getMigrationDone(): Int
    suspend fun getWidgetCount(): Int
    suspend fun getSuccessOnLocation(): Int
    suspend fun getRestoreMode(): Int
    suspend fun getBadgeInfo(): Int
    suspend fun getAppUpdateStatus(): Int
    suspend fun getShowAlert(): Int
    suspend fun getNotificationTime(): Long
    suspend fun getEnterDetailCount(): Int
    suspend fun getReservedValue(): Int
}

/** Corresponds conceptually to `…persistence.dao.SettingsCommandDao`. */
interface StudySettingsCommandDao {
    suspend fun setTempScale(value: Int)
    suspend fun setAutoRefresh(value: Int)
    suspend fun setAutoRefreshInterval(value: Int)
    suspend fun setAutoRefreshNextTime(value: Long)
    suspend fun setFavoriteLocation(value: String)
    suspend fun setLastEdgeLocation(value: String)
    suspend fun setActiveCpType(value: String)
    suspend fun setHomeCpType(value: String)
    suspend fun setMigrationDone(value: Int)
    suspend fun setWidgetCount(value: Int)
    suspend fun setSuccessOnLocation(value: Int)
    suspend fun setRestoreMode(value: Int)
    suspend fun setBadgeInfo(value: Int)
    suspend fun setAppUpdateStatus(value: Int)
    suspend fun setShowAlert(value: Int)
    suspend fun setNotificationTime(value: Long)
    suspend fun countEnterDetail(): Int
    suspend fun setReservedValue(value: Int)
}

/** Corresponds conceptually to `…persistence.dao.SettingsObserveDao`. */
interface StudySettingsObserveDao {
    fun observeTempScale(): Flow<Int>
    fun observeAutoRefresh(): Flow<Int>
    fun observeAutoRefreshInterval(): Flow<Int>
    fun observeFavoriteLocation(): Flow<String>
    fun observeActiveCpType(): Flow<String>
    fun observeMigrationDone(): Flow<Int>
    fun observeWidgetCount(): Flow<Int>
    fun observeSuccessOnLocation(): Flow<Int>
}

/** Corresponds conceptually to `…persistence.dao.SettingsDao` — the union. */
interface StudySettingsDao :
    StudySettingsQueryDao,
    StudySettingsCommandDao,
    StudySettingsObserveDao

/**
 * Corresponds conceptually to `…persistence.AbsSettingsDao`.
 *
 * Observed responsibility: the original has an abstract base shared by the Room, raw-DB
 * and SharedPreferences settings implementations — it holds the "read, default if
 * missing" logic so each backend only supplies raw access.
 */
abstract class StudyAbsSettingsDao : StudySettingsDao {
    protected fun Int?.orDefault(default: Int) = this ?: default
    protected fun Long?.orDefault(default: Long) = this ?: default
    protected fun String?.orDefault(default: String) = this ?: default
}

/** Corresponds conceptually to `…persistence.dao.WidgetDao`. */
interface StudyWidgetDao {
    suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo?
    suspend fun getWidgetInfoList(): List<StudyWidgetInfo>
    fun observeWidget(widgetId: Int): Flow<StudyWidgetInfo?>
    fun observeWidgets(): Flow<List<StudyWidgetInfo>>
    suspend fun getCount(): Int
    suspend fun isExist(widgetId: Int): Boolean
    suspend fun insert(info: StudyWidgetInfo): Long
    suspend fun delete(widgetId: Int): Int
    suspend fun deleteAll(): Int
    suspend fun updateKey(widgetId: Int, key: String): Int
    suspend fun updateBGColor(widgetId: Int, color: Int): Int
    suspend fun updateBGTransparency(widgetId: Int, transparency: Float): Int
    suspend fun updateDarkMode(widgetId: Int, mode: Int): Int
    suspend fun updateShowNews(widgetId: Int, show: Int): Int
    suspend fun updateRestoreMode(widgetId: Int, mode: Int): Int
    suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int
    suspend fun updateWidgetComponent(widgetId: Int, order: Int, type: Int): Int
}

/**
 * Corresponds conceptually to `…persistence.CursorDao`.
 *
 * Observed responsibility: raw cursor access for the exported content providers. The
 * return type is `android.database.Cursor` all the way up because it crosses a process
 * boundary — see [dev.local.weatherstudy.persistence.database.dao.StudyCursorRoomDao].
 */
interface StudyCursorDao {
    fun getAll(): android.database.Cursor
    fun getByKey(key: String): android.database.Cursor
    fun getHourly(): android.database.Cursor
    fun getHourly(key: String): android.database.Cursor
    fun getDaily(): android.database.Cursor
    fun getDaily(key: String): android.database.Cursor
    fun getIndex(): android.database.Cursor
    fun getIndex(key: String): android.database.Cursor
    fun getSettings(): android.database.Cursor
}

/** Corresponds conceptually to `…persistence.database.dao.LifeStyleSettingsDao`'s domain view. */
interface StudyLifeStyleSettingsStore {
    suspend fun getSettings(): List<StudyLifeStyleSettings>
    suspend fun setSettings(settings: List<StudyLifeStyleSettings>)
}

/** Corresponds conceptually to `…persistence.database.dao.CorpAppDao`'s domain view. */
interface StudyCorpAppStore {
    suspend fun get(packageName: String): StudyCorpAppInfo?
    fun observeAll(): Flow<List<StudyCorpAppInfo>>
    suspend fun add(info: StudyCorpAppInfo)
    suspend fun delete(packageName: String)
    suspend fun deleteAll()
}

/**
 * Corresponds conceptually to `…persistence.ProfileDao` / `ProfileSystemDao`.
 *
 * Observed responsibility: two implementations of the same contract — one reads the
 * persisted profile, the other reads it live from the system. The original keeps both
 * because the persisted copy is what survives a firmware upgrade comparison.
 */
interface StudyProfileDao {
    suspend fun getCountryCode(): String
    suspend fun setCountryCode(code: String)
    suspend fun getSalesCode(): String
    suspend fun setSalesCode(code: String)
    suspend fun getOneUiVersion(): Int
    suspend fun setOneUiVersion(version: Int)
    suspend fun getFirstApiLevel(): Int
    suspend fun setFirstApiLevel(level: Int)
}
