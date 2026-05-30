plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
}

android {
    namespace = "com.afternote.feature.afternote.presentation"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.receiver.domain)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.android.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.navigation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)

    // Compose Preview Screenshot Testing (#330) — 1hyok 영역 마무리 묶음
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
