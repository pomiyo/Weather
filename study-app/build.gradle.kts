plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

// The original declares the Maps credential as a LITERAL android:value in its manifest
// meta-data. The reconstruction takes it from local.properties instead, so no credential is
// ever committed. See reports/maps-credential-analysis.md.
val studyMapsApiKey: String = rootProject.file("local.properties").let { f ->
    if (!f.exists()) "" else f.readLines()
        .firstOrNull { it.trimStart().startsWith("STUDY_MAPS_API_KEY=") }
        ?.substringAfter('=')?.trim().orEmpty()
}

android {
    namespace = "dev.local.weatherstudy.app"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        // An empty value is a valid configuration: the build succeeds and the map surfaces stay hidden.
        manifestPlaceholders["studyMapsApiKey"] = studyMapsApiKey
        buildConfigField("String", "STUDY_MAPS_API_KEY", "\"$studyMapsApiKey\"")
        buildConfigField("boolean", "STUDY_MAPS_AVAILABLE", studyMapsApiKey.isNotEmpty().toString())
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    api(project(":study-app-common"))
    api(project(":study-ui-common"))
    api(project(":study-domain"))
    implementation(project(":study-condition"))
    implementation(project(":study-interworking"))
    implementation(project(":study-logger"))
    implementation(project(":study-devopts"))
    implementation(project(":study-system-service"))
    implementation(project(":study-system-location"))
    implementation(project(":study-sync"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.coil)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
