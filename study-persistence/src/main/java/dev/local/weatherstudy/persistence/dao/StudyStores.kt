package dev.local.weatherstudy.persistence.dao

import dev.local.weatherstudy.domain.entity.content.StudyLifeStyleSettings
import dev.local.weatherstudy.domain.entity.settings.StudyCorpAppInfo
import dev.local.weatherstudy.persistence.database.dao.StudyCorpAppDao
import dev.local.weatherstudy.persistence.database.dao.StudyCursorRoomDao
import dev.local.weatherstudy.persistence.database.dao.StudyLifeStyleSettingsDao
import dev.local.weatherstudy.persistence.database.models.StudyCorpAppEntity
import dev.local.weatherstudy.persistence.database.models.StudyLifeStyleSettingsEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the Room-backed adapters for the leaf DAOs —
 * `LifeStyleSettingsDao`, `CorpAppDao`, `CursorRoomDao` — which, unlike the weather,
 * settings and widget families, have no three-tier split: there is nothing to cache and
 * no legacy path, so one implementation each.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyLifeStyleSettingsRoomStore @Inject constructor(
    private val dao: StudyLifeStyleSettingsDao,
) : StudyLifeStyleSettingsStore {

    override suspend fun getSettings(): List<StudyLifeStyleSettings> =
        dao.getAll().map { StudyLifeStyleSettings(type = it.type, enabled = it.allowed != 0) }

    override suspend fun setSettings(settings: List<StudyLifeStyleSettings>) =
        dao.upsertAll(
            settings.map {
                StudyLifeStyleSettingsEntity(type = it.type, allowed = if (it.enabled) 1 else 0)
            },
        )
}

/** Corresponds conceptually to `…database.dao.CorpAppDao`'s domain-facing adapter. */
@Singleton
class StudyCorpAppRoomStore @Inject constructor(
    private val dao: StudyCorpAppDao,
) : StudyCorpAppStore {

    override suspend fun get(packageName: String): StudyCorpAppInfo? =
        dao.get(packageName)?.toDomain()

    override fun observeAll(): Flow<List<StudyCorpAppInfo>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(info: StudyCorpAppInfo) = dao.insert(
        StudyCorpAppEntity(
            packageName = info.packageName,
            name = info.name,
            key = info.key.toIntOrNull() ?: 0,
            certificate = info.certificate,
        ),
    )

    override suspend fun delete(packageName: String) = dao.delete(packageName)

    override suspend fun deleteAll() = dao.deleteAll()

    private fun StudyCorpAppEntity.toDomain() = StudyCorpAppInfo(
        packageName = packageName,
        name = name,
        key = key.toString(),
        certificate = certificate,
    )
}

/**
 * Corresponds conceptually to `…persistence.CursorDao`'s Room-backed implementation.
 *
 * Observed responsibility: hand SQLite's own cursor straight out, unwrapped. This is the
 * only place in the app where a database type crosses a layer boundary, and it is
 * deliberate — the exported content providers must return a `Cursor`.
 */
@Singleton
class StudyCursorRoomStore @Inject constructor(
    private val dao: StudyCursorRoomDao,
) : StudyCursorDao {
    override fun getAll() = dao.getAllCursor()
    override fun getByKey(key: String) = dao.getByKeyCursor(key)
    override fun getHourly() = dao.getHourlyCursor()
    override fun getHourly(key: String) = dao.getHourlyCursor(key)
    override fun getDaily() = dao.getDailyCursor()
    override fun getDaily(key: String) = dao.getDailyCursor(key)
    override fun getIndex() = dao.getIndexCursor()
    override fun getIndex(key: String) = dao.getIndexCursor(key)
    override fun getSettings() = dao.getSettingsCursor()
}
