pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kelana"

include(":app")

include(":core:designsystem")
include(":core:ui")
include(":core:model")
include(":core:navigation")
include(":core:data")
include(":core:network")
include(":core:database")

include(":feature:onboarding")
include(":feature:akun")
include(":feature:jelajah")
include(":feature:tempat")
include(":feature:cerita")
include(":feature:agenda")
include(":feature:rencana")
include(":feature:peta")
include(":feature:paspor")
include(":feature:postcard")
include(":feature:notifikasi")
include(":feature:tiket")
