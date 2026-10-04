package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.WidgetComponentEntity
 *
 * Table `TABLE_WIDGET_COMPONENT_INFO`, 3 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: the reorderable blocks inside one widget, composite key
 * (widget id + order), cascading from the widget row.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_WIDGET_COMPONENT_INFO,
    primaryKeys = [StudyDbConstants.COL_WIDGET_ID, StudyDbConstants.COL_WIDGET_COMPONENT_ORDER],
    foreignKeys = [
        ForeignKey(
            entity = StudyWidgetEntity::class,
            parentColumns = [StudyDbConstants.COL_WIDGET_ID],
            childColumns = [StudyDbConstants.COL_WIDGET_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyWidgetComponentEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_ID)
    val widgetId: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_COMPONENT_ORDER)
    val order: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_WIDGET_COMPONENT_TYPE)
    val type: Int = 0,
)
