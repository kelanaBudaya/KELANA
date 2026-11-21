plugins {
    id("kelana.android.application")
    id("kelana.android.compose")
    id("kelana.android.hilt")
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
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
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
}
