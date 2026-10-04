package dev.local.weatherstudy.persistence.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import dev.local.weatherstudy.persistence.database.models.StudyWidgetComponentEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetEntity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the widget `@Relation` POJO behind
 * com.samsung.android.weather.persistence.database.dao.WidgetRoomDao
 *
 * Observed responsibility: a widget row plus its ordered component rows. The split
 * exists because the widget editor lets the user reorder blocks inside one widget, and
 * an ordered child table is how that is persisted.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyWidgetWithComponents(
    @Embedded val widget: StudyWidgetEntity,

    @Relation(parentColumn = "COL_WIDGET_ID", entityColumn = "COL_WIDGET_ID")
    val components: List<StudyWidgetComponentEntity> = emptyList(),
)
