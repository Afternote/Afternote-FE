plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.afternote.feature.onboarding.presentation"
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kakao.sdk.auth)
    implementation(libs.kakao.sdk.user)
}
