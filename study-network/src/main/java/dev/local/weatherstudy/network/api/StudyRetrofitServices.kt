package dev.local.weatherstudy.network.api.forecast

import dev.local.weatherstudy.network.models.forecast.common.StudyLocalWeatherDto
import dev.local.weatherstudy.network.models.forecast.common.StudySearchDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the seven Retrofit services in
 * com.samsung.android.weather.network.api.forecast:
 * TwcRetrofitService, WjpRetrofitService, WkrRetrofitService, HuaRetrofitService,
 * SRCRetrofitService, SRCAlertRetrofitService (+ GalaxyStoreRetrofitService elsewhere)
 *
 * ### What the original looks like, and what is reconstructed
 *
 * Each provider has a **triad**, visible as three classes per package in the APK:
 *
 * ```
 * network/api/forecast/<provider>/
 *   <P>RetrofitService      the @GET interface
 *   <P>AuthInterceptor      adds the provider's credential to every request
 *   <P>MessageInterceptor   provider-specific error/payload handling
 *   <P>Auth                 (hua, src only) a separate signing step
 * ```
 *
 * The services are all-`@GET`, all-`@Query`, and the method names say what the
 * capability split in `:study-domain`'s eleven `*Api` interfaces is for — e.g. the
 * original's `TwcRetrofitService` has `getCurrentAndDailyForecastByGeocode` AND
 * `getCurrentAndDailyForecastByPlaceId`, which is exactly why
 * `StudyForecastApi.getRemoteWeather` has a coordinate overload and a location overload.
 *
 * **No real endpoint path, host, query-parameter name or credential is reproduced.**
 * Base URLs come from [dev.local.weatherstudy.domain.source.backend.StudySecureLinkProvider],
 * which the reconstruction points at local fixtures; the method *shapes* are what carry
 * the architecture. See `reports/class-mapping.md` §10 for the provider renaming.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyProviderARetrofitService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("placeId") placeId: String,
        @Query("units") units: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("forecast")
    suspend fun getForecastByGeocode(
        @Query("geocode") geocode: String,
        @Query("units") units: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("forecast/partial")
    suspend fun getPartialForecast(
        @Query("placeId") placeId: String,
        @Query("units") units: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("forecast/represent")
    suspend fun getRepresentForecast(
        @Query("code") code: String,
    ): StudyLocalWeatherDto

    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("language") language: String,
    ): StudySearchDto

    @GET("search/autocomplete")
    suspend fun autoComplete(
        @Query("query") query: String,
        @Query("language") language: String,
    ): StudySearchDto

    @GET("location/point")
    suspend fun getLocationPoint(
        @Query("geocode") geocode: String,
        @Query("language") language: String,
    ): StudySearchDto

    @GET("airquality")
    suspend fun getAirQuality(
        @Query("placeId") placeId: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("expiretime")
    suspend fun getExpireTime(
        @Query("placeId") placeId: String,
    ): StudyLocalWeatherDto

    @GET("video")
    suspend fun getVideoList(
        @Query("placeId") placeId: String,
    ): StudyLocalWeatherDto
}

/**
 * Corresponds conceptually to `WjpRetrofitService`.
 *
 * Observed difference from provider A, and the reason the services are not one
 * interface: this backend serves an XML feed for part of its payload (the APK bundles
 * Simple XML alongside Moshi), and exposes a "forecast change" endpoint the others do
 * not. Capability differences like this are what the eleven domain `*Api` interfaces
 * and `StudyWeatherPolicy` exist to express.
 */
interface StudyProviderBRetrofitService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("code") code: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("search")
    suspend fun search(@Query("query") query: String): StudySearchDto

    @GET("forecast/change")
    suspend fun getForecastChange(@Query("code") code: String): StudyLocalWeatherDto

    @GET("todaystories")
    suspend fun getTodayStories(@Query("code") code: String): StudyLocalWeatherDto

    @GET("radar")
    suspend fun getRadar(@Query("code") code: String): StudyLocalWeatherDto
}

/**
 * Corresponds conceptually to `WkrRetrofitService`.
 *
 * Observed difference: this is the only provider with the themed-place catalogue the
 * map search uses — `getThemeCategories` / `getThemeRegions` / `getThemePlaces`, which
 * is why `StudyThemeApi` is a separate capability and `supportThemeArea()` is a policy
 * question.
 */
interface StudyProviderCRetrofitService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("code") code: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("search")
    suspend fun search(@Query("query") query: String): StudySearchDto

    @GET("forecast/change")
    suspend fun getForecastChange(@Query("code") code: String): StudyLocalWeatherDto

    @GET("theme/categories")
    suspend fun getThemeCategories(@Query("language") language: String): StudySearchDto

    @GET("theme/regions")
    suspend fun getThemeRegions(
        @Query("categoryId") categoryId: String,
        @Query("language") language: String,
    ): StudySearchDto

    @GET("theme/places")
    suspend fun getThemePlaces(
        @Query("categoryId") categoryId: String,
        @Query("regionIds") regionIds: String,
    ): StudySearchDto

    @GET("news")
    suspend fun getNews(@Query("code") code: String): StudyLocalWeatherDto
}

/**
 * Corresponds conceptually to `HuaRetrofitService`.
 *
 * Observed difference: this provider's package is the one with a separate `HuaAuth`
 * class in addition to its interceptor — a two-step signing flow rather than a single
 * header. See [StudyProviderAuth].
 */
interface StudyProviderDRetrofitService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("locationKey") locationKey: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("search")
    suspend fun search(@Query("query") query: String): StudySearchDto

    @GET("airquality")
    suspend fun getAirQuality(@Query("locationKey") locationKey: String): StudyLocalWeatherDto
}

/**
 * Corresponds conceptually to `SRCRetrofitService`.
 *
 * Observed difference: it is the only provider split across TWO services —
 * `SRCRetrofitService` and `SRCAlertRetrofitService`, on different base URLs
 * (`getSRCDomain` vs `getSRCAlertDomain` on the link provider). Severe-weather alerts
 * come from a separate host, which is why [StudyProviderEAlertRetrofitService] exists.
 */
interface StudyProviderERetrofitService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("code") code: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto

    @GET("search")
    suspend fun search(@Query("query") query: String): StudySearchDto

    @GET("insight")
    suspend fun getInsight(@Query("code") code: String): StudyLocalWeatherDto
}

/** Corresponds conceptually to `SRCAlertRetrofitService` — a second host for alerts. */
interface StudyProviderEAlertRetrofitService {

    @GET("alerts")
    suspend fun getAlerts(
        @Query("code") code: String,
        @Query("language") language: String,
    ): StudyLocalWeatherDto
}
