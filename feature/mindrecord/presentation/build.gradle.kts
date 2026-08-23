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

    // 매퍼가 android.util.Log 를 타 JVM 단위 테스트로는 돌지 않는다 (#751).
    testImplementation(libs.robolectric)
}
