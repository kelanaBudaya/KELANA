import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Jetpack Compose + Material 3. Works on top of an Android application or library module. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(plugin("kotlin-compose"))

            val configureCompose: CommonExtension<*, *, *, *, *, *>.() -> Unit = {
                buildFeatures.compose = true
                // Only modules that get the androidTest dependencies below are instrumented.
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<com.android.build.api.dsl.LibraryExtension> { configureCompose() }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<com.android.build.api.dsl.ApplicationExtension> { configureCompose() }
            }

            dependencies {
                val bom = platform(lib("androidx-compose-bom"))
                add("implementation", bom)
                add("implementation", lib("androidx-compose-ui"))
                add("implementation", lib("androidx-compose-foundation"))
                add("implementation", lib("androidx-compose-material3"))
                add("implementation", lib("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", lib("androidx-compose-ui-tooling"))
                // Compose UI tests (src/androidTest), run on a device or emulator with connectedDebugAndroidTest.
                add("androidTestImplementation", bom)
                add("androidTestImplementation", lib("androidx-compose-ui-test-junit4"))
                add("androidTestImplementation", lib("androidx-test-ext-junit"))
                add("androidTestImplementation", lib("androidx-test-runner"))
                add("debugImplementation", lib("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
