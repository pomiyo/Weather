package dev.local.weatherstudy.domain.type

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.type.LifeStyleType
 *
 * Observed responsibility: the nine activities the LifeStyle card can show. Note these
 * ids are 0-based and dense — they double as the row order in
 * `LifeStyleSettingsFragment` and as the COL_LIFESTYLE_TYPE primary key.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyLifeStyleType {
    const val RUNNING = 0
    const val CYCLING = 1
    const val GARDENING = 2
    const val STARGAZING = 3
    const val TENNIS = 4
    const val WALKING = 5
    const val GOLF = 6
    const val HIKING = 7
    const val CAMPING = 8

    val ALL = listOf(RUNNING, CYCLING, GARDENING, STARGAZING, TENNIS, WALKING, GOLF, HIKING, CAMPING)
}

/**
 * Corresponds conceptually to `…type.LifeStyleStateType`.
 *
 * Observed responsibility: the suitability band. The original encodes the BAND COUNT
 * in the value — a 3-level activity uses 1..3, a 4-level one uses 10..13, a 5-level
 * one 20..24 — so a renderer can tell how many segments to draw from the value alone.
 */
object StudyLifeStyleStateType {
    const val POOR_OF_3LEVELS = 1
    const val FAIR_OF_3LEVELS = 2
    const val GOOD_OF_3LEVELS = 3

    const val POOR_OF_4LEVELS = 10
    const val FAIR_OF_4LEVELS = 11
    const val GOOD_OF_4LEVELS = 12
    const val GREAT_OF_4LEVELS = 13

    const val LEVEL1_OF_5LEVELS = 20
    const val LEVEL2_OF_5LEVELS = 21
    const val LEVEL3_OF_5LEVELS = 22
    const val LEVEL4_OF_5LEVELS = 23
    const val LEVEL5_OF_5LEVELS = 24

    /** Reconstruction of the band-count decode the original relies on implicitly. */
    fun bandCount(stateType: Int): Int = when (stateType) {
        in POOR_OF_3LEVELS..GOOD_OF_3LEVELS -> 3
        in POOR_OF_4LEVELS..GREAT_OF_4LEVELS -> 4
        in LEVEL1_OF_5LEVELS..LEVEL5_OF_5LEVELS -> 5
        else -> 0
    }
}

/** Corresponds conceptually to `…type.LifeStyleIntervalType`. */
object StudyLifeStyleIntervalType {
    const val HOUR = 0
    const val DAY = 1
}

/** Corresponds conceptually to `…type.LocationsLabelType`. */
object StudyLocationsLabelType {
    const val NONE = 0
    const val HOME = 1
    const val WORK = 2
    const val SCHOOL = 3
    const val CUSTOM = 4
}
