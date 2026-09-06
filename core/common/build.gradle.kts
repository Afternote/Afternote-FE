plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.common"
    resourcePrefix = "core_common_"
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)

    // launchMemorialVideo 의 URL 검증이 android.net.Uri 파싱에 기대므로 JVM 테스트에 Android
    // 구현이 필요하다. 매니페스트는 읽지 않으므로 isIncludeAndroidResources 는 켜지 않는다.
    testImplementation(libs.robolectric)
}
