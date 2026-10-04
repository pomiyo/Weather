package dev.local.weatherstudy.persistence.dao

import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetComponent
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import dev.local.weatherstudy.persistence.database.dao.StudyAwayModeLocationsDao
import dev.local.weatherstudy.persistence.database.dao.StudySettingsRoomDao
import dev.local.weatherstudy.persistence.database.dao.StudyWeatherRoomDao
import dev.local.weatherstudy.persistence.database.dao.StudyWidgetRoomDao
import dev.local.weatherstudy.persistence.database.models.StudyAwayModeLocationsEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetComponentEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetEntity
import dev.local.weatherstudy.persistence.mapper.StudyWeatherEntityMapper
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the Room-backed tier of the DAO family — the adapters
 * between `…persistence.dao.*Dao` (domain types) and
 * `…persistence.database.dao.*RoomDao` (entities).
 *
 * Observed responsibility: nothing but mapping and delegation. All the SQL is in
 * `:study-database`; all the domain shape is in `:study-domain`; this is the seam.
 * Keeping it thin is what lets the same contract be served by the legacy raw-SQLite
 * tier and the in-memory tier without either knowing about Room.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyWeatherRoomBackedDao @Inject constructor(
    private val roomDao: StudyWeatherRoomDao,
    private val awayDao: StudyAwayModeLocationsDao,
    private val mapper: StudyWeatherEntityMapper,
) : StudyWeatherDao {

    override suspend fun getWeather(key: String): StudyWeather? =
        roomDao.getWeather(key)?.let(mapper::toDomain)

    override suspend fun getWeathers(): List<StudyWeather> =
        roomDao.getWeathers().map(mapper::toDomain)

    override fun observeWeathers(): Flow<List<StudyWeather>> =
        roomDao.observeWeathers().map { rows -> rows.map(mapper::toDomain) }

    override suspend fun getCount(key: String): Int = roomDao.getCount(key)

    override suspend fun isExist(key: String): Boolean = roomDao.isExist(key)

    override suspend fun saveWeather(weather: StudyWeather): Long =
        roomDao.saveAggregate(mapper.toEntity(weather))

    override suspend fun saveWeathers(weathers: List<StudyWeather>): List<Long> =
        roomDao.saveAggregates(weathers.map(mapper::toEntity))

    override suspend fun updateWeather(weather: StudyWeather): Int =
        roomDao.saveAggregate(mapper.toEntity(weather)).let { 1 }

    override suspend fun updateWeathers(weathers: List<StudyWeather>): Int =
        roomDao.saveAggregates(weathers.map(mapper::toEntity)).size

    override suspend fun replaceWeathers(weathers: List<StudyWeather>): Int =
        roomDao.replaceAggregates(weathers.map(mapper::toEntity))

    override suspend fun deleteWeather(key: String): Int = roomDao.deleteWeatherByKey(key)

    override suspend fun deleteWeathersByKey(keys: List<String>): Int =
        roomDao.deleteWeathersByKey(keys)

    override suspend fun deleteAll(): Int = roomDao.deleteAll()

    override suspend fun updateOrder(keys: List<String>): Int = roomDao.reorder(keys)

    override suspend fun updateLabel(key: String, label: String, labelType: String): Int =
        roomDao.updateLabel(key, label, labelType)

    override suspend fun addAwayLocation(location: StudyAwayModeLocation) =
        awayDao.insert(
            StudyAwayModeLocationsEntity(
                awayKey = location.awayLocation,
                homeKey = location.homeLocation,
            ),
        )

    override suspend fun getAwayLocationByAwayKey(key: String): StudyAwayModeLocation? =
        awayDao.getByAwayKey(key)?.toDomain()

    override suspend fun getAwayLocationByHomeKey(key: String): StudyAwayModeLocation? =
        awayDao.getByHomeKey(key)?.toDomain()

    override suspend fun clearAwayLocations() = awayDao.clear()

    private fun StudyAwayModeLocationsEntity.toDomain() = StudyAwayModeLocation(
        awayLocation = awayKey,
        homeLocation = homeKey,
        isActive = true,
    )
}

/**
 * Corresponds conceptually to `…persistence.database.dao.SettingsRoomDao`'s adapter.
 *
 * Observed responsibility: applies the defaults. The Room DAO returns nullable column
 * reads (the single row may not exist yet); [StudyAbsSettingsDao] is where the original
 * puts the "or default" logic, shared with the prefs and legacy tiers.
 */
