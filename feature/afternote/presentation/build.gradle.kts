plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.afternote.feature.afternote.presentation"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.feature.afternote.domain)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.android.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.navigation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
}
