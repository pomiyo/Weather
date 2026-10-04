package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.RemoteConfigEntity
 *
 * Table `TABLE_REMOTE_CONFIG_INFO`, 2 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: a version-keyed blob of remote configuration, stored
 * opaquely as TEXT.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_REMOTE_CONFIG_INFO,
    primaryKeys = [StudyDbConstants.COL_VERSION],
)
data class StudyRemoteConfigEntity(
    @ColumnInfo(name = StudyDbConstants.COL_VERSION)
    val version: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONFIG_INFO)
    val config: String = "",
)
