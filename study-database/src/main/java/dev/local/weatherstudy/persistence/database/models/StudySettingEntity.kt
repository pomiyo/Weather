package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.SettingEntity
 *
 * Table `TABLE_SETTING_INFO`, 32 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibilities:
 * - a SINGLE-ROW table (`COL_SETTING_ID` is the key and only one row is ever written)
 *   acting as the app's key-value state store: user preferences, consent state,
 *   migration state, restore state, widget count, provider type, counters
 * - **column names have drifted from their meaning across 51 schema versions**, and
 *   the exported schema records it. Three columns are repurposed:
 *     `COL_SETTING_DEFAULT_LOCATION`        now holds `privacyPolicyGrantVersion`
 *     `COL_SETTING_PINNED_LOCATION`         now holds `mostProbableActivity`
 *     `COL_SETTING_AUTO_REFRESH_ON_OPENING` now holds `reservedValue`
 *   The field names below are the original's; the column names are kept as-is because
 *   renaming a column is a migration, and the app chose not to pay it. This is
 *   preserved deliberately — it is what a long-lived schema actually looks like.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_SETTING_INFO,
    primaryKeys = [StudyDbConstants.COL_SETTING_ID],
)
data class StudySettingEntity(
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_ID)
    val id: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_TEMP_SCALE)
    val tempScale: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_AUTO_REFRESH_TIME)
    val autoRefreshInterval: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_AUTO_REF_NEXT_TIME)
    val autoRefreshNextTime: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_NOTIFICATION_SET_TIME)
    val notificationTime: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_LAST_SEL_LOCATION)
    val favoriteLocation: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_LAST_EDGE_LOCATION)
    val lastEdgeLocation: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_SHOW_USE_LOCATION_POPUP)
    val privacyPolicyAgreement: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_WIDGET_COUNT)
    val widgetCount: Int = 0,
    @ColumnInfo(name = StudyDbConstants.DAEMON_DIVISION_CHECK)
    val daemonVersion: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_DEFAULT_LOCATION)
    val privacyPolicyGrantVersion: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_LOCATION_SERVICES)
    val successOnLocation: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_SHOW_MOBILE_POPUP)
    val consentToUseMobileNetwork: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_SHOW_WLAN_POPUP)
    val consentToUseWlan: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_PERMISSION_NOTICE)
    val consentToPermissionNotice: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_SHOW_CHARGER_POPUP)
    val consentToNetworkCharges: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_INITIAL_CP_TYPE)
    val activeCpType: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_RESTORE_MODE)
    val restoreMode: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_RECOMMEND_UPDATE_TIME)
    val recommendUpdateTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_MIGRATION_DONE)
    val migrationDone: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_PINNED_LOCATION)
    val mostProbableActivity: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_SHOW_ALERT)
    val showAlert: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_MARKET_UPDATE_BADGE)
    val badgeInfo: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_FORCED_UPDATE)
    val appUpdateStatus: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_IS_INIT_DONE)
    val isInitDone: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_AUTO_REFRESH_ON_OPENING)
    val reservedValue: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_ST_SETTINGS_STATE)
    val stSettingsState: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_NEWS_OPT_IN_DONE)
    val newsOptInDone: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_PP_VERSION)
    val ppVersion: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_PP_GRANT_VERSION)
    val ppGrantVersion: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_AUTO_REFRESH)
    val autoRefresh: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_SETTING_HOME_CP_TYPE)
    val homeCpType: String? = null,
)
