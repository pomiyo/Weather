package dev.local.weatherstudy.backend.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.local.weatherstudy.backend.entity.StudyBackendEntity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.dao.BackendDao
 *
 * Observed responsibility: exactly two methods — `getValue(key)` and
 * `setValue(key, value)`. The registry is untyped on purpose; see
 * [dev.local.weatherstudy.backend.StudyBackendDatabase].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyBackendDao {
    suspend fun getValue(key: String): String
    suspend fun setValue(key: String, value: String)
}

/** The Room-backed tier, mirroring the pattern in `:study-persistence`. */
@Dao
interface StudyBackendRoomDao {
    @Query("SELECT backend_value FROM backend_registry WHERE backend_key = :key")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun upsert(entity: StudyBackendEntity)
}
