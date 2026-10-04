package dev.local.weatherstudy.domain.source.local

import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.local.WeatherLocalQueryDataSource
 * com.samsung.android.weather.domain.source.local.WeatherLocalCommandDataSource
 * com.samsung.android.weather.domain.source.local.WeatherLocalObserveDataSource
 * com.samsung.android.weather.domain.source.local.AwayModeLocalDataSource
 * com.samsung.android.weather.domain.source.local.WeatherLocalDataSource
 *
 * Observed responsibility — and the reason there are four interfaces, not one:
 * the original splits local access into **query / command / observe**, then unions
 * them. Every consumer depends only on the slice it needs: the content providers
 * take the query half, `PersistenceWorker` takes the command half, and the
 * ViewModels take the observe half. That is also how `WeatherRepo` is defined —
 * it declares no methods of its own and simply unions these contracts with the
 * remote one.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyWeatherLocalQueryDataSource {
    suspend fun getLocalWeather(key: String): StudyWeather?
    suspend fun getLocalWeathers(): List<StudyWeather>
    suspend fun getCount(key: String): Int
    suspend fun isExist(key: String): Boolean
}

/** Corresponds conceptually to `…local.WeatherLocalCommandDataSource`. */
interface StudyWeatherLocalCommandDataSource {
    suspend fun saveWeather(weather: StudyWeather): Long
    suspend fun saveWeathers(weathers: List<StudyWeather>): List<Long>
    suspend fun updateWeather(weather: StudyWeather): Int
    suspend fun updateWeathers(weathers: List<StudyWeather>): Int
    suspend fun replaceWeathers(weathers: List<StudyWeather>): Int
    suspend fun deleteWeather(key: String): Int
    suspend fun deleteWeathers(weathers: List<StudyWeather>): Int
    suspend fun deleteWeathersByKey(keys: List<String>): Int
    suspend fun deleteAll(): Int
    suspend fun updateOrder(keys: List<String>): Int
    suspend fun updateLabel(key: String, label: String, labelType: String): Int
}

/** Corresponds conceptually to `…local.WeatherLocalObserveDataSource`. */
interface StudyWeatherLocalObserveDataSource {
    fun observeWeathers(): Flow<List<StudyWeather>>
}

/**
 * Corresponds conceptually to `…local.AwayModeLocalDataSource`.
 *
 * Observed responsibility: the geofence "home vs away" bookkeeping. Note the lookup
 * is available by BOTH keys — `HomeToAwayModeWorker` needs away→home, and the
 * detail screen needs home→away.
 */
interface StudyAwayModeLocalDataSource {
    suspend fun addAwayLocationKey(awayModeLocation: StudyAwayModeLocation)
    suspend fun getAwayModeLocationByAwayKey(key: String): StudyAwayModeLocation?
    suspend fun getAwayModeLocationByHomeKey(key: String): StudyAwayModeLocation?
    suspend fun clearAwayModeLocations()
}

/** Corresponds conceptually to `…local.WeatherLocalDataSource` — the union. */
interface StudyWeatherLocalDataSource :
    StudyWeatherLocalQueryDataSource,
    StudyWeatherLocalCommandDataSource,
    StudyWeatherLocalObserveDataSource,
    StudyAwayModeLocalDataSource
