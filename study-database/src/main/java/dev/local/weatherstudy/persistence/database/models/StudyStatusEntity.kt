package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.StatusEntity
 *
 * Table `TABLE_STATUS_INFO`, 3 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: the three-column shared operation status
 * (`StudyStatusRepo`). `COL_STATUS_FROM` records which surface started it, so the
 * refresh a widget began is visible to the detail screen's spinner.
 * The field is named `from` in the original — a Kotlin soft keyword, kept.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_STATUS_INFO,
    primaryKeys = [StudyDbConstants.COL_STATUS_ID],
)
data class StudyStatusEntity(
    @ColumnInfo(name = StudyDbConstants.COL_STATUS_ID)
    val id: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_STATUS_CODE)
    val status: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_STATUS_FROM)
    val `from`: Int = 0,
)
