import java.util.Properties

plugins {
    id("kelana.android.library")
    id("kelana.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

// Base URL is a placeholder. Resolution order: local.properties, Gradle property, gradle.properties default.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val apiBaseUrl: String = localProperties.getProperty("KELANA_API_BASE_URL")
    ?: providers.gradleProperty("KELANA_API_BASE_URL").orNull
    ?: "https://api.kelana.invalid/"

android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
}

dependencies {
    api(libs.retrofit)
    api(libs.okhttp)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    api(libs.kotlinx.serialization.json)
}
