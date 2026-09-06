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
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "afternote-fe"

include(":app")
include(":baselineprofile")

// Core Modules
include(":core:common")
include(":core:data")
include(":core:datastore")
include(":core:domain")
include(":core:model")
include(":core:network")
include(":core:ui")

// Feature Modules
include(":feature:afternote:data")
include(":feature:afternote:domain")
include(":feature:afternote:presentation")

include(":feature:home:presentation")

include(":feature:mindrecord:data")
include(":feature:mindrecord:domain")
include(":feature:mindrecord:presentation")

include(":feature:onboarding:data")
include(":feature:onboarding:presentation")

include(":feature:receiver:data")
include(":feature:receiver:domain")
include(":feature:receiver:presentation")

include(":feature:setting:data")
include(":feature:setting:domain")
include(":feature:setting:presentation")

include(":feature:timeletter:data")
include(":feature:timeletter:domain")
include(":feature:timeletter:presentation")
include(":feature:timeletter:res")

// Architecture test module (Konsist) — 레이어 의존 방향 회귀 가드
include(":konsist")
