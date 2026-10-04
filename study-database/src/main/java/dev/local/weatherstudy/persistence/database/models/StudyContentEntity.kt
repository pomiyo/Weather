package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.ContentEntity
 *
 * Table `TABLE_CONTENT_INFO`, 11 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: one table for radar, videos, today-stories and news,
 * discriminated by `COL_CONTENT_TYPE`. Primary key is (weather key + title + link
 * url) even though `COL_CONTENT_ID` exists — the id was added later (note its `''`
 * default) and the key was never migrated.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_CONTENT_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_CONTENT_TITLE, StudyDbConstants.COL_CONTENT_LINK_URL],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyContentEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_ID)
    val id: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_TYPE)
    val type: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_TITLE)
    val title: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_DESC)
    val summary: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_NARRATIVE)
    val narrative: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_THUMBNAIL)
    val thumbnail: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_LINK_URL)
    val linkUrl: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_MORE_URL)
    val moreUrl: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_EXPIRE_TIME)
    val expiredTime: Long = 0L,
    @ColumnInfo(name = StudyDbConstants.COL_CONTENT_ORDER)
    val order: Int = 0,
)
