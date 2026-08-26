plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.afternote.presentation"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric 이 이 모듈의 *병합된* 매니페스트를 읽게 한다. 끄면 패키지가
            // `org.robolectric.default` 로 떨어져 FileProvider authority 가 매니페스트와 어긋난다 (#369).
            isIncludeAndroidResources = true
        }
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

    // 렌더는 같아도 클릭 전달·접근성 semantics 가 달라지는 컨트롤 회귀를 실제 Compose 트리로 검사한다 (#1168).
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Compose Preview Screenshot Testing (#330) — 1hyok 영역 마무리 묶음
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
