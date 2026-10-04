package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.WebMenuEntity
 *
 * Table `TABLE_WEB_MENU_INFO`, 6 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: provider-supplied bottom-of-screen links. The primary key
 * is (weather key + title + url) — the app does not assign ids, so the provider
 * decides how many rows exist and duplicates collapse naturally.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_WEB_MENU_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_WEB_MENU_TITLE, StudyDbConstants.COL_WEB_MENU_URL],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyWebMenuEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_WEB_MENU_TYPE)
    val type: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_WEB_MENU_TITLE)
    val title: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_WEB_MENU_IMAGE)
    val image: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_WEB_MENU_URL)
    val url: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_WEB_MENU_UPDATE_TIME)
    val updateTime: Long? = null,
)
