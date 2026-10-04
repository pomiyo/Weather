package dev.local.weatherstudy.backend

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.local.weatherstudy.backend.dao.StudyBackendRoomDao
import dev.local.weatherstudy.backend.entity.StudyBackendEntity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.BackendDatabase
 *
 * ### What the original is, and the hard boundary around it
 *
 * The original is a Room wrapper over a **database that ships inside the APK**:
 * `assets/database/backend_ver8.db`. It holds, per backend, the base domain, the
 * legal links, the partner code — and **the API credentials**. That is why
 * `BackendDao` is a plain key/value interface (`getValue(key)` / `setValue(key, value)`)
 * rather than a typed one: the table is a bag of named secrets and settings.
 *
 * This project read **table definitions only** from that file. No row contents were
 * recorded, none are reproduced here, and `reports/` contains no value from it. The
 * reconstruction ships **no pre-populated database**: `StudyBackendDatabase` starts
 * empty, so every credential read returns empty and every domain read falls back to
 * the local fixture URL.
 *
 * The *shape* is what is educational: a data-driven backend registry, consulted at
 * runtime, is the reason one APK serves five regional backends — and the reason an
 * independent rebuild cannot use them.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Database(
    entities = [StudyBackendEntity::class],
    version = BACKEND_DATABASE_VERSION,
    exportSchema = false,
)
abstract class StudyBackendDatabase : RoomDatabase() {
    abstract fun backendDao(): StudyBackendRoomDao
}

/**
 * The original's bundled file is `backend_ver8.db`, i.e. version 8. The number is kept
 * so the lineage is readable; nothing is imported from that file.
 */
const val BACKEND_DATABASE_VERSION = 8

/** The original reads a pre-packaged asset. The reconstruction creates an empty database. */
const val BACKEND_DATABASE_NAME = "backend_study.db"
