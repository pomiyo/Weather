package dev.local.weatherstudy.interworking.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.interworking.*
import javax.inject.Singleton

/**
 * Educational reconstruction of `interworking.di.{InterWorkingModule, RecognitionModule,
 * SmartThingsModule}` and `store.di.AppStoreModule`.
 *
 * SmartThings binds to the **mock**, not the bridge — the original ships that mock behind
 * DevOpts, and it is the path that works off a Samsung device. Repoint it at
 * `StudySmartThingsBridge` to see the real dependency throw.
 */
@Module
@InstallIn(SingletonComponent::class)
interface StudyInterworkingModule {
    @Binds @Singleton fun bindRubin(i: StudyRubinBridge): StudyRubinDataSource
    @Binds @Singleton fun bindSmartThings(i: StudyMockSmartThingsDataSource): StudySmartThingsDataSource
    @Binds @Singleton fun bindNews(i: StudyNewsBridge): StudyNewsRepo
    @Binds @Singleton fun bindAppStore(i: StudyGalaxyStoreBridge): StudyAppStoreRepo
}
