plugins {
    id("kelana.android.library")
    id("kelana.android.compose")
    id("kelana.android.hilt")
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.browser)
    implementation(libs.maplibre.android)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.serialization.json)
}
