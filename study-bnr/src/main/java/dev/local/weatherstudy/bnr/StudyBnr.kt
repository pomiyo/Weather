package dev.local.weatherstudy.bnr

import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.widget.StudyWidgetInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction of `weather-bnr` —
 * com.samsung.android.weather.bnr.{data,converter,helper,usecase,constant}
 *
 * ### What backup/restore has to cope with
 *
 * The original's `bnr/data` package holds **52 classes**, and the reason is visible in their
 * names: alongside `BnrWeatherEntity` / `BnrWidgetEntity` / `BnrSettingEntity` there is a
 * parallel `BnrOldWeatherEntity` / `BnrOldWidgetEntity` set, plus `Old*` variants of nearly
 * every nested type (`OldAlert`, `OldCondition`, `OldCurrentObservation`, `OldLocation`,
 * `OldTime`, each with a `…P` "primitive" twin).
 *
 * That is a **two-generation backup format**. A restore can present data written by a much
 * older version of the app, so the converters read both shapes and normalise. It is the same
 * reason the legacy raw-SQLite DAO tier still ships.
 *
 * Transport is Samsung Cloud via `com.samsung.android.scloud.oem.lib.ClientProvider`, which is
 * not available here — so [StudySamsungCloudBnrBridge] is a documented stub and the rest of the
 * module (the format, the converters, the version negotiation) is reconstructed around it.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
object StudyBnrConstants {
    /** the current backup schema */
    const val VERSION_CURRENT = 2

    /** the pre-Room generation the `Old*` entities exist to read */
    const val VERSION_LEGACY = 1

    const val KEY_WEATHER = "weather"
    const val KEY_WIDGET = "widget"
    const val KEY_SETTINGS = "settings"
    const val KEY_LIFESTYLE = "lifestyle"

    const val RESULT_OK = 0
    const val RESULT_FAIL_FORMAT = 1
    const val RESULT_FAIL_TRANSPORT = 2
    const val RESULT_FAIL_VERSION = 3
}

/** `bnr.data.BnrWeatherEntity` — the current backup shape for one saved location. */
data class StudyBnrWeatherEntity(
    val key: String,
    val cityName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val order: Int = 0,
    val label: String = "",
    val labelType: Int = 0,
    val providerName: String = "",
)

/**
 * `bnr.data.BnrOldWeatherEntity` — the previous generation.
 *
 * Note what it lacks: no provider name (there was one backend), no label. A restore from this
 * shape has to synthesise both, which is what [StudyBnrWeatherConverter] does.
 */
data class StudyBnrOldWeatherEntity(
    val key: String,
    val name: String = "",
    val lat: String = "",
    val lon: String = "",
    val idx: Int = 0,
)

/** `bnr.data.BnrWidgetEntity`. */
data class StudyBnrWidgetEntity(
    val widgetId: Int,
    val weatherKey: String = "",
    val backgroundColor: Int = 0,
    val transparency: Float = 0.6f,
    val nightMode: Int = 0,
    val componentOrder: List<Int> = emptyList(),
)

/** `bnr.data.BnrSettingEntity`. */
data class StudyBnrSettingEntity(
    val tempScale: Int = 0,
    val autoRefreshInterval: Int = 3,
    val favoriteLocation: String = "",
    val showAlert: Int = 1,
)

/** `bnr.data.BnrLifeStyleEntity`. */
data class StudyBnrLifeStyleEntity(val type: Int, val allowed: Boolean)

/** the envelope the whole backup travels in. */
data class StudyBnrPayload(
    val version: Int = StudyBnrConstants.VERSION_CURRENT,
    val weathers: List<StudyBnrWeatherEntity> = emptyList(),
    val widgets: List<StudyBnrWidgetEntity> = emptyList(),
    val settings: StudyBnrSettingEntity = StudyBnrSettingEntity(),
    val lifeStyle: List<StudyBnrLifeStyleEntity> = emptyList(),
)

/**
 * `bnr.converter.*` — reads **both** generations.
 *
 * The version-dispatch in [fromBackup] is the whole point of the `Old*` entity family.
 */
@Singleton
class StudyBnrWeatherConverter @Inject constructor() {

    fun toBackup(weather: StudyWeather) = StudyBnrWeatherEntity(
        key = weather.location.key,
        cityName = weather.location.cityName,
        latitude = weather.location.latitude,
        longitude = weather.location.longitude,
        order = weather.location.priority,
        label = weather.location.label,
        labelType = weather.location.labelType,
        providerName = weather.providerName,
    )

    /** current format */
    fun fromBackup(entity: StudyBnrWeatherEntity) =
        dev.local.weatherstudy.domain.entity.weather.StudyLocation(
            key = entity.key,
            cityName = entity.cityName,
            latitude = entity.latitude,
            longitude = entity.longitude,
            priority = entity.order,
            label = entity.label,
            labelType = entity.labelType,
        )

    /**
     * legacy format — coordinates arrive as strings, there is no provider and no label.
     * The restored location keeps its key so a refresh can repopulate everything else.
     */
    fun fromBackup(entity: StudyBnrOldWeatherEntity) =
        dev.local.weatherstudy.domain.entity.weather.StudyLocation(
            key = entity.key,
            cityName = entity.name,
            latitude = entity.lat.toDoubleOrNull() ?: 0.0,
            longitude = entity.lon.toDoubleOrNull() ?: 0.0,
            priority = entity.idx,
        )
}

