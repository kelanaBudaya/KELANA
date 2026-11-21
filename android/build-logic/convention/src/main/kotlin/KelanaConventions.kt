import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal const val KELANA_COMPILE_SDK = 36
internal const val KELANA_MIN_SDK = 26
internal const val KELANA_JDK = 17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.lib(alias: String) = libs.findLibrary(alias).get()

internal fun Project.plugin(alias: String): String = libs.findPlugin(alias).get().get().pluginId

/** `:core:designsystem` becomes `id.kelana.core.designsystem`. */
internal fun Project.kelanaNamespace(): String = "id.kelana" + path.replace(':', '.')

/** Shared Android settings for application and library modules. */
internal fun configureKotlinAndroid(project: Project, extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        // The installed platform is API 36.1 (API 36, minor revision 1).
        compileSdk {
            version = release(KELANA_COMPILE_SDK) {
                minorApiLevel = 1
            }
        }
        defaultConfig.minSdk = KELANA_MIN_SDK
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
