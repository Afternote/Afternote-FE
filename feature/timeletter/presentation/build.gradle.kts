plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.timeletter.presentation"
}

dependencies {
    implementation(projects.feature.timeletter.domain)
    implementation(projects.feature.timeletter.res)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.android.navigation.compose)
}
