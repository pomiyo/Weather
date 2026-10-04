package dev.local.weatherstudy.network.gateway

import android.content.Context
import android.location.Geocoder
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * A place as the gateway knows it. The reconstruction's `placeId` is opaque to every
 * layer above the transport, exactly as a provider's location key is in the original.
 */
data class StudyGatewayPlace(
    val placeId: String,
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val countryCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timeZone: String = "",
    val utcOffsetSeconds: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("placeId", placeId)
        .put("city", city)
        .put("state", state)
        .put("country", country)
        .put("countryCode", countryCode)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("timeZone", timeZone)
        .put("utcOffsetSeconds", utcOffsetSeconds)

    companion object {
        private const val PREFIX_GEO = "geo-"
        private const val GEO_SCALE = 100.0

        /** ~1 km grid, so small movements of the device resolve to the same place */
        fun geoId(latitude: Double, longitude: Double): String =
            "$PREFIX_GEO${(latitude * GEO_SCALE).roundToInt()}_${(longitude * GEO_SCALE).roundToInt()}"

        fun fromGeoId(placeId: String): StudyGatewayPlace? {
            if (!placeId.startsWith(PREFIX_GEO)) return null
            val parts = placeId.removePrefix(PREFIX_GEO).split('_')
            val latitude = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val longitude = parts.getOrNull(1)?.toIntOrNull() ?: return null
            return StudyGatewayPlace(
                placeId = placeId,
                latitude = latitude / GEO_SCALE,
                longitude = longitude / GEO_SCALE,
            )
        }

        fun fromJson(json: JSONObject) = StudyGatewayPlace(
            placeId = json.optString("placeId"),
            city = json.optString("city"),
            state = json.optString("state"),
            country = json.optString("country"),
            countryCode = json.optString("countryCode"),
            latitude = json.optDouble("latitude", 0.0),
            longitude = json.optDouble("longitude", 0.0),
            timeZone = json.optString("timeZone"),
            utcOffsetSeconds = json.optInt("utcOffsetSeconds", 0),
        )
    }
}

/**
 * The gateway's memory of which `placeId` means which place — what a real backend holds
 * server-side. Kept in its own preferences file, outside the weather database, because
 * it is transport state and not app data.
 */
internal class StudyGatewayPlaceIndex(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun get(placeId: String): StudyGatewayPlace? =
        prefs.getString(placeId, null)
            ?.let { runCatching { StudyGatewayPlace.fromJson(JSONObject(it)) }.getOrNull() }

    fun put(place: StudyGatewayPlace) {
        prefs.edit().putString(place.placeId, place.toJson().toString()).apply()
    }

    private companion object {
        const val FILE_NAME = "study_gateway_places"
    }
}

/** A coordinate turned into a display name with the platform geocoder. */
internal class StudyGatewayReverseGeocoder(context: Context) {

    private val appContext = context.applicationContext

    data class Name(val city: String, val state: String, val country: String, val countryCode: String)

    @Suppress("DEPRECATION")
    fun name(latitude: Double, longitude: Double): Name? = runCatching {
        if (!Geocoder.isPresent()) return null
        val address = Geocoder(appContext, Locale.getDefault())
            .getFromLocation(latitude, longitude, 1)
            ?.firstOrNull() ?: return null
        Name(
            city = address.locality ?: address.subAdminArea ?: address.adminArea.orEmpty(),
            state = address.adminArea.orEmpty(),
            country = address.countryName.orEmpty(),
            countryCode = address.countryCode.orEmpty(),
        )
    }.getOrNull()
}

/**
 * The only class in the project that talks to the public internet. Three read-only GETs,
 * no key, no account, no user data beyond the coordinate or the typed city name.
 */
internal class StudyOpenDataClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun forecast(latitude: Double, longitude: Double): JSONObject = get(
        FORECAST_URL.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", latitude.toString())
            .addQueryParameter("longitude", longitude.toString())
            .addQueryParameter("current", CURRENT_FIELDS)
            .addQueryParameter("hourly", HOURLY_FIELDS)
            .addQueryParameter("daily", DAILY_FIELDS)
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("timeformat", "unixtime")
            .addQueryParameter("forecast_days", FORECAST_DAYS)
            .addQueryParameter("past_days", "1")
            .build(),
    )

    fun airQuality(latitude: Double, longitude: Double): JSONObject = get(
        AIR_QUALITY_URL.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", latitude.toString())
            .addQueryParameter("longitude", longitude.toString())
            .addQueryParameter("current", "us_aqi,pm10,pm2_5")
            .addQueryParameter("timeformat", "unixtime")
            .build(),
    )

    fun geocode(name: String, language: String): JSONObject = get(
        GEOCODING_URL.toHttpUrl().newBuilder()
            .addQueryParameter("name", name)
            .addQueryParameter("count", "12")
            .addQueryParameter("language", language)
            .addQueryParameter("format", "json")
            .build(),
    )

    private fun get(url: HttpUrl): JSONObject {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("upstream ${url.host} returned ${response.code}")
            val text = response.body?.string() ?: throw IOException("upstream ${url.host} returned no body")
            return JSONObject(text)
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 20L
        const val FORECAST_DAYS = "10"
        const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val AIR_QUALITY_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
        const val CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
            "precipitation,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m," +
            "dew_point_2m,visibility,uv_index"
        const val HOURLY_FIELDS = "temperature_2m,weather_code,precipitation_probability,precipitation," +
            "wind_speed_10m,wind_direction_10m,relative_humidity_2m,is_day"
        const val DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
            "precipitation_probability_max,precipitation_sum,uv_index_max"
    }
}
