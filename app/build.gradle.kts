plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.local.weatherstudy"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.local.weatherstudy"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.7.3.10-study"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    // presentation
    implementation(project(":study-app"))
    implementation(project(":study-app-common"))
    implementation(project(":study-ui-common"))
    implementation(project(":study-widget"))

    // domain + data
    implementation(project(":study-domain"))
    implementation(project(":study-data"))
    implementation(project(":study-persistence"))
    implementation(project(":study-database"))
    implementation(project(":study-network"))
    implementation(project(":study-backend"))

    // background + gating
    implementation(project(":study-sync"))
    implementation(project(":study-condition"))

    // platform abstraction
    implementation(project(":study-system-service"))
    implementation(project(":study-system-service-android"))
    implementation(project(":study-system-service-samsung"))
    implementation(project(":study-system-location"))

    // peripheral
    implementation(project(":study-interworking"))
    implementation(project(":study-logger"))
    implementation(project(":study-bnr"))
    implementation(project(":study-devopts"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
