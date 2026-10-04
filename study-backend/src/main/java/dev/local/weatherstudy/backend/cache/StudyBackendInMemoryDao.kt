package dev.local.weatherstudy.backend.cache

import dev.local.weatherstudy.backend.dao.StudyBackendDao
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.cache.BackendInMemoryDao
 *
 * Observed responsibility: the in-memory tier of the backend registry — the same
 * three-tier pattern as the weather DAOs. It matters more here than elsewhere: an
 * interceptor asks for a credential on **every outbound request**, so a disk read per
 * request would be unacceptable.
 *
 * In the reconstruction the cache is simply empty, which is why every credential read
 * returns `""` and the auth interceptor attaches nothing.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyBackendInMemoryDao @Inject constructor() : StudyBackendDao {

    private val cache = ConcurrentHashMap<String, String>()

    override suspend fun getValue(key: String): String = cache[key].orEmpty()

    override suspend fun setValue(key: String, value: String) {
        cache[key] = value
    }
}
