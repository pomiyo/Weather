package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.AlertEntity
 *
 * Table `TABLE_ALERT_INFO`, 8 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: severe-weather advisories, composite key
 * (weather key + provider detail key). Note `COL_ALERT_ISSUE_TIME` is TEXT while
 * `COL_ALERT_EXPIRE_TIME` is INTEGER — the provider sends the issue time as a
 * formatted string with its own timezone, kept verbatim for display.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_ALERT_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_ALERT_DETAIL_KEY],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyAlertEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_DETAIL_KEY)
    val detailKey: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_DESCRIPTION)
    val description: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_SEVERITY_CODE)
    val severityCode: Int? = null,
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_EXPIRE_TIME)
    val expireTime: Long? = null,
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_ISSUE_TIME)
    val issueTime: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_ISSUE_TIMEZONE)
    val issueTimeZone: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_ALERT_LINK_URL)
    val linkURL: String? = null,
)
