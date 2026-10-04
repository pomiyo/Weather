package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.LifeStyleSettingsEntity
 *
 * Table `TABLE_LIFESTYLE_SETTINGS_INFO`, 2 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: which life-style rows the USER has enabled. Two columns,
 * no foreign key, no weather key — this is preference, not data, and it survives
 * every location being deleted.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_LIFESTYLE_SETTINGS_INFO,
    primaryKeys = [StudyDbConstants.COL_LIFESTYLE_SETTINGS_TYPE],
)
data class StudyLifeStyleSettingsEntity(
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_SETTINGS_TYPE)
    val type: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_LIFESTYLE_SETTINGS_ALLOWED)
    val allowed: Int = 0,
)
