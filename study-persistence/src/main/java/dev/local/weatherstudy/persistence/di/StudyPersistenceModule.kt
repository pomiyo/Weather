package dev.local.weatherstudy.persistence.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.persistence.dao.StudyCorpAppRoomStore
import dev.local.weatherstudy.persistence.dao.StudyCorpAppStore
import dev.local.weatherstudy.persistence.dao.StudyCursorDao
import dev.local.weatherstudy.persistence.dao.StudyCursorRoomStore
import dev.local.weatherstudy.persistence.dao.StudyLifeStyleSettingsRoomStore
import dev.local.weatherstudy.persistence.dao.StudyLifeStyleSettingsStore
import dev.local.weatherstudy.persistence.dao.StudySettingsDao
import dev.local.weatherstudy.persistence.dao.StudySettingsRoomBackedDao
import dev.local.weatherstudy.persistence.dao.StudyWeatherDao
import dev.local.weatherstudy.persistence.dao.StudyWeatherRoomBackedDao
import dev.local.weatherstudy.persistence.dao.StudyWidgetDao
import dev.local.weatherstudy.persistence.dao.StudyWidgetRoomBackedDao
import dev.local.weatherstudy.persistence.database.StudyDbConstants
import dev.local.weatherstudy.persistence.database.StudyWeatherDatabase
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.persistence.di.PersistenceModule
 *
 * ### The one-line decision at the centre of the DAO family
 *
 * [StudyPersistenceBindingModule] is where the three-tier choice is resolved. Swap
 * `StudyWeatherRoomBackedDao` for `StudyWeatherInMemoryDao` on that `@Binds` and the
 * entire app runs off the cache — nothing else changes, because every layer above
 * depends on the `StudyWeatherDao` interface. That substitutability is the reason the
 * tier exists, and it is worth trying in Android Studio to see it work.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudyPersistenceModule {

    @Provides
    @Singleton
    fun provideWeatherDatabase(@ApplicationContext context: Context): StudyWeatherDatabase =
        Room.databaseBuilder(
            context,
            StudyWeatherDatabase::class.java,
            StudyDbConstants.DATABASE_NAME,
        ).build()

    @Provides
    @Singleton
    fun provideWeatherRoomDao(db: StudyWeatherDatabase) = db.weatherDao()

    @Provides
    @Singleton
    fun provideSettingsRoomDao(db: StudyWeatherDatabase) = db.settingsDao()

    @Provides
    @Singleton
    fun provideWidgetRoomDao(db: StudyWeatherDatabase) = db.widgetDao()

    @Provides
    @Singleton
    fun provideCursorRoomDao(db: StudyWeatherDatabase) = db.cursorDao()

    @Provides
    @Singleton
    fun provideStatusDao(db: StudyWeatherDatabase) = db.statusDao()

    @Provides
    @Singleton
    fun provideInsightContentDao(db: StudyWeatherDatabase) = db.insightContentDao()

    @Provides
    @Singleton
    fun provideAwayModeLocationsDao(db: StudyWeatherDatabase) = db.awayModeLocationsDao()

    @Provides
    @Singleton
    fun provideLifeStyleSettingsDao(db: StudyWeatherDatabase) = db.lifeStyleSettingsDao()

    @Provides
    @Singleton
    fun provideRemoteConfigDao(db: StudyWeatherDatabase) = db.remoteConfigDao()

    @Provides
    @Singleton
    fun provideCorpAppDao(db: StudyWeatherDatabase) = db.corpAppDao()
}

/** The tier selection. See the note on [StudyPersistenceModule]. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyPersistenceBindingModule {

    @Binds
    @Singleton
    fun bindWeatherDao(impl: StudyWeatherRoomBackedDao): StudyWeatherDao

    @Binds
    @Singleton
    fun bindSettingsDao(impl: StudySettingsRoomBackedDao): StudySettingsDao

    @Binds
    @Singleton
    fun bindWidgetDao(impl: StudyWidgetRoomBackedDao): StudyWidgetDao

    @Binds
    @Singleton
    fun bindCursorDao(impl: StudyCursorRoomStore): StudyCursorDao

    @Binds
    @Singleton
    fun bindLifeStyleSettingsStore(impl: StudyLifeStyleSettingsRoomStore): StudyLifeStyleSettingsStore

    @Binds
    @Singleton
    fun bindCorpAppStore(impl: StudyCorpAppRoomStore): StudyCorpAppStore
}
