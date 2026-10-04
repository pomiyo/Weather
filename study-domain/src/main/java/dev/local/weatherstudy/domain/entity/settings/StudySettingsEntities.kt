package dev.local.weatherstudy.domain.entity.settings

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.settings.CorpAppInfo
 *
 * Observed responsibility: an allow-listed companion app permitted to read weather
 * through the exported content providers. The `certificate` field is the signature
 * check — which is also why `SignatureCheckContentProvider` exists and why that
 * check cannot be reproduced off a platform-signed build.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyCorpAppInfo(
    val packageName: String,
    val key: String = "",
    val name: String = "",
    val certificate: String = "",
)
