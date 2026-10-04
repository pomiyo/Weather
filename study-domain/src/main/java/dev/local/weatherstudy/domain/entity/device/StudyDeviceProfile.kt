package dev.local.weatherstudy.domain.entity.device

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.device.DeviceType
 * com.samsung.android.weather.domain.entity.device.DeviceProfile
 * com.samsung.android.weather.domain.entity.device.DeviceMonitor
 *
 * Observed responsibility: the form factor the UI adapts to. This is not cosmetic in
 * the original — `GetColumnSize` returns 1 or 2 from it, which flips
 * `StaggeredGridLayoutManager.LayoutParams.isFullSpan` on every detail card.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
enum class StudyDeviceType {
    PHONE,
    TABLET,
    FOLD_MAIN,
    FOLD_COVER,
    FLIP_MAIN,
    FLIP_COVER,
    DESKTOP,
    UNKNOWN,
}

/** Corresponds conceptually to `…entity.device.DeviceMonitor`. */
interface StudyDeviceMonitor {
    fun isTablet(): Boolean
    fun isFoldable(): Boolean
    fun isFolded(): Boolean
    fun isDesktopMode(): Boolean
    fun isKidsMode(): Boolean
    fun isRetailMode(): Boolean
}

/** Corresponds conceptually to `…entity.device.DeviceProfile`. */
interface StudyDeviceProfile : StudyDeviceMonitor {
    fun getDeviceType(): StudyDeviceType
}

/**
 * Corresponds conceptually to `…entity.oneui.OneUiProfile`.
 *
 * Observed responsibility: in the original this interface EXTENDS `WeatherPolicy`,
 * so the One UI version is one of the inputs that decides which features are on.
 * Reconstructed with the same inheritance for exactly that reason.
 */
interface StudyOneUiProfile : dev.local.weatherstudy.domain.policy.StudyWeatherPolicy {
    fun getOneUiVersion(): Int
}
