package dev.local.weatherstudy.data.source.local

import dev.local.weatherstudy.domain.entity.content.StudyLifeStyleSettings
import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import dev.local.weatherstudy.domain.source.local.StudyLifeStyleSettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudySettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWeatherLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWidgetLocalDataSource
import dev.local.weatherstudy.persistence.dao.StudySettingsDao
import dev.local.weatherstudy.persistence.dao.StudyWeatherDao
import dev.local.weatherstudy.persistence.dao.StudyWidgetDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.source.local.WeatherLocalDataSourceImpl
 * com.samsung.android.weather.data.source.local.WeatherSettingsLocalDataSource
 * com.samsung.android.weather.data.source.local.WidgetLocalDataSourceImpl
 * com.samsung.android.weather.data.source.local.LifeStyleSettingsLocalDataSourceImpl
 *
 * Observed responsibility: adapt a `:study-persistence` DAO to the domain's data-source
 * contract. Like the repositories above, these are thin — but not empty: the DAO's
 * three-tier choice is made *here*, by which DAO gets injected. That is the one decision
 * this layer owns.
 *
 * The chain in full, for one read:
 *
 * ```
 * StudyGetWeather (use case, :study-domain)
 *   → StudyWeatherRepo              (interface, union of capabilities)
 *     → StudyWeatherRepoImpl        (:study-data, pure delegation)
 *       → StudyWeatherLocalDataSource (interface)
 *         → StudyWeatherLocalDataSourceImpl   ← this file
 *           → StudyWeatherDao       (interface, :study-persistence)
 *             → StudyWeatherRoomBackedDao     (mapper + delegation)
 *               → StudyWeatherRoomDao         (:study-database, the SQL)
 * ```
 *
 * Seven hops. That is genuinely what the original does, and each hop exists for a
 * reason named in the class that owns it.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyWeatherLocalDataSourceImpl @Inject constructor(
    private val weatherDao: StudyWeatherDao,
) : StudyWeatherLocalDataSource {

    override suspend fun getLocalWeather(key: String): StudyWeather? = weatherDao.getWeather(key)
    override suspend fun getLocalWeathers(): List<StudyWeather> = weatherDao.getWeathers()
    override suspend fun getCount(key: String): Int = weatherDao.getCount(key)
    override suspend fun isExist(key: String): Boolean = weatherDao.isExist(key)
    override fun observeWeathers(): Flow<List<StudyWeather>> = weatherDao.observeWeathers()

    override suspend fun saveWeather(weather: StudyWeather): Long = weatherDao.saveWeather(weather)
    override suspend fun saveWeathers(weathers: List<StudyWeather>): List<Long> =
        weatherDao.saveWeathers(weathers)
    override suspend fun updateWeather(weather: StudyWeather): Int = weatherDao.updateWeather(weather)
    override suspend fun updateWeathers(weathers: List<StudyWeather>): Int =
        weatherDao.updateWeathers(weathers)
    override suspend fun replaceWeathers(weathers: List<StudyWeather>): Int =
        weatherDao.replaceWeathers(weathers)
    override suspend fun deleteWeather(key: String): Int = weatherDao.deleteWeather(key)
    override suspend fun deleteWeathers(weathers: List<StudyWeather>): Int =
        weatherDao.deleteWeathersByKey(weathers.map { it.location.key })
    override suspend fun deleteWeathersByKey(keys: List<String>): Int =
        weatherDao.deleteWeathersByKey(keys)
    override suspend fun deleteAll(): Int = weatherDao.deleteAll()
    override suspend fun updateOrder(keys: List<String>): Int = weatherDao.updateOrder(keys)
    override suspend fun updateLabel(key: String, label: String, labelType: String): Int =
        weatherDao.updateLabel(key, label, labelType)

    override suspend fun addAwayLocationKey(awayModeLocation: StudyAwayModeLocation) =
        weatherDao.addAwayLocation(awayModeLocation)
    override suspend fun getAwayModeLocationByAwayKey(key: String): StudyAwayModeLocation? =
        weatherDao.getAwayLocationByAwayKey(key)
    override suspend fun getAwayModeLocationByHomeKey(key: String): StudyAwayModeLocation? =
        weatherDao.getAwayLocationByHomeKey(key)
    override suspend fun clearAwayModeLocations() = weatherDao.clearAwayLocations()
}

/**
 * Corresponds conceptually to `…data.source.local.WeatherSettingsLocalDataSource`.
 *
 * Note the original's name: it is `WeatherSettingsLocalDataSource`, not
 * `SettingsLocalDataSourceImpl` — the settings source is named for the app, because the
 * settings table also holds app state rather than only user preferences. See the note
 * on `StudySettingEntity`.
 *
 * The accessors not backed by the reconstruction's DAO subset answer with defaults; the
 * full column list is on the entity.
 */
