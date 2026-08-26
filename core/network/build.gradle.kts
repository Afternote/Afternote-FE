plugins {
    id("afternote.android.library")
    id("afternote.android.retrofit")
    id("afternote.android.hilt")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig { buildConfigField("String", "BASE_URL", "\"https://afternote.kro.kr/api/v1/\"") }
    // JVM 유닛 테스트에서 Android API 스텁은 기본적으로 "not mocked" RuntimeException 을 던진다.
    // TokenAuthenticatorTest 의 실패 경로들이 Log.e 를 지나가므로, 예외 대신 기본값(0 등)을
    // 반환시켜 Log 를 no-op 으로 만든다. 공식 가이드(local-tests)는 거짓 통과 위험 때문에
    // "Only use it as a last resort" 라고 경고 — 이 모듈은 걸리는 API 가 검증 대상 아닌 Log 뿐이라
    // 채택. Android API 의 실제 반환값에 의존하는 테스트는 이 모듈에 두지 말 것.
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
}
