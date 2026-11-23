plugins {
    id("kelana.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Local reminders ("waktunya berangkat", pentas) are scheduled with WorkManager. No FCM yet.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose) // POST_NOTIFICATIONS permission request

    // The notification center and its settings are kept in DataStore (JSON), so they survive a restart.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:data"))
}
