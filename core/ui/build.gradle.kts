plugins {
    id("afternote.android.library.compose")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.ui"
    resourcePrefix = "core_ui_"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testFixtures.enable = true
    // robolectric Compose UI 테스트가 stringResource 를 읽으려면 JVM 테스트에 리소스가 실려야 한다 (#516)
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(libs.androidx.fragment.ktx)
    // MviViewModel 이 androidx.lifecycle.ViewModel 을 상속해 노출하므로 feature 컴파일
    // 클래스패스에도 올라가야 한다 — implementation 이면 상속체가 supertype 을 못 본다.
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(projects.core.common)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Compose Preview Screenshot Testing (#241)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // robolectric Compose UI 동작 테스트 — BOM 은 convention 이 test 구성엔 안 걸어 줘서 직접 (#516)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // feature Compose 테스트가 같은 48dp 판정·진단 형식을 공유한다 (#1167).
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.ui.test.junit4)
}
