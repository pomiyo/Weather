pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "WeatherStudy"

// Module graph mirrors the Gradle modules recovered from META-INF/*.kotlin_module
// and the per-module R.java namespaces in the APK. See reports/reconstruction-inventory.md §1.
include(":app")                              // samsung_weather_tos_sep14_oneui5.1_phone

// presentation
include(":study-app")                        // weather-app
include(":study-app-common")                 // weather-app-common
include(":study-ui-common")                  // weather-ui-common
include(":study-widget")                     // weather-widget

// domain
include(":study-domain")                     // weather-domain

// data
include(":study-data")                       // weather-data
include(":study-database")                   // weather-database  (versioned 1629 separately)
include(":study-persistence")                // com.samsung.android.weather.persistence
include(":study-network")                    // weather-network
include(":study-backend")                    // com.samsung.android.weather.backend

// background + startup gating
include(":study-sync")                       // weather-sync
include(":study-condition")                  // weather-condition

// platform abstraction — the Samsung seam
include(":study-system-service")             // …system.service            (interfaces)
include(":study-system-service-android")     // …system.service.android    (AOSP impls)
include(":study-system-service-samsung")     // …system.service.sep        (Samsung impls -> stubs)
include(":study-system-location")            // …system.location

// peripheral subsystems
include(":study-interworking")               // weather-interworking
include(":study-logger")                     // weather-logger
include(":study-bnr")                        // weather-bnr
include(":study-devopts")                    // com.samsung.android.weather.devopts
