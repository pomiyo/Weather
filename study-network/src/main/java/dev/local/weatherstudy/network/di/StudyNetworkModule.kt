package dev.local.weatherstudy.network.di

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider
import dev.local.weatherstudy.domain.source.backend.StudyLogProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureKeyProvider
import dev.local.weatherstudy.network.api.forecast.StudyProviderARetrofitService
import dev.local.weatherstudy.network.api.forecast.StudyProviderAuthInterceptor
import dev.local.weatherstudy.network.api.forecast.StudyProviderBRetrofitService
import dev.local.weatherstudy.network.api.forecast.StudyProviderCRetrofitService
import dev.local.weatherstudy.network.api.forecast.StudyProviderDRetrofitService
import dev.local.weatherstudy.network.api.forecast.StudyProviderERetrofitService
import dev.local.weatherstudy.network.api.forecast.StudyProviderEAlertRetrofitService
import dev.local.weatherstudy.network.api.forecast.StudyProviderMessageInterceptor
import dev.local.weatherstudy.network.api.forecast.StudyRegionEndpointResolver
import dev.local.weatherstudy.network.fixture.StudyFixtureInterceptor
import dev.local.weatherstudy.network.gateway.StudyOpenDataGatewayInterceptor
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.network.di.NetworkModule
 * com.samsung.android.weather.data.di.ApiModule
 *
 * Observed responsibilities:
 * - one `OkHttpClient` and one `Moshi`, shared
 * - **one Retrofit instance per provider**, each with its own base URL and its own
 *   auth + message interceptor pair. That is why the original has five near-identical
 *   provider packages rather than one parameterised client: the interceptor chain
 *   differs per backend.
 * - the per-provider services are qualified, because five bindings of
 *   "a Retrofit service" would otherwise be ambiguous
 *
 * The reconstruction ends the chain with [StudyOpenDataGatewayInterceptor], an in-process
 * gateway that answers in the reconstruction's own DTO shape. It is the **last**
 * application interceptor, so logging, auth and message handling all run in front of it
 * exactly as they would in front of a real server. Everything else in the chain is the
 * original's shape. [StudyFixtureInterceptor] remains available as the offline
 * alternative — swap the one parameter of [provideOkHttpClient].
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudyNetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(logProvider: StudyLogProvider): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = when {
                logProvider.isBodyLogEnabled() -> HttpLoggingInterceptor.Level.BODY
                logProvider.isNetworkLogEnabled() -> HttpLoggingInterceptor.Level.BASIC
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }

    /**
     * Reconstruction of the original's shared client. The gateway is the terminal
     * interceptor, so no request ever reaches a provider host; the auth and message
     * interceptors are inserted in front of it per provider below.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        gatewayInterceptor: StudyOpenDataGatewayInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(gatewayInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideFixtureInterceptor(@ApplicationContext context: Context): StudyFixtureInterceptor =
        StudyFixtureInterceptor(context)

    // ---- one Retrofit per provider, as in the original ----

    @Provides
    @Singleton
    @ProviderA
    fun provideProviderARetrofit(
        client: OkHttpClient,
        moshi: Moshi,
        resolver: StudyRegionEndpointResolver,
        secureKeyProvider: StudySecureKeyProvider,
        logProvider: StudyLogProvider,
    ): Retrofit = buildRetrofit(
        StudyForecastProvider.PROVIDER_A, client, moshi, resolver, secureKeyProvider, logProvider,
    )

    @Provides
    @Singleton
    @ProviderB
    fun provideProviderBRetrofit(
        client: OkHttpClient,
        moshi: Moshi,
        resolver: StudyRegionEndpointResolver,
        secureKeyProvider: StudySecureKeyProvider,
        logProvider: StudyLogProvider,
    ): Retrofit = buildRetrofit(
        StudyForecastProvider.PROVIDER_B, client, moshi, resolver, secureKeyProvider, logProvider,
    )

    @Provides
    @Singleton
    @ProviderC
    fun provideProviderCRetrofit(
        client: OkHttpClient,
        moshi: Moshi,
        resolver: StudyRegionEndpointResolver,
        secureKeyProvider: StudySecureKeyProvider,
        logProvider: StudyLogProvider,
    ): Retrofit = buildRetrofit(
        StudyForecastProvider.PROVIDER_C, client, moshi, resolver, secureKeyProvider, logProvider,
    )

    @Provides
    @Singleton
    @ProviderD
    fun provideProviderDRetrofit(
        client: OkHttpClient,
        moshi: Moshi,
        resolver: StudyRegionEndpointResolver,
        secureKeyProvider: StudySecureKeyProvider,
        logProvider: StudyLogProvider,
    ): Retrofit = buildRetrofit(
        StudyForecastProvider.PROVIDER_D, client, moshi, resolver, secureKeyProvider, logProvider,
    )

    @Provides
    @Singleton
    @ProviderE
    fun provideProviderERetrofit(
        client: OkHttpClient,
        moshi: Moshi,
        resolver: StudyRegionEndpointResolver,
        secureKeyProvider: StudySecureKeyProvider,
        logProvider: StudyLogProvider,
    ): Retrofit = buildRetrofit(
        StudyForecastProvider.PROVIDER_E, client, moshi, resolver, secureKeyProvider, logProvider,
    )

    private fun buildRetrofit(
        providerId: String,
        client: OkHttpClient,
        moshi: Moshi,
        resolver: StudyRegionEndpointResolver,
        secureKeyProvider: StudySecureKeyProvider,
        logProvider: StudyLogProvider,
    ): Retrofit {
        // the shared client's last interceptor answers the request, so the per-provider
        // pair has to go in front of it rather than behind
        val terminal = client.interceptors.last()
        val perProviderClient = client.newBuilder()
            .apply { interceptors().remove(terminal) }
            .addInterceptor(StudyProviderAuthInterceptor(providerId, secureKeyProvider))
            .addInterceptor(StudyProviderMessageInterceptor(providerId, logProvider))
            .addInterceptor(terminal)
            .build()
        return Retrofit.Builder()
            .baseUrl(StudyRegionEndpointResolver.FIXTURE_BASE_URL)
            .client(perProviderClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // ---- services ----

    @Provides
    @Singleton
    fun provideProviderAService(@ProviderA retrofit: Retrofit): StudyProviderARetrofitService =
        retrofit.create(StudyProviderARetrofitService::class.java)

    @Provides
    @Singleton
    fun provideProviderBService(@ProviderB retrofit: Retrofit): StudyProviderBRetrofitService =
        retrofit.create(StudyProviderBRetrofitService::class.java)

    @Provides
    @Singleton
    fun provideProviderCService(@ProviderC retrofit: Retrofit): StudyProviderCRetrofitService =
        retrofit.create(StudyProviderCRetrofitService::class.java)

    @Provides
    @Singleton
    fun provideProviderDService(@ProviderD retrofit: Retrofit): StudyProviderDRetrofitService =
        retrofit.create(StudyProviderDRetrofitService::class.java)

    @Provides
    @Singleton
    fun provideProviderEService(@ProviderE retrofit: Retrofit): StudyProviderERetrofitService =
        retrofit.create(StudyProviderERetrofitService::class.java)

    @Provides
    @Singleton
    fun provideProviderEAlertService(
        @ProviderE retrofit: Retrofit,
    ): StudyProviderEAlertRetrofitService =
        retrofit.create(StudyProviderEAlertRetrofitService::class.java)
}

/**
 * Qualifiers, one per backend.
 *
 * The original distinguishes the five Retrofit instances the same way — without them,
 * five `@Provides Retrofit` bindings would be ambiguous. Their presence in the DI graph
 * is itself the clearest statement that this app has five backends, not one.
 */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ProviderA

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ProviderB

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ProviderC

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ProviderD

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ProviderE
