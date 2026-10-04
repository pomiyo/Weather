package dev.local.weatherstudy.provider

import dagger.hilt.android.EntryPointAccessors
import dev.local.weatherstudy.data.cp.StudyAbsWeatherContentProvider
import dev.local.weatherstudy.data.cp.StudyProviderTrustLevel
import dev.local.weatherstudy.data.cp.StudySignatureCheckContentProvider
import dev.local.weatherstudy.domain.repo.StudyWeatherProviderRepo
import dev.local.weatherstudy.domain.source.backend.StudyAuthorityProvider

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.provider.WeatherContentProvider
 * com.sec.android.daemonapp.provider.SystemLevelContentProvider
 * com.sec.android.daemonapp.provider.DangerousLevelContentProvider
 *
 * Three exported providers over **one** dataset at three trust tiers — see
 * [StudyAbsWeatherContentProvider] for why. The subclass's whole contribution is its
 * trust level and (in the manifest) its permission declaration.
 *
 * A `ContentProvider` is created before `Application.onCreate` and cannot be
 * `@AndroidEntryPoint`, so dependencies come from a Hilt `EntryPoint` resolved lazily on
 * first query — which is also what the original must do.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyWeatherContentProvider : StudyAbsWeatherContentProvider() {
    override val trustLevel = StudyProviderTrustLevel.LEGACY
    override val repo: StudyWeatherProviderRepo get() = entryPoint().weatherProviderRepo()
    override val authorityProvider: StudyAuthorityProvider get() = entryPoint().authorityProvider()
}

/**
 * The system tier. It extends [StudySignatureCheckContentProvider], whose
 * `isCallerTrusted()` is **always false** in the reconstruction — the original's check
 * requires the app to be platform-signed. A documented denial, not a silent allow.
 */
class StudySystemLevelContentProvider : StudySignatureCheckContentProvider() {
    override val trustLevel = StudyProviderTrustLevel.SYSTEM
    override val repo: StudyWeatherProviderRepo get() = entryPoint().weatherProviderRepo()
    override val authorityProvider: StudyAuthorityProvider get() = entryPoint().authorityProvider()
}

/** The third-party tier: a `dangerous` runtime permission, read-only. */
class StudyDangerousLevelContentProvider : StudyAbsWeatherContentProvider() {
    override val trustLevel = StudyProviderTrustLevel.DANGEROUS
    override val repo: StudyWeatherProviderRepo get() = entryPoint().weatherProviderRepo()
    override val authorityProvider: StudyAuthorityProvider get() = entryPoint().authorityProvider()
}

/** Hilt entry point: a ContentProvider is created too early for `@AndroidEntryPoint`. */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface StudyProviderEntryPoint {
    fun weatherProviderRepo(): StudyWeatherProviderRepo
    fun authorityProvider(): StudyAuthorityProvider
}

private fun android.content.ContentProvider.entryPoint(): StudyProviderEntryPoint =
    EntryPointAccessors.fromApplication(
        requireNotNull(context) { "provider has no context" }.applicationContext,
        StudyProviderEntryPoint::class.java,
    )
