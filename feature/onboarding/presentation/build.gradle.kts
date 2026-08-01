plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
}

// Google Cloud Console에서 발급받은 Web Client ID.
val googleWebClientId = socialLoginKey("GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "com.afternote.feature.onboarding.presentation"

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kakao.sdk.auth)
    implementation(libs.kakao.sdk.user)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // ViewModel 코루틴 테스트 — runTest 로 viewModelScope 를 제어한다.
    testImplementation(libs.coroutines.test)

    // Compose Preview Screenshot Testing (#330)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // LoginViewModel 계약 단위 테스트 — Main 디스패처 치환용 (PR #647 리뷰 반영)
    testImplementation(libs.coroutines.test)
}
