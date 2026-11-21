import java.util.Properties

plugins {
    id("kelana.android.library")
    id("kelana.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

// Optional local config. Missing values default to empty and the app shows "not configured" states.
// Keys (local.properties or -P): supabase.url, supabase.anonKey, google.webClientId, google.cloudProjectNumber
// (Play Integrity), integrity.devToken (debug only, true = send "dev-token" for a server with KELANA_ENV=dev). Never commit them.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun configValue(key: String): String =
    (localProperties.getProperty(key) ?: providers.gradleProperty(key).orNull ?: "")
        .trim()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${configValue("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${configValue("supabase.anonKey")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${configValue("google.webClientId")}\"")
        buildConfigField("String", "GOOGLE_CLOUD_PROJECT_NUMBER", "\"${configValue("google.cloudProjectNumber")}\"")
        buildConfigField("boolean", "INTEGRITY_DEV_TOKEN", configValue("integrity.devToken").toBoolean().toString())
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.androidx.work.runtime)
    implementation(libs.play.integrity)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
