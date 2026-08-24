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

    // 캘린더 날짜 셀 상호작용을 JVM 에서 실제로 눌러 확인한다 (#724).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
