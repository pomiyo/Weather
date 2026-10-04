package dev.local.weatherstudy.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.local.weatherstudy.persistence.database.models.StudyAwayModeLocationsEntity
import dev.local.weatherstudy.persistence.database.models.StudyCorpAppEntity
import dev.local.weatherstudy.persistence.database.models.StudyInsightContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyLifeStyleSettingsEntity
import dev.local.weatherstudy.persistence.database.models.StudyRemoteConfigEntity
import dev.local.weatherstudy.persistence.database.models.StudySettingEntity
import dev.local.weatherstudy.persistence.database.models.StudyStatusEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetComponentEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetEntity
import dev.local.weatherstudy.persistence.database.relation.StudyWidgetWithComponents
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.SettingsRoomDao
 *
 * Observed responsibility — and the structural point:
 *
 * `TABLE_SETTING_INFO` is a **single-row table**, and the original's DAO therefore has
 * one accessor pair per COLUMN rather than a whole-row get/set. There are ~28 of them.
 * That looks redundant until you see the observe half: each column is observed
 * independently, so a temperature-unit change does not re-emit to a widget that is
 * watching the refresh interval. A whole-row `Flow<SettingEntity>` would wake every
 * observer on every write.
 *
 * Reconstructed with the same shape. A representative subset of the columns is
 * implemented; the pattern is identical for the rest and the full column list is in
 * [dev.local.weatherstudy.persistence.database.models.StudySettingEntity].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Dao
interface StudySettingsRoomDao {

    @Query("SELECT * FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = :id")
    suspend fun getRow(id: Int = SINGLE_ROW_ID): StudySettingEntity?

    @Upsert
    suspend fun upsert(entity: StudySettingEntity)

    @Query("SELECT COUNT(*) FROM TABLE_SETTING_INFO")
    suspend fun count(): Int

