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

    // BiometricCryptoGate 가 BiometricPrompt.CryptoObject 를 공개 시그니처로 돌려주므로
    // 소비 모듈의 컴파일 클래스패스에도 올라가야 한다 — implementation 이면 반환 타입을 못 본다.
    api(libs.androidx.biometric)

    // launchMemorialVideo 의 URL 검증이 android.net.Uri 파싱에 기대므로 JVM 테스트에 Android
    // 구현이 필요하다. 매니페스트는 읽지 않으므로 isIncludeAndroidResources 는 켜지 않는다.
    testImplementation(libs.robolectric)
}
