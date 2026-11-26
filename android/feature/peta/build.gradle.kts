plugins {
    id("kelana.android.feature")
}

dependencies {
    implementation(libs.maplibre.android)
    implementation(libs.okhttp)
    testImplementation(project(":core:data"))
}
