package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.WidgetEntity
 *
 * Table `TABLE_WIDGET_INFO`, 11 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: one row per placed widget, keyed by AppWidgetManager id.
 * `COL_WEATHER_KEY` is nullable here and has no foreign key — a widget may point at
 * a location that has been deleted, which is the orphan case
 * `StudyAddLocation.rebindOrphanWidgets` repairs.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_WIDGET_INFO,
    primaryKeys = [StudyDbConstants.COL_WIDGET_ID],
)
data class StudyWidgetEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_ID)
    val widgetId: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_BACKGROUND_COLOR)
    val widgetBGColor: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_BACKGROUND_TRANSPARENCY)
    val widgetBGTransprency: Double? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_NIGHT_MODE)
    val nightMode: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_RESTORE_MODE)
    val restoreMode: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_ADDED_IN_DCM_LAUNCHER)
    val addedInDCMLauncher: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_SHOW_NEWS)
    val showNews: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_SHOW_HOURLY)
    val showHourly: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_SHAPE)
    val widgetShape: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_SHOW_BACKGROUND)
    val showBackground: Int? = null,
)
