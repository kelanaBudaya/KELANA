import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(plugin("android-application"))
            pluginManager.apply(plugin("kotlin-android"))

            extensions.configure<ApplicationExtension> {
                namespace = "id.kelana.app"
                configureKotlinAndroid(target, this)
                defaultConfig {
                    applicationId = "id.kelana.app"
                    targetSdk = KELANA_COMPILE_SDK
                }
                testOptions.unitTests.isReturnDefaultValues = true
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(KELANA_JDK)
            }
        }
    }
}
