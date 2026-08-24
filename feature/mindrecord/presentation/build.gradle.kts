plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"
    resourcePrefix = "mindrecord_"

    // Robolectric 이 컴파일된 리소스로 화면을 띄운다 (#729).
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(libs.coil.compose)
    implementation(libs.compose.rich.editor)

    testImplementation(libs.coroutines.test)

    // 컴파일된 리소스로 문구를 검증한다 — aapt2 의 앞뒤 공백 제거는 소스 XML 만 봐서는 잡히지 않는다 (#732).
    // 열린 주차 메뉴의 스크롤·최하단 선택도 JVM 에서 그대로 재현한다 (#729).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
