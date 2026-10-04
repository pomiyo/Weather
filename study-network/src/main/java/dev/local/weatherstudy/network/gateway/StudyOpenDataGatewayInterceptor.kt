package dev.local.weatherstudy.network.gateway

import android.content.Context
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Educational reconstruction — a reconstruction-only component.
 *
 * Corresponds conceptually to: nothing in the original. The original talks to five
 * Samsung-mediated backends with credentials bundled in the APK; none of those are
 * contacted, reused or imitated here.
 *
 * ### What it is
 *
 * An **in-process gateway**: it answers the reconstruction's own endpoint shapes
 * (`forecast`, `search`, `location/point`, …) with the reconstruction's own DTO shape
 * ([dev.local.weatherstudy.network.models.forecast.common.StudyLocalWeatherDto]), and it
 * sources the numbers from Open-Meteo, a public keyless weather API. The project's
 * STEP 12 lists "a public API" among the permitted transport replacements.
 *
 * ### Why it sits at the interceptor level
 *
 * Same reason as [dev.local.weatherstudy.network.fixture.StudyFixtureInterceptor], which
 * it supersedes as the default: everything above the transport stays genuine. The
 * Retrofit services, Moshi parsing, per-provider converters, the auth and message
 * interceptors, the repository and the use-case pipeline all execute against a real
 * response body. A fake `WeatherRemoteDataSource` would make the network module dead code.
 *
 * It is registered LAST in the application-interceptor chain so the per-provider auth and
 * message interceptors run before it, as they would in front of a real server.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyOpenDataGatewayInterceptor @Inject constructor(
    context: Context,
) : Interceptor {

    private val upstream = StudyOpenDataClient()
    private val places = StudyGatewayPlaceIndex(context)
    private val composer = StudyGatewayForecastComposer()
    private val reverseGeocoder = StudyGatewayReverseGeocoder(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.trim('/').substringAfter("weatherstudy/")
        val body = try {
            route(path, request)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("gateway failed for '$path': ${e.message}", e)
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (body == null) HTTP_NOT_FOUND else HTTP_OK)
            .message(if (body == null) "Not Found (gateway)" else "OK (gateway)")
            .body((body ?: EMPTY_OBJECT).toResponseBody(JSON))
            .build()
    }

    private fun route(path: String, request: Request): String? {
        val url = request.url
        return when (path) {
            "forecast", "forecast/partial", "airquality" -> {
                val place = url.queryParameter("geocode")?.let(::placeForGeocode)
                    ?: resolvePlace(listOf("placeId", "code", "locationKey").firstNotNullOfOrNull(url::queryParameter))
                    ?: return null
                forecast(place)
            }

            "search", "search/autocomplete" ->
                search(url.queryParameter("query").orEmpty(), url.queryParameter("language"))

            "location/point" -> {
                val place = url.queryParameter("geocode")?.let(::placeForGeocode) ?: return null
                JSONObject()
                    .put("results", JSONArray().put(composer.location(place)))
                    .put("count", 1)
                    .toString()
            }

            // capabilities the public source has no equivalent for: an empty document, which
            // every DTO field tolerates
            else -> EMPTY_OBJECT
        }
    }

    private fun forecast(place: StudyGatewayPlace): String {
        val weather = upstream.forecast(place.latitude, place.longitude)
        // air quality is a separate upstream service; a failure there must not fail the forecast
        val air = runCatching { upstream.airQuality(place.latitude, place.longitude) }.getOrNull()
        val resolved = place.copy(
            timeZone = weather.optString("timezone", place.timeZone),
            utcOffsetSeconds = weather.optInt("utc_offset_seconds", place.utcOffsetSeconds),
        )
        places.put(resolved)
        return composer.compose(resolved, weather, air, System.currentTimeMillis()).toString()
    }

    private fun search(query: String, language: String?): String {
        if (query.isBlank()) return JSONObject().put("results", JSONArray()).put("count", 0).toString()
        val found = upstream.geocode(query.trim(), language?.substringBefore('-') ?: "en")
        val results = JSONArray()
        val rows = found.optJSONArray("results") ?: JSONArray()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val place = StudyGatewayPlace(
                placeId = "$PREFIX_PLACE${row.optLong("id")}",
                city = row.optString("name"),
                state = row.optString("admin1"),
                country = row.optString("country"),
                countryCode = row.optString("country_code"),
                latitude = row.optDouble("latitude"),
                longitude = row.optDouble("longitude"),
                timeZone = row.optString("timezone"),
            )
            places.put(place)
            results.put(composer.location(place))
        }
        return JSONObject().put("results", results).put("count", results.length()).toString()
    }

    private fun resolvePlace(placeId: String?): StudyGatewayPlace? {
        if (placeId.isNullOrEmpty()) return null
        places.get(placeId)?.let { return it }
        // a coordinate-derived id carries its own position, so it survives a cleared index
        return StudyGatewayPlace.fromGeoId(placeId)
    }

    private fun placeForGeocode(geocode: String): StudyGatewayPlace? {
        val parts = geocode.split(',')
        val latitude = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
        val longitude = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
        val id = StudyGatewayPlace.geoId(latitude, longitude)
        places.get(id)?.takeIf { it.city.isNotEmpty() }?.let { return it }
        val named = reverseGeocoder.name(latitude, longitude)
        return StudyGatewayPlace(
            placeId = id,
            city = named?.city.orEmpty(),
            state = named?.state.orEmpty(),
            country = named?.country.orEmpty(),
            countryCode = named?.countryCode.orEmpty(),
            latitude = latitude,
            longitude = longitude,
        ).also(places::put)
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val EMPTY_OBJECT = "{}"
        const val PREFIX_PLACE = "om-"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
