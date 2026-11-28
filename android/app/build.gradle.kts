import java.util.Properties

plugins {
    id("kelana.android.application")
    id("kelana.android.compose")
    id("kelana.android.hilt")
}

// Release signing: android/keystore.properties (gitignored) with storeFile, storePassword, keyAlias, keyPassword.
// Without it, release builds are unsigned and cannot be uploaded to Play.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    defaultConfig {
        versionCode = 2
        versionName = "0.1.0"
    }
    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:akun"))
    implementation(project(":feature:jelajah"))
    implementation(project(":feature:tempat"))
    implementation(project(":feature:cerita"))
    implementation(project(":feature:agenda"))
    implementation(project(":feature:rencana"))
    implementation(project(":feature:peta"))
    implementation(project(":feature:paspor"))
    implementation(project(":feature:postcard"))
    implementation(project(":feature:notifikasi"))
    implementation(project(":feature:tiket"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(project(":core:model"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
