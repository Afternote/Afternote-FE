pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "afternote-fe"
include(":app")
include(":core:network")
include(":core:ui")
include(":feature:timeletter:presentation")
include(":feature:timeletter:domain")
include(":feature:timeletter:data")
include(":feature:mindrecord:data")
include(":feature:mindrecord:domain")
include(":feature:mindrecord:presentation")
include(":feature:setting:data")
include(":feature:setting:presentation")
include(":feature:setting:domain")
include(":feature:afternote:data")
include(":feature:afternote:domain")
include(":feature:afternote:presentation")
include(":feature:onboarding:data")
include(":feature:onboarding:presentation")
include(":feature:onboarding:domain")
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:datastore")
