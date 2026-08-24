plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"

    testOptions.unitTests.isIncludeAndroidResources = true
    resourcePrefix = "mindrecord_"
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
    testImplementation(libs.robolectric)
}
