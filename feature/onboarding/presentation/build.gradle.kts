import java.util.Properties

plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.afternote.feature.onboarding.presentation"

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Google Cloud Console에서 발급받은 Web Client ID. 저장소에 키를 박지 말 것.
        // 루트 local.properties(gitignore) 또는 CI 환경변수 GOOGLE_WEB_CLIENT_ID만 사용.
        val googleWebClientId =
            localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
                ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
                ?: ""
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

    // Compose Preview Screenshot Testing (#319) — feature 모듈 단위 적용 1차
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
