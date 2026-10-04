package dev.local.weatherstudy.persistence.cache

import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import dev.local.weatherstudy.persistence.dao.StudyWeatherDao
import dev.local.weatherstudy.persistence.dao.StudyWidgetDao
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.cache.WeatherInMemoryDao
 * com.samsung.android.weather.persistence.cache.WidgetInMemoryDao
 * com.samsung.android.weather.persistence.cache.SettingsInMemoryDao
 * com.samsung.android.weather.data.source.local.WeatherInMemoryDataSource
 * com.samsung.android.weather.backend.cache.BackendInMemoryDao
 * com.samsung.android.weather.data.source.secure.SecureDataInMemoryDao
 *
 * ### Why the app has a whole in-memory DAO tier
 *
 * Six of the DAO families have an `InMemory` implementation, and they are not a
 * performance afterthought — they are a *correctness* mechanism for the non-UI
 * surfaces. A widget update, a complication refresh and a `ContentProvider.query()`
 * all run in short-lived contexts where touching disk is either slow (RemoteViews must
 * be built promptly) or impossible (a provider query cannot suspend). The in-memory
 * tier serves those reads from whatever the last disk read produced.
 *
 * Because it implements the SAME contract as the Room tier, nothing above the DAO knows
 * which one it got — the choice is a DI binding, which is the whole point of the
 * three-tier family.
 *
 * `@Singleton` is essential here: a per-injection instance would defeat it.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyWeatherInMemoryDao @Inject constructor() : StudyWeatherDao {

    private val cache = MutableStateFlow<Map<String, StudyWeather>>(emptyMap())
    private val awayLocations = ConcurrentHashMap<String, StudyAwayModeLocation>()

    override suspend fun getWeather(key: String): StudyWeather? = cache.value[key]

    override suspend fun getWeathers(): List<StudyWeather> =
        cache.value.values.sortedBy { it.location.priority }

    override fun observeWeathers(): Flow<List<StudyWeather>> =
        cache.asStateFlow().map { it.values.sortedBy { w -> w.location.priority } }

    override suspend fun getCount(key: String): Int = if (cache.value.containsKey(key)) 1 else 0

    override suspend fun isExist(key: String): Boolean = cache.value.containsKey(key)

    override suspend fun saveWeather(weather: StudyWeather): Long {
        cache.value = cache.value + (weather.location.key to weather)
        return 1L
    }

    override suspend fun saveWeathers(weathers: List<StudyWeather>): List<Long> {
        cache.value = cache.value + weathers.associateBy { it.location.key }
        return weathers.map { 1L }
    }

    override suspend fun updateWeather(weather: StudyWeather): Int {
        saveWeather(weather)
        return 1
    }

    override suspend fun updateWeathers(weathers: List<StudyWeather>): Int {
        saveWeathers(weathers)
        return weathers.size
    }

    override suspend fun replaceWeathers(weathers: List<StudyWeather>): Int {
        cache.value = weathers.associateBy { it.location.key }
        return weathers.size
    }

    override suspend fun deleteWeather(key: String): Int {
        val had = cache.value.containsKey(key)
        cache.value = cache.value - key
        return if (had) 1 else 0
    }

    override suspend fun deleteWeathersByKey(keys: List<String>): Int {
        val before = cache.value.size
        cache.value = cache.value - keys.toSet()
        return before - cache.value.size
    }

    override suspend fun deleteAll(): Int {
        val before = cache.value.size
        cache.value = emptyMap()
        return before
    }

    override suspend fun updateOrder(keys: List<String>): Int {
        cache.value = cache.value.mapValues { (key, weather) ->
            val index = keys.indexOf(key)
            if (index >= 0) weather.copy(location = weather.location.copy(priority = index)) else weather
        }
        return keys.size
    }

    override suspend fun updateLabel(key: String, label: String, labelType: String): Int {
        val existing = cache.value[key] ?: return 0
        cache.value = cache.value + (
            key to existing.copy(
                location = existing.location.copy(
                    label = label,
                    labelType = labelType.toIntOrNull() ?: 0,
                ),
            )
            )
        return 1
    }

    override suspend fun addAwayLocation(location: StudyAwayModeLocation) {
        awayLocations[location.awayLocation] = location
    }

    override suspend fun getAwayLocationByAwayKey(key: String): StudyAwayModeLocation? =
        awayLocations[key]

    override suspend fun getAwayLocationByHomeKey(key: String): StudyAwayModeLocation? =
        awayLocations.values.firstOrNull { it.homeLocation == key }

    override suspend fun clearAwayLocations() = awayLocations.clear()
}

