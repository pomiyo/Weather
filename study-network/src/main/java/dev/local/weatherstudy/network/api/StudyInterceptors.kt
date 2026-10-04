package dev.local.weatherstudy.network.api.forecast

import dev.local.weatherstudy.domain.source.backend.StudyLogProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureKeyProvider
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the five `*AuthInterceptor` classes in
 * com.samsung.android.weather.network.api.forecast.{twc,wjp,wkr,hua,src}
 *
 * Observed responsibility: attach the provider's credential to every outbound request.
 * The interceptor is **per provider** because each backend takes it differently — one
 * as a query parameter, one as a header, two behind a separate signing step
 * (`HuaAuth`, `SRCAuth`).
 *
 * ### This is the credential boundary, and it is empty here
 *
 * The original reads its keys from the bundled `assets/database/backend_ver8.db` via
 * `SecureKeyProvider`. No key was read from that database during this project, none is
 * reproduced, and the reconstruction's `StudySecureKeyProvider` implementation returns
 * empty strings. The interceptor therefore attaches nothing — but it still exists, in
 * the right place, with the right shape, so the architecture reads correctly: the
 * network layer never knows a credential's value, only that it asks for one.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyProviderAuthInterceptor @Inject constructor(
    private val providerId: String,
    private val secureKeyProvider: StudySecureKeyProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = runBlocking { secureKeyProvider.getProviderApiKey(providerId) }
        val request = chain.request()
        val authorised = if (apiKey.isEmpty()) {
            // the reconstruction's expected path: nothing to attach
            request
        } else {
            request.newBuilder().header(HEADER_API_KEY, apiKey).build()
        }
        return chain.proceed(authorised)
    }

    private companion object {
        const val HEADER_API_KEY = "X-Api-Key"
    }
}

/**
 * Corresponds conceptually to `HuaAuth` / `SRCAuth`.
 *
 * Observed responsibility: two of the five providers need a signing step distinct from
 * the interceptor — a secret combined with the request to produce a per-request token.
 * Reconstructed as the same seam, with no algorithm and no secret.
 */
class StudyProviderAuth @Inject constructor(
    private val secureKeyProvider: StudySecureKeyProvider,
) {
    /**
     * The original derives a per-request signature from its secret key. The derivation
     * is proprietary and is not reconstructed; the reconstruction returns an empty
     * token, which the interceptor then declines to attach.
     */
    suspend fun sign(providerId: String, canonicalRequest: String): String {
        val secret = secureKeyProvider.getProviderSecretKey(providerId)
        return if (secret.isEmpty()) "" else UNRECONSTRUCTED_SIGNATURE
    }

    private companion object {
        const val UNRECONSTRUCTED_SIGNATURE = ""
    }
}

/**
 * Corresponds conceptually to the five `*MessageInterceptor` classes.
 *
 * Observed responsibility: provider-specific *response* handling, separate from auth.
 * Each backend reports failure differently — HTTP status for some, a success flag in a
 * 200 body for others — so the error mapping cannot be shared. This is what lets the
 * repository above see one error model.
 *
 * `LogProvider` gates body logging, which is why it is injected rather than a
 * BuildConfig check: developer options can turn it on at runtime.
 */
class StudyProviderMessageInterceptor @Inject constructor(
    private val providerId: String,
    private val logProvider: StudyLogProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) {
            throw StudyProviderException(providerId, response.code, response.message)
        }
        return response
    }
}

/**
 * Corresponds conceptually to the per-provider exceptions the message interceptors
 * raise (R8-renamed in the APK, so the names are not recoverable).
 */
class StudyProviderException(
    val providerId: String,
    val code: Int,
    override val message: String,
) : java.io.IOException("provider=$providerId code=$code message=$message")
