package dev.local.weatherstudy.persistence.dao

import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction (stub — role-preserving, per the project's STEP 22).
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.dao.WeatherDbDao
 * com.samsung.android.weather.persistence.database.dao.SettingsDbDao
 * com.samsung.android.weather.persistence.database.dao.WidgetDbDao
 * com.samsung.android.weather.persistence.database.CursorDbDao
 *
 * ### What this tier is, and why it is a stub rather than deleted
 *
 * Samsung Weather predates Room. The `*DbDao` family is the **legacy raw-SQLite path**,
 * reaching the database through `SQLiteOpenHelper` and hand-written SQL rather than
 * through Room. The decompiled `CursorDbDao` carries a `writableDatabase` lazy
 * property, which is the giveaway.
 *
 * It is still in the shipping APK because:
 * - the pre-1502 schema versions were migrated by hand-written SQL, and this is the
 *   path that ran them
 * - a restore from a very old backup can land data that Room's auto-migrations cannot
 *   reach without it
 *
 * Reconstructing the SQL would mean reconstructing a schema history that the exported
 * Room schemas do not record (see `StudySchemaVersionLadder.FIRST_AUTO_MIGRATED_VERSION`).
 * So the tier is **kept as a class, with its role documented, and its methods throw** —
 * exactly the "stub rather than collapse" rule. Deleting it would hide a real third of
 * the DAO family and make the three-tier design look like a two-tier one.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyWeatherDbDao @Inject constructor() : StudyWeatherDao {

    override suspend fun getWeather(key: String): StudyWeather? = throw legacy()
    override suspend fun getWeathers(): List<StudyWeather> = throw legacy()
    override fun observeWeathers(): Flow<List<StudyWeather>> = throw legacy()
    override suspend fun getCount(key: String): Int = throw legacy()
    override suspend fun isExist(key: String): Boolean = throw legacy()
    override suspend fun saveWeather(weather: StudyWeather): Long = throw legacy()
    override suspend fun saveWeathers(weathers: List<StudyWeather>): List<Long> = throw legacy()
    override suspend fun updateWeather(weather: StudyWeather): Int = throw legacy()
    override suspend fun updateWeathers(weathers: List<StudyWeather>): Int = throw legacy()
    override suspend fun replaceWeathers(weathers: List<StudyWeather>): Int = throw legacy()
    override suspend fun deleteWeather(key: String): Int = throw legacy()
    override suspend fun deleteWeathersByKey(keys: List<String>): Int = throw legacy()
    override suspend fun deleteAll(): Int = throw legacy()
    override suspend fun updateOrder(keys: List<String>): Int = throw legacy()
    override suspend fun updateLabel(key: String, label: String, labelType: String): Int = throw legacy()
    override suspend fun addAwayLocation(location: StudyAwayModeLocation): Unit = throw legacy()
    override suspend fun getAwayLocationByAwayKey(key: String): StudyAwayModeLocation? = throw legacy()
    override suspend fun getAwayLocationByHomeKey(key: String): StudyAwayModeLocation? = throw legacy()
    override suspend fun clearAwayLocations(): Unit = throw legacy()

    private fun legacy() = StudyLegacyPathUnavailable("WeatherDbDao")
}

/**
 * Marks a call into the legacy raw-SQLite tier.
 *
 * Not a type in the original — it exists so the stubs can fail with an explanation
 * rather than silently returning empty data, which would look like data loss.
 */
class StudyLegacyPathUnavailable(dao: String) : UnsupportedOperationException(
    "$dao is the original's legacy raw-SQLite tier. Its hand-written SQL covers schema " +
        "versions below ${
            dev.local.weatherstudy.persistence.database.migration
                .StudySchemaVersionLadder.FIRST_AUTO_MIGRATED_VERSION
        }, which the APK's exported Room schemas do not record, so it is not " +
        "reconstructed. The Room tier serves all reads and writes here. See " +
        "reports/room-schema-analysis.md.",
)
