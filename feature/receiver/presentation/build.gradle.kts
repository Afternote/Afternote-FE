plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true

    namespace = "com.afternote.feature.receiver.presentation"
    resourcePrefix = "receiver_"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(projects.feature.receiver.domain)
    // 수신자 홈이 각 기능의 수신 Repository·모델을 조합한다. afternote 의존(도메인 모델·서비스명
    // 아이콘 매핑)은 수신자 흐름 잔여분이 afternote 에서 이 모듈로 합류(#615)하면 재검토.
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.afternote.presentation)
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.feature.timeletter.domain)
    implementation(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.navigation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.feature.mindrecord.domain))
    testImplementation(testFixtures(projects.feature.receiver.domain))
    testImplementation(testFixtures(projects.feature.timeletter.domain))
    testImplementation(libs.robolectric)

    // 실패 표기가 «레이아웃을 유지하는지» 는 실제로 그려 봐야 확인된다 (#952).
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(projects.core.ui))
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
