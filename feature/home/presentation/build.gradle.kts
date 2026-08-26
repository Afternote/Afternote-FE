plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true

    namespace = "com.afternote.feature.home.presentation"
    resourcePrefix = "home_"
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
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(testFixtures(projects.feature.mindrecord.domain))

    // 로딩 중 배지가 «미완료» 로 확정되지 않는지는 실제로 그려 봐야 확인된다 (#698).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
