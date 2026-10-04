package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.usecase.StudySingleUsecase
import dev.local.weatherstudy.domain.usecase.StudyUsecase
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.GetWeather
 *
 * Observed responsibilities:
 * - the single read path every screen uses. It does NOT hit the network.
 * - it implements BOTH `Usecase<Weather, String>` (one location by key) and
 *   `SingleUsecase<List<Weather>>` (all saved locations) — one class, two `invoke`
 *   overloads, exactly as the original declares it
 * - before returning, it runs the two "revise" passes: [StudyReviseContent] strips
 *   content the active provider's policy does not support, and [StudyReviseWebLink]
 *   rewrites or removes outbound links. **A read is not a plain fetch** — the
 *   persisted row may contain cards the current policy forbids, because the provider
 *   can change under a cached row.
 * - it consults the consent use cases: if the user has not agreed, or must re-agree
 *   to a newer policy version, links are restricted
 *
 * Dependencies match the original's six constructor parameters.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyGetWeather @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
    private val reviseContent: StudyReviseContent,
    private val reviseWebLink: StudyReviseWebLink,
    private val whetherToConsentUcl: StudyWhetherToConsentUcl,
    private val whetherToReconsentUcl: StudyWhetherToReconsentUcl,
) : StudyUsecase<StudyWeather?, String>, StudySingleUsecase<List<StudyWeather>> {

    /** one saved location, by its provider key */
    override suspend fun invoke(arg: String): StudyWeather? {
        val stored = weatherRepo.getLocalWeather(arg) ?: return null
        return revise(stored)
    }

    /** every saved location, in list order */
    override suspend fun invoke(): List<StudyWeather> =
        weatherRepo.getLocalWeathers()
            .sortedBy { it.location.priority }
            .map { revise(it) }

    private suspend fun revise(weather: StudyWeather): StudyWeather {
        val activeProvider = settingsRepo.getActiveCpType()
        val consentPending = whetherToConsentUcl() || whetherToReconsentUcl()
        return reviseWebLink(reviseContent(weather.withProvider(activeProvider)), consentPending)
    }

    private fun StudyWeather.withProvider(provider: String) =
        if (provider.isEmpty() || provider == providerName) this else copy(providerName = provider)

    companion object {
        const val TAG = "StudyGetWeather"
    }
}
