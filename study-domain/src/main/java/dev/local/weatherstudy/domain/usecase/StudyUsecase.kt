package dev.local.weatherstudy.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.BaseUsecase
 * com.samsung.android.weather.domain.usecase.Usecase
 * com.samsung.android.weather.domain.usecase.SingleUsecase
 * com.samsung.android.weather.domain.usecase.PureUsecase
 * com.samsung.android.weather.domain.usecase.ActionUsecase
 * com.samsung.android.weather.domain.usecase.UsecaseK / SingleUsecaseK
 *
 * Observed responsibility: the original has **153 use-case classes** in this package
 * alone, and they all conform to one of six single-method shapes. Two axes:
 *
 * - **arity** — takes an argument ([StudyUsecase], [StudyActionUsecase]) or not
 *   ([StudySingleUsecase], [StudyPureUsecase])
 * - **suspend vs Flow** — the `…K` variants return a `Flow` and are NOT suspending,
 *   which is how the refresh pipeline composes long-running work
 *
 * `invoke` is an operator in all of them, so a use case is called like a function —
 * `getWeather(key)`. That is why the ViewModels read as a list of verbs.
 *
 * Note a class may implement SEVERAL of these at once: the original's `GetWeather`
 * is both `Usecase<Weather, String>` (one by key) and `SingleUsecase<List<Weather>>`
 * (all of them). Kotlin allows that because the signatures differ.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyBaseUsecase

/** `R invoke(A)`, suspending. Corresponds to `Usecase<R, A>`. */
interface StudyUsecase<out R, in A> : StudyBaseUsecase {
    suspend operator fun invoke(arg: A): R
}

/** `R invoke()`, suspending. Corresponds to `SingleUsecase<R>`. */
interface StudySingleUsecase<out R> : StudyBaseUsecase {
    suspend operator fun invoke(): R
}

/** `Unit invoke()`, suspending. Corresponds to `PureUsecase`. */
interface StudyPureUsecase : StudyBaseUsecase {
    suspend operator fun invoke()
}

/** `Unit invoke(A)`, suspending. Corresponds to `ActionUsecase<A>`. */
interface StudyActionUsecase<in A> : StudyBaseUsecase {
    suspend operator fun invoke(arg: A)
}

/** `Flow<R> invoke(A)`, cold. Corresponds to `UsecaseK<Flow, A>`. */
interface StudyUsecaseK<out R, in A> : StudyBaseUsecase {
    operator fun invoke(arg: A): Flow<R>
}

/** `Flow<R> invoke()`, cold. Corresponds to `SingleUsecaseK<R>`. */
interface StudySingleUsecaseK<out R> : StudyBaseUsecase {
    operator fun invoke(): Flow<R>
}
