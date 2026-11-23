plugins {
    id("kelana.android.feature")
}

dependencies {
    implementation(libs.androidx.activity.compose) // Photo Picker (PickVisualMedia)

    testImplementation(project(":core:data"))
}