/** `bnr.converter.BnrWidgetConverter`. */
@Singleton
class StudyBnrWidgetConverter @Inject constructor() {
    fun toBackup(info: StudyWidgetInfo) = StudyBnrWidgetEntity(
        widgetId = info.widgetId,
        weatherKey = info.weatherKey,
        backgroundColor = info.widgetBGColor,
        transparency = info.widgetBGTransparency,
        nightMode = info.widgetNightMode,
        componentOrder = info.components.sortedBy { it.order }.map { it.type },
    )

    /**
     * A restored widget is marked `RESTORE_PENDING`: the AppWidgetManager ids on the new device
     * are different, so the binding cannot be trusted until the widget is re-placed. That is
     * what `COL_WIDGET_RESTORE_MODE` is for.
     */
    fun fromBackup(entity: StudyBnrWidgetEntity) = StudyWidgetInfo(
        widgetId = entity.widgetId,
        weatherKey = entity.weatherKey,
        widgetBGColor = entity.backgroundColor,
        widgetBGTransparency = entity.transparency,
        widgetNightMode = entity.nightMode,
        widgetRestoreMode = StudyWidgetInfo.RESTORE_PENDING,
        components = entity.componentOrder.mapIndexed { i, t ->
            dev.local.weatherstudy.domain.entity.widget.StudyWidgetComponent(order = i, type = t)
        },
    )
}

/** `bnr.helper.*` — version negotiation. */
@Singleton
class StudyBnrVersionHelper @Inject constructor() {
    fun canRestore(version: Int) = version in StudyBnrConstants.VERSION_LEGACY..StudyBnrConstants.VERSION_CURRENT
    fun isLegacy(version: Int) = version == StudyBnrConstants.VERSION_LEGACY
}

/**
 * `bnr.data.BnrDataSource` — the transport.
 *
 * STUB. The original reads and writes through Samsung Cloud's
 * `com.samsung.android.scloud.oem.lib.ClientProvider`, a bundled Samsung SDK that requires the
 * Samsung Cloud service. The interface is kept so `BackupReceiver` and the restore use cases
 * still have their seam; see reports/samsung-bridge-map.md §4.
 */
interface StudyBnrDataSource {
    suspend fun write(payload: StudyBnrPayload): Int
    suspend fun read(): StudyBnrPayload?
}

/** The documented stub. */
@Singleton
class StudySamsungCloudBnrBridge @Inject constructor() : StudyBnrDataSource {
    override suspend fun write(payload: StudyBnrPayload): Int =
        StudyBnrConstants.RESULT_FAIL_TRANSPORT
    override suspend fun read(): StudyBnrPayload? = null
}

/** `bnr.usecase.BackupWeather`. */
class StudyBackupWeather @Inject constructor(
    private val dataSource: StudyBnrDataSource,
    private val weatherConverter: StudyBnrWeatherConverter,
    private val widgetConverter: StudyBnrWidgetConverter,
    private val weatherRepo: dev.local.weatherstudy.domain.repo.StudyWeatherRepo,
    private val widgetRepo: dev.local.weatherstudy.domain.repo.StudyWidgetRepo,
    private val settingsRepo: dev.local.weatherstudy.domain.repo.StudySettingsRepo,
) : dev.local.weatherstudy.domain.usecase.StudySingleUsecase<Int> {

    override suspend fun invoke(): Int = dataSource.write(
        StudyBnrPayload(
            weathers = weatherRepo.getLocalWeathers().map(weatherConverter::toBackup),
            widgets = widgetRepo.getWidgetInfoList().map(widgetConverter::toBackup),
            settings = StudyBnrSettingEntity(
                tempScale = settingsRepo.getTempScale(),
                autoRefreshInterval = settingsRepo.getAutoRefreshInterval(),
                favoriteLocation = settingsRepo.getFavoriteLocation(),
                showAlert = settingsRepo.getShowAlert(),
            ),
        ),
    )
}

/**
 * `bnr.usecase.RestoreWeather`.
 *
 * Restores locations as bare rows and lets the refresh pipeline repopulate them — a backup
 * carries no forecast data, only which places the user had saved. `setRestoreMode` is what
 * `MainState.BnRState` reads to show the restore notice.
 */
class StudyRestoreWeather @Inject constructor(
    private val dataSource: StudyBnrDataSource,
    private val versionHelper: StudyBnrVersionHelper,
    private val settingsRepo: dev.local.weatherstudy.domain.repo.StudySettingsRepo,
) : dev.local.weatherstudy.domain.usecase.StudySingleUsecase<Int> {

    override suspend fun invoke(): Int {
        val payload = dataSource.read() ?: return StudyBnrConstants.RESULT_FAIL_TRANSPORT
        if (!versionHelper.canRestore(payload.version)) return StudyBnrConstants.RESULT_FAIL_VERSION

        settingsRepo.setTempScale(payload.settings.tempScale)
        settingsRepo.setAutoRefreshInterval(payload.settings.autoRefreshInterval)
        settingsRepo.setFavoriteLocation(payload.settings.favoriteLocation)
        settingsRepo.setRestoreMode(StudyWidgetInfo.RESTORE_PENDING)
        return StudyBnrConstants.RESULT_OK
    }
}
