import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: Android library + Compose + Hilt, depending on :core:* only.
 * Features never depend on each other; the dependency rule is enforced in the root build.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("kelana.android.library")
            pluginManager.apply("kelana.android.compose")
            pluginManager.apply("kelana.android.hilt")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:navigation"))
                add("implementation", lib("androidx-navigation-compose"))
                add("implementation", lib("androidx-hilt-navigation-compose"))
                add("implementation", lib("androidx-lifecycle-viewmodel-compose"))
            }
        }
    }
}
