package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.AwayModeLocationsEntity
 *
 * Table `TABLE_AWAY_MODE_LOCATIONS_INFO`, 4 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: the home/away pairing for the geofence feature. It stores
 * the PROVIDER for each side as well as the key, because home and away can be served
 * by different regional backends when the user travels.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_AWAY_MODE_LOCATIONS_INFO,
    primaryKeys = [StudyDbConstants.COL_AWAY_LOCATION],
)
data class StudyAwayModeLocationsEntity(
    @ColumnInfo(name = StudyDbConstants.COL_AWAY_LOCATION)
    val awayKey: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_HOME_LOCATION)
    val homeKey: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_AWAY_PROVIDER)
    val awayProvider: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_HOME_PROVIDER)
    val homeProvider: String = "",
)
