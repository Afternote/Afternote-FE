plugins {
    id("afternote.android.application")
    id("afternote.android.navigation")
}

android {
    namespace = "com.afternote.afternote_fe"

    defaultConfig {
        applicationId = "com.afternote.afternote_fe"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(projects.core.ui)

    implementation(projects.feature.afternote.presentation)
    implementation(projects.feature.mindrecord.presentation)
    implementation(projects.feature.timeletter.presentation)
    implementation(projects.feature.onboarding.presentation)
}
