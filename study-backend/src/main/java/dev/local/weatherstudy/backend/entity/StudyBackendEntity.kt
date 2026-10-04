package dev.local.weatherstudy.backend.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.entity.BackendEntity
 *
 * Observed responsibility: one named value in the backend registry. The original's
 * table is a flat key/value store — which is why [dev.local.weatherstudy.backend.dao.StudyBackendDao]
 * has exactly two methods, and why the typed accessors live one layer up in
 * `StudySecureKeyProvider` / `StudySecureLinkProvider` rather than here.
 *
 * Column names are the reconstruction's own: the original table's definition was the
 * only thing read from the bundled database, and reproducing even its column names is
 * unnecessary for the architecture.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Entity(tableName = "backend_registry", primaryKeys = ["backend_key"])
data class StudyBackendEntity(
    @ColumnInfo(name = "backend_key") val key: String,
    @ColumnInfo(name = "backend_value") val value: String = "",
)
