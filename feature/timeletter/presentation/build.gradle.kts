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
    implementation(libs.androidx.material.icons.extended)
}
