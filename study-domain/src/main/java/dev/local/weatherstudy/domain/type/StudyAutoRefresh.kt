package dev.local.weatherstudy.domain.type

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.type.AutoRefresh
 *
 * Observed responsibilities:
 * - [Flag] is a bitmask naming WHAT to refresh — the refresh pipeline is not
 *   all-or-nothing; a widget tick may refresh only the observation, while opening
 *   the detail screen refreshes forecast + content + insight.
 * - [From] names WHO asked, and is carried all the way into analytics and into
 *   `UpdateRefreshTimeWhenFailed` so a failed widget refresh does not block a
 *   later user-initiated one.
 * - [Interval] holds the two minimum refresh gaps.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyAutoRefresh {

    const val ACTION_AUTO_REFRESH = "dev.local.weatherstudy.action.AUTO_REFRESH"

    /** `AutoRefresh.Flag` — bitmask of what a refresh pass should fetch. */
    object Flag {
        const val OBSERVATION = 1 shl 0
        const val FORECAST = 1 shl 1
        const val CONTENT = 1 shl 2
        const val CURRENT_POSITION = 1 shl 3
        const val FORECAST_CHANGE = 1 shl 4
        const val NOTIFICATION = 1 shl 5

        const val ALL = OBSERVATION or FORECAST or CONTENT or CURRENT_POSITION or
            FORECAST_CHANGE or NOTIFICATION
    }

    /** `AutoRefresh.From` — the surface that requested the refresh. */
    object From {
        const val UNKNOWN = 0
        const val SYSTEM = 1
        const val DETAIL = 2
        const val LOCATION = 3
        const val SETTING = 4
        const val APP_WIDGET = 5
        const val COVER_WIDGET = 6
        const val FACE_WIDGET = 7
        const val EDGE = 8
        const val SMART_PAGE = 9
        const val TILE = 10
        const val WEAR = 11
        const val WALLPAPER = 12
        const val DEEP_LINK = 13
        const val RESTORE = 14
        const val CORP = 15
        const val CUSTOMIZATION = 16
        const val ACTIVITY_TRANSITION = 17
    }

    /** `AutoRefresh.Interval` — the floor on how often each family may be refetched. */
    object Interval {
        const val FORECAST = 3 * 60 * 60 * 1000L
        const val CONTENT = 6 * 60 * 60 * 1000L
    }
}

/**
 * Corresponds conceptually to `…type.SettingValue`.
 *
 * Observed responsibility: the persisted settings vocabulary. [AutoRefreshInterval]
 * is the user's choice in Settings; `NONE` means manual-only, which is why
 * `SyncAutoRefresh` cancels the periodic work rather than scheduling a long interval.
 */
object StudySettingValue {
    const val OFF = 0
    const val ON = 1
    const val AUTO_REFRESH_UNKNOWN = -1

    object AutoRefreshInterval {
        const val NONE = 0
        const val EVERY_HOUR = 1
        const val EVERY_3HOUR = 3
        const val EVERY_6HOUR = 6
        const val EVERY_12HOUR = 12
        const val EVERY_24HOUR = 24

        fun toMillis(value: Int): Long = value.toLong() * 60L * 60L * 1000L
    }

    object PpAgreement {
        const val NONE = 0
        const val CHANGE_NOTIFICATION = 1
        const val AWAY_PP_AGREEMENT = 2
    }

    object RefreshStatus {
        const val DONE = 0
        const val RUNNING = 1
        const val FAILED = 2
        const val NEED_REFRESH = 3
    }
}

/**
 * Corresponds conceptually to `…type.Keys`.
 *
 * Observed responsibility: the string keys the status table and the WorkManager
 * unique-work names share. They are in the domain layer because both `:study-sync`
 * and `:study-data` need the same name to agree on "is a refresh already running".
 */
object StudyKeys {
    const val CURRENT = "current"
    const val REFRESH = "refresh"
    const val AUTO_REFRESH = "auto_refresh"
    const val ADD_REPRESENT_LOCATION = "add_represent_location"
    const val TO_AWAY_MODE = "to_away_mode"
    const val PLUG_IN_BOOT_SYNC = "plug_in_boot_sync"
    const val SAMSUNG_NEWS = "samsung_news"
    const val RUBIN_INIT = "rubin_init"
}

/** Corresponds conceptually to `…type.NotificationType`. */
object StudyNotificationType {
    const val NORMAL = "normal"
    const val PANEL = "panel"
    const val REFRESH = "refresh"
    const val AUTO_REFRESH = "auto_refresh"
    const val FORECAST_CHANGE = "forecast_change"
    const val APP_UPDATE = "app_update"
    const val DEX = "dex"

    object Setting {
        const val NONE = 0
        const val ONGOING_FORECAST = 1
        const val TODAY_FORECAST = 2
        const val TOMORROW_FORECAST = 3
        const val WEATHER_FORECAST = 4
        const val SEVERE_WEATHER_ALERTS = 5
        const val SHORT_TERM_PRECIPITATION = 6
    }
}

/** Corresponds conceptually to `…type.BadgeValue`. */
object StudyBadgeValue {
    const val NONE = 0
    const val SHOW = 1
    const val SHOW_WITH_POPUP = 2
    const val MARKET_UPDATE = 1
    const val MARKET_UPDATE_FORCED = 4
}

/** Corresponds conceptually to `…type.AppUpdateResult`. */
object StudyAppUpdateResult {
    const val NOT_SUPPORT = 0
    const val UPDATE_UNAVAILABLE = 1
    const val UPDATE_AVAILABLE = 2
    const val FORCED_UPDATE = 3
    const val SERVER_ERROR = 4
    const val NETWORK_ERROR = 5
}
