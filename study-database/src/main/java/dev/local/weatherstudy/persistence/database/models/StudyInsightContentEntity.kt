package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.InsightContentEntity
 *
 * Table `TABLE_INSIGHT_CONTENT_INFO`, 14 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibilities:
 * - insight cards, composite key (weather key + order)
 * - the three `SHOW_*` flags persist which surfaces may display this insight
 * - **`COL_INSIGHT_SERIALIZED_JSON`** is where the typed `AdditionalInfo` payload
 *   goes. The domain models it as a 16-variant sealed hierarchy; Room stores it as
 *   one JSON string. That is the deliberate escape hatch: adding an insight kind
 *   needs no migration.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_INSIGHT_CONTENT_INFO,
    primaryKeys = [StudyDbConstants.COL_WEATHER_KEY, StudyDbConstants.COL_INSIGHT_ORDER],
    foreignKeys = [
        ForeignKey(
            entity = StudyWeatherEntity::class,
            parentColumns = [StudyDbConstants.COL_WEATHER_KEY],
            childColumns = [StudyDbConstants.COL_WEATHER_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudyInsightContentEntity(
    @ColumnInfo(name = StudyDbConstants.COL_WEATHER_KEY)
    val key: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_TYPE)
    val insightType: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_ORDER)
    val order: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SHOW_NOTIFICATION)
    val showNotification: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SHOW_WIDGET)
    val showWidget: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SHOW_DETAIL)
    val showDetail: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_SHOW_DEFAULT)
    val showDefault: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_TITLE)
    val title: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_TEXT)
    val text: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_SHORT_TEXT)
    val shortText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_DEFAULT_TEXT)
    val defaultText: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_URL)
    val url: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_TIME_DESCRIPTION)
    val timeDescription: String? = null,
    @ColumnInfo(name = StudyDbConstants.COL_INSIGHT_SERIALIZED_JSON)
    val serializedJson: String? = null,
)
