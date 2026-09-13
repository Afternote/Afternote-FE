plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

// Google Cloud Console에서 발급받은 Web Client ID.
val googleWebClientId = socialLoginKey("GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "com.afternote.feature.onboarding.presentation"
    resourcePrefix = "onboarding_"

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    // Robolectric 이 실제 문자열·테마를 읽어야 레이아웃이 시안대로 렌더된다.
    testOptions.unitTests.isIncludeAndroidResources = true

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kakao.sdk.auth)
    implementation(libs.kakao.sdk.user)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // ViewModel 코루틴 테스트 — runTest 와 Main 디스패처 치환으로 viewModelScope 를 제어한다.
    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.domain))

    // 레이아웃 폭 배분은 픽셀이 아니라 노드 bounds 라, 스크린샷 대신 Compose 로 직접 잰다.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(projects.core.ui))
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Compose Preview Screenshot Testing (#330)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
