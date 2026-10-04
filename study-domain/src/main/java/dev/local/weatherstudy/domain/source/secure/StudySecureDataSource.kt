package dev.local.weatherstudy.domain.source.secure

import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.secure.SecureDataSource
 *
 * Observed responsibility: a string key/value store the original uses for values it
 * wants kept out of the plain settings table. Reconstructed as the same three-method
 * contract; the reconstruction's implementation is an ordinary in-memory map, since
 * there is nothing secret to protect here.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudySecureDataSource {
    suspend fun getValue(key: String): String
    suspend fun setValue(key: String, value: String)
    suspend fun observeValue(key: String): Flow<String>
}
