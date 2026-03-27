plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.setting.presentation"
}

dependencies {
    implementation(projects.feature.setting.domain)
    implementation(projects.core.common)
    implementation(projects.core.ui)
}