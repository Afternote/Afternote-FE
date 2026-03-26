plugins {
    id("afternote.android.feature")
}

android {
    namespace = "com.afternote.feature.onboarding.presentation"
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(libs.kakao.sdk.user)
}
