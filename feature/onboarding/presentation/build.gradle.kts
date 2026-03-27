plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.onboarding.presentation"
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
}
