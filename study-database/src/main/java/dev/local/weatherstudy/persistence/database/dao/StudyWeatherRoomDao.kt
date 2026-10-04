package dev.local.weatherstudy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.local.weatherstudy.persistence.database.models.StudyAlertEntity
import dev.local.weatherstudy.persistence.database.models.StudyContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyDailyEntity
import dev.local.weatherstudy.persistence.database.models.StudyForecastChangeEntity
import dev.local.weatherstudy.persistence.database.models.StudyHourlyEntity
import dev.local.weatherstudy.persistence.database.models.StudyIndexEntity
import dev.local.weatherstudy.persistence.database.models.StudyInsightContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyLifeStyleEntity
import dev.local.weatherstudy.persistence.database.models.StudyWeatherEntity
import dev.local.weatherstudy.persistence.database.models.StudyWebMenuEntity
import dev.local.weatherstudy.persistence.database.relation.StudyWeatherWithChildren
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.WeatherRoomDao
 *
 * Observed responsibilities:
 * - the Room half of the three-tier DAO family. The interface the repository depends
 *   on lives in `:study-persistence` as `StudyWeatherDao`; this is one of its three
 *   implementations, alongside a legacy-cursor one and an in-memory cache:
 *
 *   ```
 *   StudyWeatherDao  (interface, :study-persistence)
 *        ├── StudyWeatherRoomDao      ← this file, :study-database
 *        ├── StudyWeatherDbDao        ← legacy raw-SQLite path (stubbed)
 *        └── StudyWeatherInMemoryDao  ← cache
 *   ```
 *
 * - writes are `@Transaction` methods that fan out to the child tables, because the
 *   aggregate spans eight tables. Reads use a `@Relation` POJO
 *   ([StudyWeatherWithChildren]) so one call returns the whole graph.
 * - the child deletes are not written out: every child table CASCADEs from
 *   `COL_WEATHER_KEY`, so deleting the root row is sufficient. That is a schema
 *   decision doing work the DAO would otherwise have to.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Dao
interface StudyWeatherRoomDao {

    // ---------------- queries ----------------

    @Transaction
    @Query("SELECT * FROM TABLE_WEATHER_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun getWeather(key: String): StudyWeatherWithChildren?

    @Transaction
    @Query("SELECT * FROM TABLE_WEATHER_INFO ORDER BY COL_WEATHER_ORDER ASC")
    suspend fun getWeathers(): List<StudyWeatherWithChildren>

    @Transaction
    @Query("SELECT * FROM TABLE_WEATHER_INFO ORDER BY COL_WEATHER_ORDER ASC")
    fun observeWeathers(): Flow<List<StudyWeatherWithChildren>>

    @Query("SELECT COUNT(*) FROM TABLE_WEATHER_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun getCount(key: String): Int

    @Query("SELECT COUNT(*) FROM TABLE_WEATHER_INFO")
    suspend fun getTotalCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM TABLE_WEATHER_INFO WHERE COL_WEATHER_KEY = :key)")
    suspend fun isExist(key: String): Boolean

    // ---------------- root row ----------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(entity: StudyWeatherEntity): Long

    @Update
    suspend fun updateWeather(entity: StudyWeatherEntity): Int

    @Delete
    suspend fun deleteWeather(entity: StudyWeatherEntity): Int

    @Query("DELETE FROM TABLE_WEATHER_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun deleteWeatherByKey(key: String): Int

    @Query("DELETE FROM TABLE_WEATHER_INFO WHERE COL_WEATHER_KEY IN (:keys)")
    suspend fun deleteWeathersByKey(keys: List<String>): Int

    @Query("DELETE FROM TABLE_WEATHER_INFO")
    suspend fun deleteAll(): Int

    @Query("UPDATE TABLE_WEATHER_INFO SET COL_WEATHER_ORDER = :order WHERE COL_WEATHER_KEY = :key")
    suspend fun updateOrder(key: String, order: Int): Int

    @Query(
        "UPDATE TABLE_WEATHER_INFO SET COL_WEATHER_LOCATION_LABEL = :label, " +
            "COL_WEATHER_LOCATION_LABEL_TYPE = :labelType WHERE COL_WEATHER_KEY = :key",
    )
    suspend fun updateLabel(key: String, label: String, labelType: String): Int

    // ---------------- child tables ----------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourly(entities: List<StudyHourlyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaily(entities: List<StudyDailyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndices(entities: List<StudyIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(entities: List<StudyAlertEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(entities: List<StudyContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebMenus(entities: List<StudyWebMenuEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifeStyles(entities: List<StudyLifeStyleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecastChange(entity: StudyForecastChangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(entities: List<StudyInsightContentEntity>)

    @Query("DELETE FROM TABLE_HOURLY_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearHourly(key: String)

    @Query("DELETE FROM TABLE_DAILY_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearDaily(key: String)

    @Query("DELETE FROM TABLE_LIFE_INDEX_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearIndices(key: String)

    @Query("DELETE FROM TABLE_ALERT_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearAlerts(key: String)

    @Query("DELETE FROM TABLE_CONTENT_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearContents(key: String)

    @Query("DELETE FROM TABLE_WEB_MENU_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearWebMenus(key: String)

    @Query("DELETE FROM TABLE_LIFESTYLE_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearLifeStyles(key: String)

    @Query("DELETE FROM TABLE_INSIGHT_CONTENT_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clearInsights(key: String)

    // ---------------- aggregate write ----------------

    /**
     * Replace one location and every child row it owns.
     *
     * Reconstruction of the original's aggregate save: clear the children first, then
     * re-insert. A REPLACE on the children alone would leave rows for hours and days
     * the new forecast no longer covers.
     */
    @Transaction
    suspend fun saveAggregate(aggregate: StudyWeatherWithChildren): Long {
        val key = aggregate.weather.key
        clearHourly(key)
        clearDaily(key)
        clearIndices(key)
        clearAlerts(key)
        clearContents(key)
        clearWebMenus(key)
        clearLifeStyles(key)
        clearInsights(key)

        val rowId = insertWeather(aggregate.weather)
        insertHourly(aggregate.hourly)
        insertDaily(aggregate.daily)
        insertIndices(aggregate.indices)
        insertAlerts(aggregate.alerts)
        insertContents(aggregate.contents)
        insertWebMenus(aggregate.webMenus)
        insertLifeStyles(aggregate.lifeStyles)
        insertInsights(aggregate.insights)
        aggregate.forecastChange?.let { insertForecastChange(it) }
        return rowId
    }

    @Transaction
    suspend fun saveAggregates(aggregates: List<StudyWeatherWithChildren>): List<Long> =
        aggregates.map { saveAggregate(it) }

    /**
     * Reconstruction of `replaceWeathers`: swap the entire saved set atomically. Used by
     * backup-restore and by the provider-change flow, where a partial result would leave
     * the user with a mix of two providers' data.
     */
    @Transaction
    suspend fun replaceAggregates(aggregates: List<StudyWeatherWithChildren>): Int {
        deleteAll()
        saveAggregates(aggregates)
        return aggregates.size
    }

    @Transaction
    suspend fun reorder(keys: List<String>): Int {
        keys.forEachIndexed { index, key -> updateOrder(key, index) }
        return keys.size
    }
}
