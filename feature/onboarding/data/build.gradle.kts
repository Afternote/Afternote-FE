plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.onboarding.data"
}

dependencies {
    implementation(projects.core.network)
}
