plugins {
    id("kelana.android.library")
    id("kelana.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