/**
 * Corresponds conceptually to `…persistence.cache.WidgetInMemoryDao`.
 *
 * Observed responsibility: the widget surfaces read this one. An `AppWidgetProvider`
 * `onUpdate` has a few seconds and may be invoked for many ids at once, so a cached
 * widget↔location binding is what keeps the Glance render off disk.
 */
@Singleton
class StudyWidgetInMemoryDao @Inject constructor() : StudyWidgetDao {

    private val cache = MutableStateFlow<Map<Int, StudyWidgetInfo>>(emptyMap())

    override suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo? = cache.value[widgetId]
    override suspend fun getWidgetInfoList(): List<StudyWidgetInfo> = cache.value.values.toList()
    override fun observeWidget(widgetId: Int): Flow<StudyWidgetInfo?> =
        cache.asStateFlow().map { it[widgetId] }
    override fun observeWidgets(): Flow<List<StudyWidgetInfo>> =
        cache.asStateFlow().map { it.values.toList() }
    override suspend fun getCount(): Int = cache.value.size
    override suspend fun isExist(widgetId: Int): Boolean = cache.value.containsKey(widgetId)

    override suspend fun insert(info: StudyWidgetInfo): Long {
        cache.value = cache.value + (info.widgetId to info)
        return 1L
    }

    override suspend fun delete(widgetId: Int): Int {
        val had = cache.value.containsKey(widgetId)
        cache.value = cache.value - widgetId
        return if (had) 1 else 0
    }

    override suspend fun deleteAll(): Int {
        val before = cache.value.size
        cache.value = emptyMap()
        return before
    }

    override suspend fun updateKey(widgetId: Int, key: String) = mutate(widgetId) { it.copy(weatherKey = key) }
    override suspend fun updateBGColor(widgetId: Int, color: Int) =
        mutate(widgetId) { it.copy(widgetBGColor = color) }
    override suspend fun updateBGTransparency(widgetId: Int, transparency: Float) =
        mutate(widgetId) { it.copy(widgetBGTransparency = transparency) }
    override suspend fun updateDarkMode(widgetId: Int, mode: Int) =
        mutate(widgetId) { it.copy(widgetNightMode = mode) }
    override suspend fun updateShowNews(widgetId: Int, show: Int) =
        mutate(widgetId) { it.copy(showNews = show) }
    override suspend fun updateRestoreMode(widgetId: Int, mode: Int) =
        mutate(widgetId) { it.copy(widgetRestoreMode = mode) }
    override suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int) =
        mutate(widgetId) { it.copy(widgetAddedInDCMLauncher = added) }

    override suspend fun updateWidgetComponent(widgetId: Int, order: Int, type: Int) =
        mutate(widgetId) { info ->
            val others = info.components.filterNot { it.order == order }
            info.copy(
                components = (
                    others + dev.local.weatherstudy.domain.entity.widget.StudyWidgetComponent(order, type)
                    ).sortedBy { it.order },
            )
        }

    private inline fun mutate(widgetId: Int, transform: (StudyWidgetInfo) -> StudyWidgetInfo): Int {
        val existing = cache.value[widgetId] ?: return 0
        cache.value = cache.value + (widgetId to transform(existing))
        return 1
    }
}
