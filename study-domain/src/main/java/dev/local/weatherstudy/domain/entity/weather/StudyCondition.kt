package dev.local.weatherstudy.domain.entity.weather

import com.squareup.moshi.JsonClass

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.weather.Condition
 *
 * Observed responsibilities:
 * - the measurement payload shared by current / hourly / daily observations
 * - holds THREE condition codes: the provider's own `externalCode`, Samsung's
 *   normalised `internalCode`, and an `expansionCode` for finer variants.
 *   `iconNum` is derived from these by the `AssignIconNum` use case.
 * - carries the life indices for this slot as a nested list
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@JsonClass(generateAdapter = true)
data class StudyCondition(
    val internalCode: Int = INVALID_CODE,
    val externalCode: String = "",
    val expansionCode: String = "",
    val iconNum: Int = INVALID_CODE,
    val weatherText: String = "",
    val narrative: String = "",
    val temperature: Double = INVALID_TEMPERATURE,
    val feelsLikeTemp: Double = INVALID_TEMPERATURE,
    val maxTemp: Double = INVALID_TEMPERATURE,
    val minTemp: Double = INVALID_TEMPERATURE,
    val yesterdayMaxTemp: Double = INVALID_TEMPERATURE,
    val yesterdayMinTemp: Double = INVALID_TEMPERATURE,
    val indexList: List<StudyIndex> = emptyList(),
) {
    companion object {
        const val INVALID_CODE = -1
        const val INVALID_TEMPERATURE = -999.0
    }
}

/** Reconstruction of `ConditionKt`. */
fun StudyCondition.indexOf(type: Int): StudyIndex? = indexList.firstOrNull { it.type == type }

fun StudyCondition.hasValidTemperature(): Boolean =
    temperature != StudyCondition.INVALID_TEMPERATURE
