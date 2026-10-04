package dev.local.weatherstudy.interworking

import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction of `weather-interworking` —
 * com.samsung.android.weather.interworking.{rubin,smartthings,news,store,launcher}
 *
 * Four bridges to Samsung platform services. All four are documented stubs: each names the
 * service it needs and reports unavailable, so the policy layer disables the features and the
 * call sites stay visible. Per-method detail in reports/samsung-bridge-map.md §4.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
class StudySamsungServiceUnavailable(service: String, reason: String) :
    UnsupportedOperationException("$service is unavailable: $reason")

/**
 * `interworking.rubin.*` — Samsung Rubin, on-device personalisation.
 *
 * The APK bundles `com.samsung.android.rubin.sdk` (778 classes) and 25 `Runestone*Api`
 * classes. Rubin infers "most probable activity" — commuting, at home, exercising — which the
 * original stores in `COL_SETTING_PINNED_LOCATION` (a repurposed column) and uses to decide
 * whether to refresh on the move.
 */
interface StudyRubinDataSource {
    suspend fun getMostProbableActivity(): Int
    suspend fun isRubinEnabled(): Boolean
}

@Singleton
class StudyRubinBridge @Inject constructor() : StudyRubinDataSource {
    override suspend fun getMostProbableActivity(): Int = ACTIVITY_UNKNOWN
    override suspend fun isRubinEnabled(): Boolean = false

    companion object { const val ACTIVITY_UNKNOWN = 0 }
}

/**
 * `interworking.smartthings.*` — the SmartThings detail card.
 *
 * Bundles `com.samsung.android.sdk.stkit` (188 classes). Returns the user's rooms and a
 * summary per room, which the SmartThings card renders. `MockSmartThingsDataSource` exists in
 * the original behind DevOpts — that mock is the working path here.
 */
data class StudyRoomDto(val id: String, val name: String, val deviceCount: Int = 0)
data class StudySummaryDto(val roomId: String, val text: String)

interface StudySmartThingsDataSource {
    suspend fun getRooms(): List<StudyRoomDto>
    suspend fun getSummary(roomId: String): StudySummaryDto?
    suspend fun isAvailable(): Boolean
}

/** the bridge: requires the SmartThings platform app. */
@Singleton
class StudySmartThingsBridge @Inject constructor() : StudySmartThingsDataSource {
    override suspend fun getRooms(): List<StudyRoomDto> =
        throw StudySamsungServiceUnavailable("SmartThings (sdk.stkit)", "needs the SmartThings app")
    override suspend fun getSummary(roomId: String): StudySummaryDto? =
        throw StudySamsungServiceUnavailable("SmartThings (sdk.stkit)", "needs the SmartThings app")
    override suspend fun isAvailable(): Boolean = false
}

/** `interworking.smartthings.source.impl.MockSmartThingsDataSource` — the DevOpts path. */
@Singleton
class StudyMockSmartThingsDataSource @Inject constructor() : StudySmartThingsDataSource {
    override suspend fun getRooms() = listOf(
        StudyRoomDto("living", "Living room", 3),
        StudyRoomDto("bedroom", "Bedroom", 2),
    )
    override suspend fun getSummary(roomId: String) =
        StudySummaryDto(roomId, "2 devices on · 22°")
    override suspend fun isAvailable() = true
}

/** `interworking.smartthings.usecase.GetSmartThings` — injected into DetailViewModel. */
class StudyGetSmartThings @Inject constructor(
    private val dataSource: StudySmartThingsDataSource,
) : dev.local.weatherstudy.domain.usecase.StudySingleUsecase<List<StudyRoomDto>> {
    override suspend fun invoke(): List<StudyRoomDto> =
        if (!dataSource.isAvailable()) emptyList()
        else runCatching { dataSource.getRooms() }.getOrDefault(emptyList())
}

/**
 * `interworking.news.*` — Samsung News.
 *
 * Needs the Samsung News package; `NewsPackageReceiver` watches for it being installed or
 * removed. `NewsBitmapImageWorker` prefetches thumbnails because RemoteViews cannot load a
 * URL — see StudyRemoteImageView.
 */
interface StudyNewsRepo {
    suspend fun getLocalWeatherNews(locationKey: String): List<StudyWebContent>
    suspend fun isNewsAvailable(): Boolean
}

@Singleton
class StudyNewsBridge @Inject constructor() : StudyNewsRepo {
    override suspend fun getLocalWeatherNews(locationKey: String): List<StudyWebContent> = emptyList()
    override suspend fun isNewsAvailable(): Boolean = false
}

/**
 * `interworking.store.*` — Galaxy Store update check.
 *
 * `AppUpdateCondition` consults this during startup; a `FORCED_UPDATE` result blocks the app.
 * With the bridge reporting nothing available, the condition always passes.
 */
data class StudyAppUpdateInfo(val result: Int, val versionCode: Int = 0)

interface StudyAppStoreRepo {
    suspend fun checkUpdate(): StudyAppUpdateInfo
}

@Singleton
class StudyGalaxyStoreBridge @Inject constructor() : StudyAppStoreRepo {
    override suspend fun checkUpdate() = StudyAppUpdateInfo(
        dev.local.weatherstudy.domain.type.StudyAppUpdateResult.NOT_SUPPORT,
    )
}

/** `interworking.launcher.*` — the Samsung launcher widget/shortcut handshake. */
@Singleton
class StudyLauncherBridge @Inject constructor() {
    fun isSamsungLauncher(): Boolean = false
    fun supportsWidgetPin(): Boolean = false
}
