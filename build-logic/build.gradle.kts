plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ktlint.gradle)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "afternote.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "afternote.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "afternote.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRetrofit") {
            id = "afternote.android.retrofit"
            implementationClass = "AndroidRetrofitConventionPlugin"
        }
        register("androidNavigation") {
            id = "afternote.android.navigation"
            implementationClass = "AndroidNavigationConventionPlugin"
        }
        register("androidFeature") {
            id = "afternote.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "afternote.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("androidDomain") {
            id = "afternote.android.domain"
            implementationClass = "AndroidDomainConventionPlugin"
        }
        register("androidApplication") {
            id = "afternote.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLint") {
            id = "afternote.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("androidDatastore") {
            id = "afternote.android.datastore"
            implementationClass = "AndroidDatastoreConventionPlugin"
        }
    }
}