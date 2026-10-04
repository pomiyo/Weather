package dev.local.weatherstudy.domain.source.local

import dev.local.weatherstudy.domain.entity.content.StudyLifeStyleSettings
import dev.local.weatherstudy.domain.entity.weather.StudyTheme
import dev.local.weatherstudy.domain.entity.weather.StudyThemePlace
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.local.WidgetLocalDataSource
 *
 * Observed responsibility: widget↔location binding and per-widget appearance.
 * Every setter is per-field and per-widget-id, because a widget update must not
 * rewrite the whole row (several widgets of different kinds share the table).
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyWidgetLocalDataSource {
    suspend fun insert(widgetInfo: StudyWidgetInfo): Long
    suspend fun delete(widgetId: Int): Int
    suspend fun deleteAll(): Int
    suspend fun getCount(): Int
    suspend fun isExist(widgetId: Int): Boolean
    suspend fun getWidgetInfo(widgetId: Int): StudyWidgetInfo?
    suspend fun getWidgetInfoList(): List<StudyWidgetInfo>
    fun observeWidget(widgetId: Int): Flow<StudyWidgetInfo?>
    fun observeWidgets(): Flow<List<StudyWidgetInfo>>

    suspend fun updateKey(widgetId: Int, key: String): Int
    suspend fun updateBGColor(widgetId: Int, color: Int): Int
    suspend fun updateBGTransparency(widgetId: Int, transparency: Float): Int
    suspend fun updateDarkMode(widgetId: Int, mode: Int): Int
    suspend fun updateShowNews(widgetId: Int, show: Int): Int
    suspend fun updateRestoreMode(widgetId: Int, mode: Int): Int
    suspend fun updateAddedInDCMLauncher(widgetId: Int, added: Int): Int
    suspend fun updateWidgetComponent(widgetId: Int, order: Int, type: Int): Int
}

/** Corresponds conceptually to `…local.LifeStyleSettingsLocalDataSource`. */
interface StudyLifeStyleSettingsLocalDataSource {
    suspend fun getSettings(): List<StudyLifeStyleSettings>
    suspend fun setSettings(settings: List<StudyLifeStyleSettings>)
}

/**
 * Corresponds conceptually to `…local.ThemeLocalDataSource`.
 *
 * Observed responsibility: caches the map-search themed-place catalogue, keyed by
 * language — `getUpdatedLanguage()` exists so a locale change invalidates it.
 */
interface StudyThemeLocalDataSource {
    fun getLocalCategories(): Flow<List<StudyTheme>>
    fun getLocalRegions(categoryId: String): Flow<List<StudyTheme>>
    fun getUpdatedTime(): Flow<Long>
    fun getUpdatedLanguage(): Flow<String>
    suspend fun setLocalTheme(
        categories: List<StudyTheme>,
        places: Map<String, List<StudyThemePlace>>,
        updatedTime: Long,
        language: String,
    )
}

/**
 * Corresponds conceptually to `…local.ProfileDataSource`.
 *
 * Observed responsibility: the device/firmware profile the policy layer branches on.
 * `salesCode` is the Samsung CSC code — see `StudyCscFeature` in `:study-system-service`
 * for where it actually comes from on a Samsung device, and why it is stubbed here.
 */
interface StudyProfileDataSource {
    suspend fun getCountryCode(): String
    suspend fun setCountryCode(code: String)
    suspend fun getSalesCode(): String
    suspend fun setSalesCode(code: String)
    suspend fun getOneUiVersion(): Int
    suspend fun setOneUiVersion(version: Int)
    suspend fun getFirstApiLevel(): Int
    suspend fun setFirstApiLevel(level: Int)
    suspend fun getUclVersion(): Int
    suspend fun setPpVersion(version: Int)
}
