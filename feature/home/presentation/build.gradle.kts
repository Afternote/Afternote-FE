plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    alias(libs.plugins.compose.screenshot)
}

android {
    namespace = "com.afternote.feature.home.presentation"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.feature.mindrecord.presentation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // HomeTabViewModel 경합 테스트 — 가상 시간으로 viewModelScope 요청 순서를 제어한다.
    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.feature.mindrecord.domain))

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
