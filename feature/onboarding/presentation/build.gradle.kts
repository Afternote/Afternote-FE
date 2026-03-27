plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.onboarding.presentation"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jetbrains.kotlinx.serialization.json)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