@Singleton
class StudyWeatherSettingsLocalDataSource @Inject constructor(
    private val settingsDao: StudySettingsDao,
) : StudySettingsLocalDataSource {

    override suspend fun getTempScale(): Int = settingsDao.getTempScale()
    override suspend fun getAutoRefresh(): Int = settingsDao.getAutoRefresh()
    override suspend fun getAutoRefreshInterval(): Int = settingsDao.getAutoRefreshInterval()
    override suspend fun getAutoRefreshNextTime(): Long = settingsDao.getAutoRefreshNextTime()
    override suspend fun getFavoriteLocation(): String = settingsDao.getFavoriteLocation()
    override suspend fun getLastEdgeLocation(): String = settingsDao.getLastEdgeLocation()
    override suspend fun getActiveCpType(): String = settingsDao.getActiveCpType()
    override suspend fun getHomeCpType(): String = settingsDao.getHomeCpType()
    override suspend fun needChangeCp(): Boolean =
        settingsDao.getActiveCpType() != settingsDao.getHomeCpType() &&
            settingsDao.getHomeCpType().isNotEmpty()
    override suspend fun showCpChangePopup(): Int = 0
    override suspend fun getShowAlert(): Int = settingsDao.getShowAlert()
    override suspend fun getBadgeInfo(): Int = settingsDao.getBadgeInfo()
    override suspend fun getAppUpdateStatus(): Int = settingsDao.getAppUpdateStatus()
    override suspend fun getDaemonVersion(): String = ""
    override suspend fun whetherMigrationDone(): Int = settingsDao.getMigrationDone()
    override suspend fun getRestoreMode(): Int = settingsDao.getRestoreMode()
    override suspend fun getWidgetCount(): Int = settingsDao.getWidgetCount()
    override suspend fun getNotificationTime(): Long = settingsDao.getNotificationTime()
    override suspend fun getEnterDetailCount(): Int = settingsDao.getEnterDetailCount()
    override suspend fun getEnterDetailCountWithApproximateLocation(): Int = 0
    override suspend fun getSuccessOnLocation(): Int = settingsDao.getSuccessOnLocation()
    override suspend fun isAwayMode(): Boolean = false
    override suspend fun isAwayModeFirstAccess(): Boolean = true
    override suspend fun getRepresentFirstStart(): Boolean = true
    override suspend fun getMostProbableActivity(): Int = 0
    override suspend fun getNewsOptInDone(): Int = 0
    override suspend fun getSmartThingsSettingsState(): Int = 0
    override suspend fun getReservedValue(): Int = settingsDao.getReservedValue()

    override suspend fun setTempScale(scale: Int) = settingsDao.setTempScale(scale)
    override suspend fun setAutoRefresh(value: Int) = settingsDao.setAutoRefresh(value)
    override suspend fun setAutoRefreshInterval(interval: Int) =
        settingsDao.setAutoRefreshInterval(interval)
    override suspend fun setAutoRefreshNextTime(time: Long) =
        settingsDao.setAutoRefreshNextTime(time)
    override suspend fun setFavoriteLocation(key: String) = settingsDao.setFavoriteLocation(key)
    override suspend fun setLastEdgeLocation(key: String) = settingsDao.setLastEdgeLocation(key)
    override suspend fun setActiveCpType(type: String) = settingsDao.setActiveCpType(type)
    override suspend fun setHomeCpType(type: String) = settingsDao.setHomeCpType(type)
    override suspend fun setNeedChangeCp(need: Boolean) = Unit
    override suspend fun setShowCpChangePopup(value: Int) = Unit
    override suspend fun setShowAlert(value: Int) = settingsDao.setShowAlert(value)
    override suspend fun setBadgeInfo(value: Int) = settingsDao.setBadgeInfo(value)
    override suspend fun setAppUpdateStatus(status: Int) = settingsDao.setAppUpdateStatus(status)
    override suspend fun setDaemonVersion(version: String) = Unit
    override suspend fun setMigrationDone(value: Int) = settingsDao.setMigrationDone(value)
    override suspend fun setRestoreMode(mode: Int) = settingsDao.setRestoreMode(mode)
    override suspend fun setWidgetCount(count: Int) = settingsDao.setWidgetCount(count)
    override suspend fun setNotificationTime(time: Long) = settingsDao.setNotificationTime(time)
    override suspend fun countEnterDetail(): Int = settingsDao.countEnterDetail()
    override suspend fun countEnterDetailWithApproximateLocation(): Int = 0
    override suspend fun setSuccessOnLocation(value: Int) = settingsDao.setSuccessOnLocation(value)
    override suspend fun setIsAwayMode(away: Boolean) = Unit
    override suspend fun setAwayModeFirstAccess(first: Boolean) = Unit
    override suspend fun setRepresentFirstStart(first: Boolean) = Unit
    override suspend fun setMostProbableActivity(activity: Int) = Unit
    override suspend fun setNewsOptInDone(value: Int) = Unit
    override suspend fun setSmartThingsSettingsState(state: Int) = Unit
    override suspend fun setReservedValue(value: Int) = settingsDao.setReservedValue(value)

    override fun observeTempScale(): Flow<Int> = settingsDao.observeTempScale()
    override fun observeAutoRefresh(): Flow<Int> = settingsDao.observeAutoRefresh()
    override fun observeAutoRefreshInterval(): Flow<Int> = settingsDao.observeAutoRefreshInterval()
    override fun observeAutoRefreshNextTime(): Flow<Long> =
        kotlinx.coroutines.flow.flowOf(0L)
    override fun observeFavoriteLocation(): Flow<String> = settingsDao.observeFavoriteLocation()
    override fun observeLastEdgeLocation(): Flow<String> = kotlinx.coroutines.flow.flowOf("")
    override fun observeActiveCpType(): Flow<String> = settingsDao.observeActiveCpType()
    override fun observeHomeCpType(): Flow<String> = kotlinx.coroutines.flow.flowOf("")
    override fun observeNeedChangeCp(): Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false)
    override fun observeShowAlert(): Flow<Int> = kotlinx.coroutines.flow.flowOf(1)
    override fun observeBadgeInfo(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeAppUpdateStatus(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeDaemonVersion(): Flow<String> = kotlinx.coroutines.flow.flowOf("")
    override fun observeMigrationDone(): Flow<Int> = settingsDao.observeMigrationDone()
    override fun observeRestoreMode(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeWidgetCount(): Flow<Int> = settingsDao.observeWidgetCount()
    override fun observeNotificationTime(): Flow<Long> = kotlinx.coroutines.flow.flowOf(0L)
    override fun observeEnterDetailCount(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeEnterDetailCountWithApproximateLocation(): Flow<Int> =
        kotlinx.coroutines.flow.flowOf(0)
    override fun observeSuccessOnLocation(): Flow<Int> = settingsDao.observeSuccessOnLocation()
    override fun observeAwayModeFirstAccess(): Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
    override fun observeMostProbableActivity(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeNewsOptInDone(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeSmartThingsSettingsState(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override fun observeReservedValue(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
}

/** Corresponds conceptually to `…data.source.local.WidgetLocalDataSourceImpl`. */
@Singleton
class StudyWidgetLocalDataSourceImpl @Inject constructor(
    private val widgetDao: StudyWidgetDao,
) : StudyWidgetLocalDataSource {

    override suspend fun insert(widgetInfo: StudyWidgetInfo): Long = widgetDao.insert(widgetInfo)
    override suspend fun delete(widgetId: Int): Int = widgetDao.delete(widgetId)
    override suspend fun deleteAll(): Int = widgetDao.deleteAll()
    override suspend fun getCount(): Int = widgetDao.getCount()
    override suspend fun isExist(widgetId: Int): Boolean = widgetDao.isExist(widgetId)
    override suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo? =
        widgetDao.getWidgetInfo(widgetId)
    override suspend fun getWidgetInfoList(): List<StudyWidgetInfo> = widgetDao.getWidgetInfoList()
    override fun observeWidget(widgetId: Int): Flow<StudyWidgetInfo?> =
        widgetDao.observeWidget(widgetId)
    override fun observeWidgets(): Flow<List<StudyWidgetInfo>> = widgetDao.observeWidgets()

    override suspend fun updateKey(widgetId: Int, key: String): Int =
        widgetDao.updateKey(widgetId, key)
    override suspend fun updateBGColor(widgetId: Int, color: Int): Int =
        widgetDao.updateBGColor(widgetId, color)
    override suspend fun updateBGTransparency(widgetId: Int, transparency: Float): Int =
        widgetDao.updateBGTransparency(widgetId, transparency)
    override suspend fun updateDarkMode(widgetId: Int, mode: Int): Int =
        widgetDao.updateDarkMode(widgetId, mode)
    override suspend fun updateShowNews(widgetId: Int, show: Int): Int =
        widgetDao.updateShowNews(widgetId, show)
    override suspend fun updateRestoreMode(widgetId: Int, mode: Int): Int =
        widgetDao.updateRestoreMode(widgetId, mode)
    override suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int =
        widgetDao.updateAddedInDCMLauncher(widgetId, added)
    override suspend fun updateWidgetComponent(widgetId: Int, order: Int, type: Int): Int =
        widgetDao.updateWidgetComponent(widgetId, order, type)
}

/** Corresponds conceptually to `…data.source.local.LifeStyleSettingsLocalDataSourceImpl`. */
@Singleton
class StudyLifeStyleSettingsLocalDataSourceImpl @Inject constructor(
    private val store: dev.local.weatherstudy.persistence.dao.StudyLifeStyleSettingsStore,
) : StudyLifeStyleSettingsLocalDataSource {
    override suspend fun getSettings(): List<StudyLifeStyleSettings> = store.getSettings()
    override suspend fun setSettings(settings: List<StudyLifeStyleSettings>) =
        store.setSettings(settings)
}
