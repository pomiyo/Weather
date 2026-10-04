package dev.local.weatherstudy.domain.type

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.type.IndexType
 *
 * Observed responsibilities:
 * - the single registry of index ids. Every provider's index converter maps its own
 *   codes onto these, and every detail card / inner view holder / notation use case
 *   switches on them. Keeping one registry is why the five regional backends can
 *   feed one UI.
 *
 * Values are the original's, recovered verbatim from the decompiled constants —
 * they are wire-compatible ids, not an opaque implementation detail: the Room
 * COL_LIFE_INDEX_TYPE column and the exported content providers both expose them.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyIndexType {
    const val NONE = -1

    const val PRECIPITATION_PROBABILITY = 0
    const val UV = 1
    const val POLLEN = 10
    const val SUNRISE = 13
    const val SUNSET = 14
    const val PM10 = 16
    const val PM2_5 = 17
    const val WIND = 18
    const val DRESS = 19
    const val SPORTS = 20
    const val CAR_WASH = 21
    const val MAKEUP = 22
    const val COMFORT = 23
    const val VISIBILITY = 24
    const val AQI = 26
    const val HUMIDITY = 27
    const val TRAFFIC = 30
    const val JOGGING = 42
    const val GOLF = 44
    const val PRECIPITATION_PROBABILITY_NIGHT = 46
    const val PRECIPITATION_AMOUNT = 47
    const val PRECIPITATION_AMOUNT_NIGHT = 48
    const val MOONRISE = 55
    const val MOONSET = 56
    const val MOON_PHASE = 57
    const val PRESSURE = 58
    const val DEW_POINT = 59
    const val CIVIL_DAWN = 60
    const val CIVIL_DUSK = 61
    const val NAUTICAL_DAWN = 62
    const val NAUTICAL_DUSK = 63
    const val ASTRONOMICAL_DAWN = 64
    const val ASTRONOMICAL_DUSK = 65

    /**
     * The seven types the Index detail card renders with a dedicated inner view holder.
     * Order here is the default order `GetIndexGraphViewEntity` lays out.
     */
    val INDEX_CARD_TYPES = listOf(UV, HUMIDITY, PRESSURE, WIND, VISIBILITY, DEW_POINT, PRECIPITATION_AMOUNT)

    /** The types the Bottom Index card renders. */
    val BOTTOM_INDEX_CARD_TYPES = listOf(SUNRISE, SUNSET, MOONRISE, MOONSET, MOON_PHASE)

    /** The air-quality types the AirIndex card renders. */
    val AIR_INDEX_TYPES = listOf(AQI, PM10, PM2_5, POLLEN)
}

/**
 * Corresponds conceptually to `com.samsung.android.weather.domain.type.IndexCategory`.
 *
 * Observed responsibility: a BITMASK, not an enum — one index can belong to several
 * surfaces at once, which is how `GetIndexGraphViewEntity` filters the same index list
 * for the detail card, the life-index card and the widget.
 */
object StudyIndexCategory {
    const val ALL = 0
    const val LIFEINDEX = 1
    const val DETAIL = 2
    const val AIR = 4
    const val WIDGET = 8
}

/** Corresponds conceptually to `…type.IndexLevel` — severity bands, per measurement family. */
object StudyIndexLevel {
    const val NONE = 0

    // severity
    const val EXTREME = 1
    const val SEVERE = 2
    const val MODERATE = 3
    const val MINOR = 4
    const val UNKNOWN = 5

    /** `IndexLevel.Air` — the AQI bands the AirQualityBar paints. */
    object Air {
        const val GOOD = 131
        const val NORMAL = 132
        const val UNHEALTHY_FOR_SENSITIVE = 133
        const val UNHEALTHY = 134
        const val VERY_UNHEALTHY = 135
        const val HAZARDOUS = 136
    }

    /** `IndexLevel.Pollen` */
    object Pollen {
        const val NONE = 0
        const val LITTLE = 191
        const val MUCH = 192
        const val VERY_MUCH = 193
    }

    /** `IndexLevel.LifeIndex` — the six-band "very bad … very good" scale. */
    object LifeIndex {
        const val VERY_BAD = 121
        const val BAD = 122
        const val NOT_GOOD = 123
        const val NORMAL = 124
        const val GOOD = 125
        const val VERY_GOOD = 126
    }

    /** `IndexLevel.MoonPhase` — the eight phases `DetailMoonPhaseView` draws. */
    object MoonPhase {
        const val NEW_MOON = 1
        const val WAXING_CRESCENT = 2
        const val FIRST_QUARTER = 3
        const val WAXING_GIBBOUS = 4
        const val FULL_MOON = 5
        const val WANING_GIBBOUS = 6
        const val LAST_QUARTER = 7
        const val WANING_CRESCENT = 8
    }

    /** `IndexLevel.Pressure` — the tendency arrow `PressureGraph` shows. */
    object Pressure {
        const val FALLING = 141
        const val STEADY = 142
        const val RISING = 143
    }

    /** `IndexLevel.AqiScale` — which national AQI scale is in force, chosen by region. */
    object AqiScale {
        const val HJ633_2012 = 1
        const val EPA = 2
        const val NAQI = 3
        const val ATMO = 4
        const val DAQI = 5
        const val UBA = 6
        const val IMECA = 7
        const val CAQI = 8
    }

    /** `IndexLevel.Uv` */
    object Uv {
        const val VERY_LOW = 111
        const val LOW = 112
        const val NORMAL = 113
        const val HIGH = 114
        const val VERY_HIGH = 115
        const val EXTREME = 116
    }

    /** `IndexLevel.WindDirection` — the compass labels `WindGraph` renders. */
    object WindDirection {
        const val NO_WIND = "NO"
        const val WHIRL_WIND = "WHIRL"
        val COMPASS = listOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        )
    }
}
