package dev.local.weatherstudy.data.repo

import dev.local.weatherstudy.domain.repo.StudyLifeStyleSettingsRepo
import dev.local.weatherstudy.domain.repo.StudyProfileRepo
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyStatusRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.repo.StudyWidgetRepo
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import dev.local.weatherstudy.domain.source.local.StudyLifeStyleSettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyProfileDataSource
import dev.local.weatherstudy.domain.source.local.StudySettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWeatherLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWidgetLocalDataSource
import dev.local.weatherstudy.domain.source.remote.StudyWeatherRemoteDataSource
import dev.local.weatherstudy.persistence.database.dao.StudyStatusDao
import dev.local.weatherstudy.persistence.database.models.StudyStatusEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.repo.WeatherRepoImpl
 *
 * ### The finding this file exists to preserve
 *
 * The decompiled `WeatherRepoImpl` has exactly two fields, and they are **synthetic**:
 *
 * ```java
 * /* synthetic */ WeatherRemoteDataSource $$delegate_0;
 * /* synthetic */ WeatherLocalDataSource  $$delegate_1;
 * ```
 *
 * `$$delegate_N` is what the Kotlin compiler emits for **interface delegation**. So the
 * original is, in full:
 *
 * ```kotlin
 * class WeatherRepoImpl(
 *     remote: WeatherRemoteDataSource,
 *     local: WeatherLocalDataSource,
 * ) : WeatherRepo,
 *     WeatherRemoteDataSource by remote,
 *     WeatherLocalDataSource by local
 * ```
 *
 * **The repository has no body.** Not a thin one — none. It writes no method, makes no
 * decision about local-versus-remote, caches nothing and merges nothing. It exists only
 * to name the union of two capability sets, which `WeatherRepo` already declared.
 *
 * All the behaviour people usually expect in a repository lives in the use cases:
 * `GetWeather` decides what a read means, `FetchWeather` decides how the network is
 * fanned out, `RefreshForecast` sequences them, and `SaveWeather` persists. A
 * reconstruction that put caching or fallback logic here would invert the design and
 * make those use cases look redundant.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyWeatherRepoImpl @Inject constructor(
    remote: StudyWeatherRemoteDataSource,
    local: StudyWeatherLocalDataSource,
) : StudyWeatherRepo,
    StudyWeatherRemoteDataSource by remote,
    StudyWeatherLocalDataSource by local

/**
 * Corresponds conceptually to `…data.repo.SettingsRepoImpl`.
 *
 * Same pattern, one delegate: the decompiled class has a single
 * `SettingsLocalDataSource localDataSource` field and no methods of its own.
 */
@Singleton
class StudySettingsRepoImpl @Inject constructor(
    localDataSource: StudySettingsLocalDataSource,
) : StudySettingsRepo, StudySettingsLocalDataSource by localDataSource

/** Corresponds conceptually to `…data.repo.LifeStyleSettingsRepoImpl`. */
@Singleton
class StudyLifeStyleSettingsRepoImpl @Inject constructor(
    localDataSource: StudyLifeStyleSettingsLocalDataSource,
) : StudyLifeStyleSettingsRepo, StudyLifeStyleSettingsLocalDataSource by localDataSource

/** Corresponds conceptually to `…data.repo.ProfileRepoImpl`. */
@Singleton
class StudyProfileRepoImpl @Inject constructor(
    dataSource: StudyProfileDataSource,
) : StudyProfileRepo, StudyProfileDataSource by dataSource

/**
 * Corresponds conceptually to `…data.repo.WidgetRepoImpl`.
 *
 * Observed difference from the delegating repositories: `WidgetRepo` declares its own
 * method names, which differ from the data source's (`updateGoDark` vs `updateDarkMode`,
 * `observeWidgetInfo` vs `observeWidget`). So this one cannot delegate — it has to
 * translate. The decompiled class has a single `WidgetLocalDataSource dataSource` field
 * and real method bodies, unlike `WeatherRepoImpl`.
 */
@Singleton
class StudyWidgetRepoImpl @Inject constructor(
    private val dataSource: StudyWidgetLocalDataSource,
) : StudyWidgetRepo {

    override suspend fun addWidgetInfo(widgetInfo: StudyWidgetInfo): Long =
        dataSource.insert(widgetInfo)

    override suspend fun deleteWidgetInfo(widgetId: Int): Int = dataSource.delete(widgetId)
    override suspend fun deleteAllWidgetInfo(): Int = dataSource.deleteAll()
    override suspend fun getWidgetCount(): Int = dataSource.getCount()
    override suspend fun isExist(widgetId: Int): Boolean = dataSource.isExist(widgetId)
    override suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo? =
        dataSource.getWidgetInfo(widgetId)
    override suspend fun getWidgetInfoList(): List<StudyWidgetInfo> = dataSource.getWidgetInfoList()
    override fun observeWidgetInfo(widgetId: Int): Flow<StudyWidgetInfo?> =
        dataSource.observeWidget(widgetId)
    override fun observeWidgetInfoList(): Flow<List<StudyWidgetInfo>> = dataSource.observeWidgets()

    override suspend fun updateWeatherKey(widgetId: Int, key: String): Int =
        dataSource.updateKey(widgetId, key)
    override suspend fun updateWidgetBGColor(widgetId: Int, color: Int): Int =
        dataSource.updateBGColor(widgetId, color)
    override suspend fun updateWidgetBGTransparency(widgetId: Int, transparency: Float): Int =
        dataSource.updateBGTransparency(widgetId, transparency)

    /** the repository's vocabulary differs from the source's here */
    override suspend fun updateGoDark(widgetId: Int, mode: Int): Int =
        dataSource.updateDarkMode(widgetId, mode)

    override suspend fun updateShowNews(widgetId: Int, show: Int): Int =
        dataSource.updateShowNews(widgetId, show)
    override suspend fun updateWidgetRestoreMode(widgetId: Int, mode: Int): Int =
        dataSource.updateRestoreMode(widgetId, mode)
    override suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int =
        dataSource.updateAddedInDCMLauncher(widgetId, added)
    override suspend fun updateWidgetComponentInfo(widgetId: Int, order: Int, type: Int): Int =
        dataSource.updateWidgetComponent(widgetId, order, type)
}

/**
 * Corresponds conceptually to `…data.repo.StatusRepoImpl`.
 *
 * Observed responsibility: the shared operation-status store. It reads the DAO directly
 * rather than going through a data source — the original's `StatusRepo` has no matching
 * `StatusDataSource`, which is consistent: there is exactly one place this state can live.
 */
@Singleton
class StudyStatusRepoImpl @Inject constructor(
    private val statusDao: StudyStatusDao,
) : StudyStatusRepo {

    override fun getStatus(id: String): Flow<Int> = statusDao.observeStatus(id).map { it ?: 0 }

    override fun getReason(id: String): Flow<Int> = statusDao.observeReason(id).map { it ?: 0 }

    override suspend fun setStatus(id: String, status: Int, reason: Int) =
        statusDao.upsert(StudyStatusEntity(id = id, status = status, from = reason))
}
