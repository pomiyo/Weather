package dev.local.weatherstudy.domain.source.local

import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.local.SettingsQueryDataSource
 * com.samsung.android.weather.domain.source.local.SettingsCommandDataSource
 * com.samsung.android.weather.domain.source.local.SettingsObserveDataSource
 * com.samsung.android.weather.domain.source.local.SettingsLocalDataSource
 *
 * Observed responsibilities:
 * - the same query/command/observe split as the weather source
 * - the settings "table" is wide and flat (TABLE_SETTING_INFO, 32 columns) and the
 *   interface is one accessor triple per column rather than a settings object.
 *   That is deliberate in the original: each column is observed independently, so a
 *   temperature-scale change does not re-emit to a widget watching the refresh interval.
 * - note how much is NOT user-facing preference but app state: migration done,
 *   restore mode, badge info, widget count, CP (content-provider) type, enter-detail
 *   counters. The settings table doubles as the app's key-value state store.
 *
 * The accessor set below mirrors the original's column-for-column.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudySettingsQueryDataSource {
    suspend fun getTempScale(): Int
    suspend fun getAutoRefresh(): Int
    suspend fun getAutoRefreshInterval(): Int
    suspend fun getAutoRefreshNextTime(): Long
    suspend fun getFavoriteLocation(): String
    suspend fun getLastEdgeLocation(): String
    suspend fun getActiveCpType(): String
    suspend fun getHomeCpType(): String
    suspend fun needChangeCp(): Boolean
    suspend fun showCpChangePopup(): Int
    suspend fun getShowAlert(): Int
    suspend fun getBadgeInfo(): Int
    suspend fun getAppUpdateStatus(): Int
    suspend fun getDaemonVersion(): String
    suspend fun whetherMigrationDone(): Int
    suspend fun getRestoreMode(): Int
    suspend fun getWidgetCount(): Int
    suspend fun getNotificationTime(): Long
    suspend fun getEnterDetailCount(): Int
    suspend fun getEnterDetailCountWithApproximateLocation(): Int
    suspend fun getSuccessOnLocation(): Int
    suspend fun isAwayMode(): Boolean
    suspend fun isAwayModeFirstAccess(): Boolean
    suspend fun getRepresentFirstStart(): Boolean
    suspend fun getMostProbableActivity(): Int
    suspend fun getNewsOptInDone(): Int
    suspend fun getSmartThingsSettingsState(): Int
    suspend fun getReservedValue(): Int
}

/** Corresponds conceptually to `…local.SettingsCommandDataSource`. */
interface StudySettingsCommandDataSource {
    suspend fun setTempScale(scale: Int)
    suspend fun setAutoRefresh(value: Int)
    suspend fun setAutoRefreshInterval(interval: Int)
    suspend fun setAutoRefreshNextTime(time: Long)
    suspend fun setFavoriteLocation(key: String)
    suspend fun setLastEdgeLocation(key: String)
    suspend fun setActiveCpType(type: String)
    suspend fun setHomeCpType(type: String)
    suspend fun setNeedChangeCp(need: Boolean)
    suspend fun setShowCpChangePopup(value: Int)
    suspend fun setShowAlert(value: Int)
    suspend fun setBadgeInfo(value: Int)
    suspend fun setAppUpdateStatus(status: Int)
    suspend fun setDaemonVersion(version: String)
    suspend fun setMigrationDone(value: Int)
    suspend fun setRestoreMode(mode: Int)
    suspend fun setWidgetCount(count: Int)
    suspend fun setNotificationTime(time: Long)
    suspend fun countEnterDetail(): Int
    suspend fun countEnterDetailWithApproximateLocation(): Int
    suspend fun setSuccessOnLocation(value: Int)
    suspend fun setIsAwayMode(away: Boolean)
    suspend fun setAwayModeFirstAccess(first: Boolean)
    suspend fun setRepresentFirstStart(first: Boolean)
    suspend fun setMostProbableActivity(activity: Int)
    suspend fun setNewsOptInDone(value: Int)
    suspend fun setSmartThingsSettingsState(state: Int)
    suspend fun setReservedValue(value: Int)
}

/** Corresponds conceptually to `…local.SettingsObserveDataSource`. */
interface StudySettingsObserveDataSource {
    fun observeTempScale(): Flow<Int>
    fun observeAutoRefresh(): Flow<Int>
    fun observeAutoRefreshInterval(): Flow<Int>
    fun observeAutoRefreshNextTime(): Flow<Long>
    fun observeFavoriteLocation(): Flow<String>
    fun observeLastEdgeLocation(): Flow<String>
    fun observeActiveCpType(): Flow<String>
    fun observeHomeCpType(): Flow<String>
    fun observeNeedChangeCp(): Flow<Boolean>
    fun observeShowAlert(): Flow<Int>
    fun observeBadgeInfo(): Flow<Int>
    fun observeAppUpdateStatus(): Flow<Int>
    fun observeDaemonVersion(): Flow<String>
    fun observeMigrationDone(): Flow<Int>
    fun observeRestoreMode(): Flow<Int>
    fun observeWidgetCount(): Flow<Int>
    fun observeNotificationTime(): Flow<Long>
    fun observeEnterDetailCount(): Flow<Int>
    fun observeEnterDetailCountWithApproximateLocation(): Flow<Int>
    fun observeSuccessOnLocation(): Flow<Int>
    fun observeAwayModeFirstAccess(): Flow<Boolean>
    fun observeMostProbableActivity(): Flow<Int>
    fun observeNewsOptInDone(): Flow<Int>
    fun observeSmartThingsSettingsState(): Flow<Int>
    fun observeReservedValue(): Flow<Int>
}

/** Corresponds conceptually to `…local.SettingsLocalDataSource` — the union. */
interface StudySettingsLocalDataSource :
    StudySettingsQueryDataSource,
    StudySettingsCommandDataSource,
    StudySettingsObserveDataSource
