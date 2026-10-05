package dev.local.weatherstudy.network.gateway

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction — a reconstruction-only component.
 *
 * Corresponds conceptually to: `ForecastProviderManager`'s *shape*, not its contents. The
 * original routes between five Samsung-mediated backends by country code and never lets
 * the user choose; `dispatchByCountryCode` is the whole mechanism, and the active provider
 * is what the attribution line, the AQI scale and several card policies read.
 *
 * This reconstruction cannot reach any of those backends, so the same idea is pointed at
 * public keyless services instead. The user-facing picker is a deliberate deviation —
 * documented in reports/visual-comparison.md — and it exists because a reconstruction
 * whose numbers disagree with the original's is far easier to reason about when you can
 * swap the source of those numbers and watch what changes.
 *
 * ### Both services are free, keyless and anonymous
 *
 * Neither needs an account, a token or a payment method. The only thing either asks for is
 * identification: MET Norway's terms require a `User-Agent` naming the application and a
 * contact, which [StudyOpenDataClient] sends. Nothing about the user is transmitted beyond
 * the coordinate being asked about — the same contract as before.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
enum class StudyGatewayService(
    val id: String,
    /** what the settings screen shows */
    val label: String,
    /** what the attribution card shows — a contractual requirement for both services */
    val attribution: String,
    val attributionUrl: String,
) {
    OPEN_METEO(
        id = "open_meteo",
        label = "Open-Meteo",
        attribution = "Open-Meteo.com",
        attributionUrl = "https://open-meteo.com/",
    ),

    /**
     * The Norwegian Meteorological Institute's own forecast, the one behind yr.no.
     *
     * A genuinely different vendor rather than a different model: its own observations,
     * its own assimilation, its own symbol vocabulary. It publishes no sunrise, sunset,
     * visibility or air quality in `locationforecast`, which is why
     * [StudyMetNoForecastComposer] computes the first two and omits the others — a
     * provider supplying less is a case the original's card policies already handle.
     */
    MET_NORWAY(
        id = "met_no",
        label = "MET Norway (yr.no)",
        attribution = "MET Norway",
        attributionUrl = "https://www.met.no/en/free-meteorological-data",
    ),
    ;

    companion object {
        val DEFAULT = OPEN_METEO

        fun of(id: String?): StudyGatewayService =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Where the choice lives.
 *
 * SharedPreferences rather than the settings table: the active service is not part of the
 * original's data model, and adding a column to `TABLE_SETTING_INFO` would put a
 * reconstruction-only concern inside a schema that is reproduced from the original. The
 * gateway already keeps its place index the same way.
 */
@Singleton
class StudyGatewayServiceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var service: StudyGatewayService
        get() = StudyGatewayService.of(prefs.getString(KEY_SERVICE, null))
        set(value) = prefs.edit().putString(KEY_SERVICE, value.id).apply()

    private companion object {
        const val FILE_NAME = "study_gateway_service"
        const val KEY_SERVICE = "service"
    }
}
