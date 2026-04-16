plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"
    resourcePrefix = "mindrecord_"
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(libs.coil.compose)
}
