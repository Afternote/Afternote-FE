plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.presentation"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jetbrains.kotlinx.serialization.json)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
