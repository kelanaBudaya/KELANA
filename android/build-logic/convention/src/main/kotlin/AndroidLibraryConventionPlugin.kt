import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(plugin("android-library"))
            pluginManager.apply(plugin("kotlin-android"))

            extensions.configure<LibraryExtension> {
                namespace = kelanaNamespace()
                configureKotlinAndroid(target, this)
                testOptions.unitTests.isReturnDefaultValues = true
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(KELANA_JDK)
            }
            dependencies {
                add("testImplementation", lib("junit"))
            }
        }
    }
}