class StudySettingsRoomBackedDao @Inject constructor(
    private val roomDao: StudySettingsRoomDao,
) : StudyAbsSettingsDao() {

    override suspend fun getTempScale(): Int = roomDao.getTempScale().orDefault(0)
    override suspend fun getAutoRefresh(): Int = roomDao.getAutoRefresh().orDefault(0)
    override suspend fun getAutoRefreshInterval(): Int = roomDao.getAutoRefreshInterval().orDefault(3)
    override suspend fun getAutoRefreshNextTime(): Long = roomDao.getAutoRefreshNextTime().orDefault(0L)
    override suspend fun getFavoriteLocation(): String = roomDao.getFavoriteLocation().orDefault("")
    override suspend fun getLastEdgeLocation(): String = ""
    override suspend fun getActiveCpType(): String = roomDao.getActiveCpType().orDefault("")
    override suspend fun getHomeCpType(): String = roomDao.getHomeCpType().orDefault("")
    override suspend fun getMigrationDone(): Int = roomDao.getMigrationDone().orDefault(0)
    override suspend fun getWidgetCount(): Int = roomDao.getWidgetCount().orDefault(0)
    override suspend fun getSuccessOnLocation(): Int = roomDao.getSuccessOnLocation().orDefault(0)
    override suspend fun getRestoreMode(): Int = 0
    override suspend fun getBadgeInfo(): Int = 0
    override suspend fun getAppUpdateStatus(): Int = 0
    override suspend fun getShowAlert(): Int = 1
    override suspend fun getNotificationTime(): Long = 0L
    override suspend fun getEnterDetailCount(): Int = 0
    override suspend fun getReservedValue(): Int = 0

    override suspend fun setTempScale(value: Int) = withRow { roomDao.setTempScale(value) }
    override suspend fun setAutoRefresh(value: Int) = withRow { roomDao.setAutoRefresh(value) }
    override suspend fun setAutoRefreshInterval(value: Int) = withRow { roomDao.setAutoRefreshInterval(value) }
    override suspend fun setAutoRefreshNextTime(value: Long) = withRow { roomDao.setAutoRefreshNextTime(value) }
    override suspend fun setFavoriteLocation(value: String) = withRow { roomDao.setFavoriteLocation(value) }
    override suspend fun setLastEdgeLocation(value: String) = Unit
    override suspend fun setActiveCpType(value: String) = withRow { roomDao.setActiveCpType(value) }
    override suspend fun setHomeCpType(value: String) = withRow { roomDao.setHomeCpType(value) }
    override suspend fun setMigrationDone(value: Int) = withRow { roomDao.setMigrationDone(value) }
    override suspend fun setWidgetCount(value: Int) = withRow { roomDao.setWidgetCount(value) }
    override suspend fun setSuccessOnLocation(value: Int) = withRow { roomDao.setSuccessOnLocation(value) }
    override suspend fun setRestoreMode(value: Int) = Unit
    override suspend fun setBadgeInfo(value: Int) = Unit
    override suspend fun setAppUpdateStatus(value: Int) = Unit
    override suspend fun setShowAlert(value: Int) = Unit
    override suspend fun setNotificationTime(value: Long) = Unit
    override suspend fun countEnterDetail(): Int = 0
    override suspend fun setReservedValue(value: Int) = Unit

    override fun observeTempScale(): Flow<Int> = roomDao.observeTempScale().map { it ?: 0 }
    override fun observeAutoRefresh(): Flow<Int> = roomDao.observeAutoRefresh().map { it ?: 0 }
    override fun observeAutoRefreshInterval(): Flow<Int> =
        roomDao.observeAutoRefreshInterval().map { it ?: 3 }
    override fun observeFavoriteLocation(): Flow<String> =
        roomDao.observeFavoriteLocation().map { it.orEmpty() }
    override fun observeActiveCpType(): Flow<String> = roomDao.observeActiveCpType().map { it.orEmpty() }
    override fun observeMigrationDone(): Flow<Int> = roomDao.observeMigrationDone().map { it ?: 0 }
    override fun observeWidgetCount(): Flow<Int> = roomDao.observeWidgetCount().map { it ?: 0 }
    override fun observeSuccessOnLocation(): Flow<Int> = roomDao.observeSuccessOnLocation().map { it ?: 0 }

    /** the single settings row is seeded lazily, as in the original */
    private suspend inline fun withRow(block: () -> Unit) {
        roomDao.ensureRow()
        block()
    }
}

