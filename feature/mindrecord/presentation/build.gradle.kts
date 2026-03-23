plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.common)
    implementation(projects.core.ui)
}
