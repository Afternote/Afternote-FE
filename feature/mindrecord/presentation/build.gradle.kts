plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"
    resourcePrefix = "mindrecord_"

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

    // 부분 성공에서 어떤 문구가 나가는지는 컴포지션을 태워야 확인된다 (#725).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
