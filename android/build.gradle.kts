import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Module dependency rules (see AGENTS.md):
//  - :feature:* may depend on :core:* only (never on another feature)
//  - :core:* never depends on a feature or on :app
//  - only :app depends on features
subprojects {
    afterEvaluate {
        val owner = path
        configurations.forEach { configuration ->
            configuration.dependencies.withType(ProjectDependency::class.java).forEach { dep ->
                val target = dep.path
                val violation = when {
                    owner.startsWith(":feature:") && !target.startsWith(":core:") ->
                        "feature modules may depend on :core:* only"
                    owner.startsWith(":core:") && target.startsWith(":feature:") ->
                        "core modules must never depend on a feature"
                    owner.startsWith(":core:") && target == ":app" ->
                        "core modules must never depend on :app"
                    else -> null
                }
                if (violation != null) {
                    throw GradleException("Illegal module dependency $owner -> $target: $violation")
                }
            }
        }
    }
}
