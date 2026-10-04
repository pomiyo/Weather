package dev.local.weatherstudy.domain.entity.state

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.state.State
 *
 * Observed responsibility: the three-way result wrapper the domain returns instead of
 * throwing. It is what lets a refresh failure reach the detail screen as a renderable
 * state rather than an exception — `DetailRefreshResultState` is built from it.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
sealed class StudyState<out T> {
    data object Loading : StudyState<Nothing>()
    data class Success<T>(val value: T) : StudyState<T>()
    data class Error<T>(val throwable: Throwable, val value: T? = null) : StudyState<T>()

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.value
}

inline fun <T, R> StudyState<T>.map(transform: (T) -> R): StudyState<R> = when (this) {
    is StudyState.Loading -> StudyState.Loading
    is StudyState.Success -> StudyState.Success(transform(value))
    is StudyState.Error -> StudyState.Error(throwable, value?.let(transform))
}
