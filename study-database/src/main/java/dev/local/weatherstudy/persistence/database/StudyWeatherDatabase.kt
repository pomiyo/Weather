package dev.local.weatherstudy.persistence.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.local.weatherstudy.persistence.database.dao.StudyAwayModeLocationsDao
import dev.local.weatherstudy.persistence.database.dao.StudyCorpAppDao
import dev.local.weatherstudy.persistence.database.dao.StudyCursorRoomDao
import dev.local.weatherstudy.persistence.database.dao.StudyInsightContentDao
import dev.local.weatherstudy.persistence.database.dao.StudyLifeStyleSettingsDao
import dev.local.weatherstudy.persistence.database.dao.StudyRemoteConfigDao
import dev.local.weatherstudy.persistence.database.dao.StudySettingsRoomDao
import dev.local.weatherstudy.persistence.database.dao.StudyStatusDao
import dev.local.weatherstudy.persistence.database.dao.StudyWeatherRoomDao
import dev.local.weatherstudy.persistence.database.dao.StudyWidgetRoomDao
import dev.local.weatherstudy.persistence.database.migration.StudyAutoMigration1627to1628
import dev.local.weatherstudy.persistence.database.migration.StudyAutoMigration1628to1629
import dev.local.weatherstudy.persistence.database.models.StudyAlertEntity
import dev.local.weatherstudy.persistence.database.models.StudyAwayModeLocationsEntity
import dev.local.weatherstudy.persistence.database.models.StudyContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyCorpAppEntity
import dev.local.weatherstudy.persistence.database.models.StudyDailyEntity
import dev.local.weatherstudy.persistence.database.models.StudyForecastChangeEntity
import dev.local.weatherstudy.persistence.database.models.StudyHourlyEntity
import dev.local.weatherstudy.persistence.database.models.StudyIndexEntity
import dev.local.weatherstudy.persistence.database.models.StudyInsightContentEntity
import dev.local.weatherstudy.persistence.database.models.StudyLifeStyleEntity
import dev.local.weatherstudy.persistence.database.models.StudyLifeStyleSettingsEntity
import dev.local.weatherstudy.persistence.database.models.StudyRemoteConfigEntity
import dev.local.weatherstudy.persistence.database.models.StudySettingEntity
import dev.local.weatherstudy.persistence.database.models.StudyStatusEntity
import dev.local.weatherstudy.persistence.database.models.StudyWeatherEntity
import dev.local.weatherstudy.persistence.database.models.StudyWebMenuEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetComponentEntity
import dev.local.weatherstudy.persistence.database.models.StudyWidgetEntity

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.database.WeatherDatabase
 *
 * Observed responsibilities:
 * - 18 entities, **10 abstract DAO accessors**, schema version **1629**
 * - the original exposes exactly these ten: `weatherDao`, `settingsDao`, `widgetDao`,
 *   `cursorDao`, `remoteConfigDao`, `statusDao`, `insightContentDao`,
 *   `awayModeLocationsDao`, `lifeStyleSettingsDao`, `corpAppDao`
 * - it lives in its **own Gradle module, versioned independently** of the app:
 *   the APK's `kotlin_module` for it is `weather-database-1629-458aeea`, i.e. the
 *   schema version and a commit hash, while every other module is `1.7.3.10`. The
 *   database is released on its own cadence.
 * - 51 exported schema versions ship in the APK's assets, from **v900 to v1629**, and
 *   27 of the later steps are declared as Room `@AutoMigration`s with spec classes.
 *
 * ### What the reconstruction does with 51 versions
 *
 * Per the project's STEP 10, the migration *architecture* is preserved and the
 * individual column additions are summarised rather than replayed: this database starts
 * at 1629 with two representative `@AutoMigration` specs wired up, and
 * `reports/room-schema-analysis.md` carries the full version table and the shape of
 * each historical step. Replaying 51 versions would add volume, not understanding —
 * and the pre-1502 steps were hand-written `Migration` subclasses whose SQL is not
 * recoverable from the exported schemas alone.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Database(
    entities = [
        StudyWeatherEntity::class,
        StudyHourlyEntity::class,
        StudyDailyEntity::class,
        StudyIndexEntity::class,
        StudyWebMenuEntity::class,
        StudyAlertEntity::class,
        StudyContentEntity::class,
        StudyForecastChangeEntity::class,
        StudyInsightContentEntity::class,
        StudyLifeStyleEntity::class,
        StudyLifeStyleSettingsEntity::class,
        StudySettingEntity::class,
        StudyWidgetEntity::class,
        StudyWidgetComponentEntity::class,
        StudyStatusEntity::class,
        StudyRemoteConfigEntity::class,
        StudyAwayModeLocationsEntity::class,
        StudyCorpAppEntity::class,
    ],
    version = StudyDbConstants.DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1627, to = 1628, spec = StudyAutoMigration1627to1628::class),
        AutoMigration(from = 1628, to = 1629, spec = StudyAutoMigration1628to1629::class),
    ],
)
@TypeConverters(StudyRoomConverters::class)
abstract class StudyWeatherDatabase : RoomDatabase() {

    abstract fun weatherDao(): StudyWeatherRoomDao
    abstract fun settingsDao(): StudySettingsRoomDao
    abstract fun widgetDao(): StudyWidgetRoomDao
    abstract fun cursorDao(): StudyCursorRoomDao
    abstract fun remoteConfigDao(): StudyRemoteConfigDao
    abstract fun statusDao(): StudyStatusDao
    abstract fun insightContentDao(): StudyInsightContentDao
    abstract fun awayModeLocationsDao(): StudyAwayModeLocationsDao
    abstract fun lifeStyleSettingsDao(): StudyLifeStyleSettingsDao
    abstract fun corpAppDao(): StudyCorpAppDao
}
