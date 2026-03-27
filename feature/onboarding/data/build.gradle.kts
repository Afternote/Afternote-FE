plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.onboarding.data"
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(projects.core.network)
}
