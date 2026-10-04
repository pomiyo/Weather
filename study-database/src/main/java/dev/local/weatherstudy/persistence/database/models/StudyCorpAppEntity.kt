package dev.local.weatherstudy.persistence.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.local.weatherstudy.persistence.database.StudyDbConstants

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.models.CorpAppEntity
 *
 * Table `TABLE_CORP_APP_INFO`, 4 columns, schema version 1629.
 * Column names, nullability, affinities and defaults are reproduced exactly from the
 * APK's exported Room schema, because they are the on-disk contract.
 *
 * Observed responsibility: the companion-app allow-list the exported content
 * providers check. `COL_KEY` is INTEGER and `COL_CERTIFICATE` is TEXT — the
 * certificate check is what `StudySignatureCheckContentProvider` cannot reproduce
 * off a platform-signed build.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(
    tableName = StudyDbConstants.TABLE_CORP_APP_INFO,
    primaryKeys = [StudyDbConstants.COL_PACKAGE_NAME],
)
data class StudyCorpAppEntity(
    @ColumnInfo(name = StudyDbConstants.COL_PACKAGE_NAME)
    val packageName: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_NAME)
    val name: String = "",
    @ColumnInfo(name = StudyDbConstants.COL_KEY)
    val key: Int = 0,
    @ColumnInfo(name = StudyDbConstants.COL_CERTIFICATE)
    val certificate: String = "",
)
