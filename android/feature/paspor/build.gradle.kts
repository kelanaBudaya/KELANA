plugins {
    id("kelana.android.feature")
}

dependencies {
    // Single location fix (getCurrentLocation) in the "Cap sekarang" handler only.
    implementation(libs.play.services.location)
    implementation(libs.androidx.activity.compose)

    // Unit tests use the fake repositories (feature -> :core:* is allowed).
    testImplementation(project(":core:data"))
}
