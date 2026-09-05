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
    // 수신자 홈이 3 피처를 모으는 집계 대시보드라 생긴 의존 (#1462).
    // domain 은 아래 방향이라 순환이 없다. receiver.presentation 은 실패 계측
    // (ReceiverFailureStage·recordReceiverFailure) 하나 때문인데, 그 계측은 수신자
    // 진입 인프라의 일부라 feature:receiver 에 남는 편이 맞다 — mindrecord.presentation
    // 을 이미 같은 이유로 의존하고 있다.
    implementation(projects.feature.receiver.domain)
    implementation(projects.feature.receiver.presentation)
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.timeletter.domain)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // HomeTabViewModel 경합 테스트 — 가상 시간으로 viewModelScope 요청 순서를 제어한다.
    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(testFixtures(projects.feature.mindrecord.domain))
    // 수신자 홈 테스트가 쓰던 fixture·스캐너를 함께 옮긴다 (#1462).
    testImplementation(testFixtures(projects.feature.receiver.domain))
    testImplementation(testFixtures(projects.feature.timeletter.domain))
    testImplementation(testFixtures(projects.core.ui))

    // 자리표시자가 화면에 도달하는지는 실제로 그려 봐야 확인된다 (#562).
    // 로딩 중 배지가 «미완료» 로 확정되지 않는지는 실제로 그려 봐야 확인된다 (#698).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
