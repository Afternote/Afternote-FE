plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.afternote.presentation"
}

dependencies {
    implementation(projects.feature.afternote.domain)
    implementation(projects.core.common)
    implementation(projects.core.ui)
}