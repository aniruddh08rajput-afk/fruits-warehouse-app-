plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
id("org.jetbrains.kotlin.plugin.compose")
}

android {
namespace = "com.example.fruitswarehouse"
compileSdk = 35

defaultConfig {
    applicationId = "com.example.fruitswarehouse"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
}

buildFeatures {
    compose = true
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}

}

dependencies {
implementation("androidx.core:core-ktx:1.15.0")
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.compose.ui:ui:1.7.6")
implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
implementation("androidx.compose.material3:material3:1.3.1")
implementation("androidx.compose.material:material-icons-extended:1.7.6")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