    // ---- per-column reads ----
    @Query("SELECT COL_SETTING_TEMP_SCALE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getTempScale(): Int?

    @Query("SELECT COL_SETTING_AUTO_REFRESH FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getAutoRefresh(): Int?

    @Query("SELECT COL_SETTING_AUTO_REFRESH_TIME FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getAutoRefreshInterval(): Int?

    @Query("SELECT COL_SETTING_AUTO_REF_NEXT_TIME FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getAutoRefreshNextTime(): Long?

    @Query("SELECT COL_SETTING_LAST_SEL_LOCATION FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getFavoriteLocation(): String?

    @Query("SELECT COL_SETTING_INITIAL_CP_TYPE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getActiveCpType(): String?

    @Query("SELECT COL_SETTING_HOME_CP_TYPE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getHomeCpType(): String?

    @Query("SELECT COL_SETTING_MIGRATION_DONE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getMigrationDone(): Int?

    @Query("SELECT COL_SETTING_WIDGET_COUNT FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getWidgetCount(): Int?

    @Query("SELECT COL_SETTING_LOCATION_SERVICES FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    suspend fun getSuccessOnLocation(): Int?

    // ---- per-column observes: each wakes only its own observers ----
    @Query("SELECT COL_SETTING_TEMP_SCALE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeTempScale(): Flow<Int?>

    @Query("SELECT COL_SETTING_AUTO_REFRESH FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeAutoRefresh(): Flow<Int?>

    @Query("SELECT COL_SETTING_AUTO_REFRESH_TIME FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeAutoRefreshInterval(): Flow<Int?>

    @Query("SELECT COL_SETTING_LAST_SEL_LOCATION FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeFavoriteLocation(): Flow<String?>

    @Query("SELECT COL_SETTING_INITIAL_CP_TYPE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeActiveCpType(): Flow<String?>

    @Query("SELECT COL_SETTING_MIGRATION_DONE FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeMigrationDone(): Flow<Int?>

    @Query("SELECT COL_SETTING_WIDGET_COUNT FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeWidgetCount(): Flow<Int?>

    @Query("SELECT COL_SETTING_LOCATION_SERVICES FROM TABLE_SETTING_INFO WHERE COL_SETTING_ID = 0")
    fun observeSuccessOnLocation(): Flow<Int?>

    // ---- per-column writes ----
    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_TEMP_SCALE = :value WHERE COL_SETTING_ID = 0")
    suspend fun setTempScale(value: Int)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_AUTO_REFRESH = :value WHERE COL_SETTING_ID = 0")
    suspend fun setAutoRefresh(value: Int)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_AUTO_REFRESH_TIME = :value WHERE COL_SETTING_ID = 0")
    suspend fun setAutoRefreshInterval(value: Int)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_AUTO_REF_NEXT_TIME = :value WHERE COL_SETTING_ID = 0")
    suspend fun setAutoRefreshNextTime(value: Long)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_LAST_SEL_LOCATION = :value WHERE COL_SETTING_ID = 0")
    suspend fun setFavoriteLocation(value: String)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_INITIAL_CP_TYPE = :value WHERE COL_SETTING_ID = 0")
    suspend fun setActiveCpType(value: String)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_HOME_CP_TYPE = :value WHERE COL_SETTING_ID = 0")
    suspend fun setHomeCpType(value: String)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_MIGRATION_DONE = :value WHERE COL_SETTING_ID = 0")
    suspend fun setMigrationDone(value: Int)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_WIDGET_COUNT = :value WHERE COL_SETTING_ID = 0")
    suspend fun setWidgetCount(value: Int)

    @Query("UPDATE TABLE_SETTING_INFO SET COL_SETTING_LOCATION_SERVICES = :value WHERE COL_SETTING_ID = 0")
    suspend fun setSuccessOnLocation(value: Int)

    /** the original seeds the single row on first access rather than in a callback */
    @Transaction
    suspend fun ensureRow() {
        if (count() == 0) upsert(StudySettingEntity(id = SINGLE_ROW_ID))
    }

    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.WidgetRoomDao
 *
 * Observed responsibility: per-field updates keyed by widget id, never whole-row
 * writes — several widgets of different kinds share this table, and the editor changes
 * one property at a time.
 */
@Dao
interface StudyWidgetRoomDao {

    @Transaction
    @Query("SELECT * FROM TABLE_WIDGET_INFO WHERE COL_WIDGET_ID = :widgetId")
    suspend fun getWidget(widgetId: Int): StudyWidgetWithComponents?

    @Transaction
    @Query("SELECT * FROM TABLE_WIDGET_INFO")
    suspend fun getWidgets(): List<StudyWidgetWithComponents>

    @Transaction
    @Query("SELECT * FROM TABLE_WIDGET_INFO WHERE COL_WIDGET_ID = :widgetId")
    fun observeWidget(widgetId: Int): Flow<StudyWidgetWithComponents?>

    @Transaction
    @Query("SELECT * FROM TABLE_WIDGET_INFO")
    fun observeWidgets(): Flow<List<StudyWidgetWithComponents>>

    @Query("SELECT COUNT(*) FROM TABLE_WIDGET_INFO")
    suspend fun getCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM TABLE_WIDGET_INFO WHERE COL_WIDGET_ID = :widgetId)")
    suspend fun isExist(widgetId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StudyWidgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(entities: List<StudyWidgetComponentEntity>)

    @Query("DELETE FROM TABLE_WIDGET_INFO WHERE COL_WIDGET_ID = :widgetId")
    suspend fun delete(widgetId: Int): Int

    @Query("DELETE FROM TABLE_WIDGET_INFO")
    suspend fun deleteAll(): Int

    @Query("UPDATE TABLE_WIDGET_INFO SET COL_WEATHER_KEY = :key WHERE COL_WIDGET_ID = :widgetId")
    suspend fun updateKey(widgetId: Int, key: String): Int

    @Query("UPDATE TABLE_WIDGET_INFO SET COL_WIDGET_BACKGROUND_COLOR = :color WHERE COL_WIDGET_ID = :widgetId")
    suspend fun updateBGColor(widgetId: Int, color: Int): Int

    @Query(
        "UPDATE TABLE_WIDGET_INFO SET COL_WIDGET_BACKGROUND_TRANSPARENCY = :transparency " +
            "WHERE COL_WIDGET_ID = :widgetId",
    )
    suspend fun updateBGTransparency(widgetId: Int, transparency: Double): Int

    @Query("UPDATE TABLE_WIDGET_INFO SET COL_WIDGET_NIGHT_MODE = :mode WHERE COL_WIDGET_ID = :widgetId")
    suspend fun updateNightMode(widgetId: Int, mode: Int): Int

    @Query("UPDATE TABLE_WIDGET_INFO SET COL_WIDGET_SHOW_NEWS = :show WHERE COL_WIDGET_ID = :widgetId")
    suspend fun updateShowNews(widgetId: Int, show: Int): Int

    @Query("UPDATE TABLE_WIDGET_INFO SET COL_WIDGET_RESTORE_MODE = :mode WHERE COL_WIDGET_ID = :widgetId")
    suspend fun updateRestoreMode(widgetId: Int, mode: Int): Int

    @Query(
        "UPDATE TABLE_WIDGET_INFO SET COL_WIDGET_ADDED_IN_DCM_LAUNCHER = :added " +
            "WHERE COL_WIDGET_ID = :widgetId",
    )
    suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int

    @Upsert
    suspend fun upsertComponent(entity: StudyWidgetComponentEntity)
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.StatusDao
 *
 * Observed responsibility: shared operation status — three columns, and `from` records
 * which surface began the operation. See [StudyStatusEntity].
 */
@Dao
interface StudyStatusDao {
    @Query("SELECT COL_STATUS_CODE FROM TABLE_STATUS_INFO WHERE COL_STATUS_ID = :id")
    fun observeStatus(id: String): Flow<Int?>

    @Query("SELECT COL_STATUS_FROM FROM TABLE_STATUS_INFO WHERE COL_STATUS_ID = :id")
    fun observeReason(id: String): Flow<Int?>

    @Upsert
    suspend fun upsert(entity: StudyStatusEntity)
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.InsightContentDao
 *
 * Observed responsibility: separate from the weather DAO even though the table cascades
 * from it, because the insight refresh runs on its own clock
 * ([dev.local.weatherstudy.domain.usecase.StudyReachToContentRefreshTime]).
 */
@Dao
interface StudyInsightContentDao {
    @Query("SELECT * FROM TABLE_INSIGHT_CONTENT_INFO WHERE COL_WEATHER_KEY = :key ORDER BY COL_INSIGHT_ORDER ASC")
    suspend fun getInsights(key: String): List<StudyInsightContentEntity>

    @Query("SELECT * FROM TABLE_INSIGHT_CONTENT_INFO WHERE COL_WEATHER_KEY = :key ORDER BY COL_INSIGHT_ORDER ASC")
    fun observeInsights(key: String): Flow<List<StudyInsightContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entities: List<StudyInsightContentEntity>)

    @Query("DELETE FROM TABLE_INSIGHT_CONTENT_INFO WHERE COL_WEATHER_KEY = :key")
    suspend fun clear(key: String)

    @Transaction
    suspend fun replace(key: String, entities: List<StudyInsightContentEntity>) {
        clear(key)
        insert(entities)
    }
}

/** Corresponds conceptually to `…dao.AwayModeLocationsDao`. */
@Dao
interface StudyAwayModeLocationsDao {
    @Query("SELECT * FROM TABLE_AWAY_MODE_LOCATIONS_INFO WHERE COL_AWAY_LOCATION = :key")
    suspend fun getByAwayKey(key: String): StudyAwayModeLocationsEntity?

    @Query("SELECT * FROM TABLE_AWAY_MODE_LOCATIONS_INFO WHERE COL_HOME_LOCATION = :key")
    suspend fun getByHomeKey(key: String): StudyAwayModeLocationsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StudyAwayModeLocationsEntity)

    @Query("DELETE FROM TABLE_AWAY_MODE_LOCATIONS_INFO")
    suspend fun clear()
}

/** Corresponds conceptually to `…dao.LifeStyleSettingsDao`. */
@Dao
interface StudyLifeStyleSettingsDao {
    @Query("SELECT * FROM TABLE_LIFESTYLE_SETTINGS_INFO")
    suspend fun getAll(): List<StudyLifeStyleSettingsEntity>

    @Query("SELECT * FROM TABLE_LIFESTYLE_SETTINGS_INFO")
    fun observeAll(): Flow<List<StudyLifeStyleSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<StudyLifeStyleSettingsEntity>)
}

/** Corresponds conceptually to `…dao.RemoteConfigDao`. */
@Dao
interface StudyRemoteConfigDao {
    @Query("SELECT * FROM TABLE_REMOTE_CONFIG_INFO WHERE COL_VERSION = :version")
    suspend fun get(version: String): StudyRemoteConfigEntity?

    @Upsert
    suspend fun upsert(entity: StudyRemoteConfigEntity)
}

/** Corresponds conceptually to `…dao.CorpAppDao`. */
@Dao
interface StudyCorpAppDao {
    @Query("SELECT * FROM TABLE_CORP_APP_INFO WHERE COL_PACKAGE_NAME = :packageName")
    suspend fun get(packageName: String): StudyCorpAppEntity?

    @Query("SELECT * FROM TABLE_CORP_APP_INFO")
    fun observeAll(): Flow<List<StudyCorpAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StudyCorpAppEntity)

    @Query("DELETE FROM TABLE_CORP_APP_INFO WHERE COL_PACKAGE_NAME = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM TABLE_CORP_APP_INFO")
    suspend fun deleteAll()
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.CursorRoomDao
 *
 * Observed responsibility: raw-cursor reads for the three exported content providers.
 * It exists because a `ContentProvider.query()` must return a `Cursor` and cannot
 * suspend — so this DAO bypasses the entity layer entirely and hands SQLite's own
 * cursor out across the process boundary. Column names are therefore external API.
 */
@Dao
interface StudyCursorRoomDao {
    @Query("SELECT * FROM TABLE_WEATHER_INFO ORDER BY COL_WEATHER_ORDER ASC")
    fun getAllCursor(): android.database.Cursor

    @Query("SELECT * FROM TABLE_WEATHER_INFO WHERE COL_WEATHER_KEY = :key")
    fun getByKeyCursor(key: String): android.database.Cursor

    @Query("SELECT * FROM TABLE_HOURLY_INFO ORDER BY COL_HOURLY_TIME ASC")
    fun getHourlyCursor(): android.database.Cursor

    @Query("SELECT * FROM TABLE_HOURLY_INFO WHERE COL_WEATHER_KEY = :key ORDER BY COL_HOURLY_TIME ASC")
    fun getHourlyCursor(key: String): android.database.Cursor

    @Query("SELECT * FROM TABLE_DAILY_INFO ORDER BY COL_DAILY_TIME ASC")
    fun getDailyCursor(): android.database.Cursor

    @Query("SELECT * FROM TABLE_DAILY_INFO WHERE COL_WEATHER_KEY = :key ORDER BY COL_DAILY_TIME ASC")
    fun getDailyCursor(key: String): android.database.Cursor

    @Query("SELECT * FROM TABLE_LIFE_INDEX_INFO")
    fun getIndexCursor(): android.database.Cursor

    @Query("SELECT * FROM TABLE_LIFE_INDEX_INFO WHERE COL_WEATHER_KEY = :key")
    fun getIndexCursor(key: String): android.database.Cursor

    @Query("SELECT * FROM TABLE_SETTING_INFO")
    fun getSettingsCursor(): android.database.Cursor
}
