package dev.local.weatherstudy.network.fixture

import android.content.Context
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Educational reconstruction — a reconstruction-only component.
 *
 * Corresponds conceptually to: nothing in the original. The original talks to five
 * Samsung-mediated backends with credentials bundled in the APK.
 *
 * ### Why it exists
 *
 * The project's STEP 12 says to keep the network architecture and replace the transport
 * with local fixtures, a mock server, a public API or a fake. This is that replacement,
 * and it is deliberately placed at the **OkHttp interceptor** level rather than by
 * swapping the data source:
 *
 * - every layer above stays genuine — the Retrofit services, the Moshi parsing, the
 *   per-provider converters, the auth and message interceptors, the repository and the
 *   use-case pipeline all run exactly as they do in the original
 * - nothing ever leaves the process, so no Samsung endpoint is contacted even by
 *   accident
 *
 * If this were a fake `WeatherRemoteDataSource` instead, the whole network module would
 * be dead code and the thing being studied would not execute.
 *
 * Fixtures live in `assets/fixtures/` and are matched by the request path.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyFixtureInterceptor @Inject constructor(
    private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.trim('/').substringAfter("weatherstudy/")
        val body = readFixture(path)
            ?: throw IOException(
                "no fixture for '$path'. The reconstruction never contacts a real backend; " +
                    "add assets/fixtures/$path.json to exercise this call.",
            )

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(HTTP_OK)
            .message("OK (fixture)")
            .body(body.toResponseBody(JSON))
            .build()
    }

    private fun readFixture(path: String): String? = runCatching {
        context.assets.open("fixtures/${path.ifEmpty { "forecast" }}.json")
            .bufferedReader()
            .use { it.readText() }
    }.getOrNull()

    private companion object {
        const val HTTP_OK = 200
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
