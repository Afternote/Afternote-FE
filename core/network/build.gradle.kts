plugins {
    id("afternote.android.library")
    id("afternote.android.retrofit")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "BASE_URL",
            "\"${project.findProperty("BASE_URL") ?: "https://api.afternote.com/"}\"",
        )
    }
}

dependencies {
    implementation(projects.core.datastore)
}
