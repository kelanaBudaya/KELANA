plugins {
    id("kelana.android.feature")
}

dependencies {
    // Unit tests use the fake repositories (feature -> :core:* is allowed).
    testImplementation(project(":core:data"))
}
