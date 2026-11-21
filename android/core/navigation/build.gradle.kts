plugins {
    id("kelana.android.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.androidx.navigation.compose)
    implementation(project(":core:model"))
}