/** Corresponds conceptually to the Room-backed widget DAO adapter. */
class StudyWidgetRoomBackedDao @Inject constructor(
    private val roomDao: StudyWidgetRoomDao,
) : StudyWidgetDao {

    override suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo? =
        roomDao.getWidget(widgetId)?.toDomain()

    override suspend fun getWidgetInfoList(): List<StudyWidgetInfo> =
        roomDao.getWidgets().map { it.toDomain() }

    override fun observeWidget(widgetId: Int): Flow<StudyWidgetInfo?> =
        roomDao.observeWidget(widgetId).map { it?.toDomain() }

    override fun observeWidgets(): Flow<List<StudyWidgetInfo>> =
        roomDao.observeWidgets().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCount(): Int = roomDao.getCount()

    override suspend fun isExist(widgetId: Int): Boolean = roomDao.isExist(widgetId)

    override suspend fun insert(info: StudyWidgetInfo): Long {
        val rowId = roomDao.insert(
            StudyWidgetEntity(
                widgetId = info.widgetId,
                key = info.weatherKey,
                widgetBGColor = info.widgetBGColor,
                widgetBGTransprency = info.widgetBGTransparency.toDouble(),
                nightMode = info.widgetNightMode,
                restoreMode = info.widgetRestoreMode,
                addedInDCMLauncher = info.widgetAddedInDCMLauncher,
                showNews = info.showNews,
                showHourly = info.showHourly,
                widgetShape = info.widgetShape,
                showBackground = info.showBackground,
            ),
        )
        roomDao.insertComponents(
            info.components.map {
                StudyWidgetComponentEntity(widgetId = info.widgetId, order = it.order, type = it.type)
            },
        )
        return rowId
    }

    override suspend fun delete(widgetId: Int): Int = roomDao.delete(widgetId)
    override suspend fun deleteAll(): Int = roomDao.deleteAll()
    override suspend fun updateKey(widgetId: Int, key: String): Int = roomDao.updateKey(widgetId, key)
    override suspend fun updateBGColor(widgetId: Int, color: Int): Int =
        roomDao.updateBGColor(widgetId, color)
    override suspend fun updateBGTransparency(widgetId: Int, transparency: Float): Int =
        roomDao.updateBGTransparency(widgetId, transparency.toDouble())
    override suspend fun updateDarkMode(widgetId: Int, mode: Int): Int =
        roomDao.updateNightMode(widgetId, mode)
    override suspend fun updateShowNews(widgetId: Int, show: Int): Int =
        roomDao.updateShowNews(widgetId, show)
    override suspend fun updateRestoreMode(widgetId: Int, mode: Int): Int =
        roomDao.updateRestoreMode(widgetId, mode)
    override suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int =
        roomDao.updateAddedInDCMLauncher(widgetId, added)

    override suspend fun updateWidgetComponent(widgetId: Int, order: Int, type: Int): Int {
        roomDao.upsertComponent(StudyWidgetComponentEntity(widgetId = widgetId, order = order, type = type))
        return 1
    }

    private fun dev.local.weatherstudy.persistence.database.relation.StudyWidgetWithComponents.toDomain() =
        StudyWidgetInfo(
            widgetId = widget.widgetId,
            weatherKey = widget.key.orEmpty(),
            showBackground = widget.showBackground ?: StudyWidgetInfo.SHOW,
            showHourly = widget.showHourly ?: StudyWidgetInfo.SHOW,
            showNews = widget.showNews ?: StudyWidgetInfo.HIDE,
            widgetBGColor = widget.widgetBGColor ?: StudyWidgetInfo.BG_COLOR_DEFAULT,
            widgetBGTransparency = (widget.widgetBGTransprency
                ?: StudyWidgetInfo.BG_TRANSPARENCY_DEFAULT.toDouble()).toFloat(),
            widgetNightMode = widget.nightMode ?: StudyWidgetInfo.NIGHT_MODE_SYSTEM,
            widgetShape = widget.widgetShape ?: StudyWidgetInfo.SHAPE_ROUND,
            widgetRestoreMode = widget.restoreMode ?: StudyWidgetInfo.RESTORE_NONE,
            widgetAddedInDCMLauncher = widget.addedInDCMLauncher ?: StudyWidgetInfo.HIDE,
            components = components
                .sortedBy { it.order }
                .map { StudyWidgetComponent(order = it.order, type = it.type) },
        )
}
