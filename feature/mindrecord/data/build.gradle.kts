plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.mindrecord.data"

    // 매퍼가 해석 실패를 android.util.Log 로 남긴다 — 순수 JUnit 에서 그 호출이 죽지 않게 한다.
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    // 취소를 다시 던지는 runCatchingCancellable — repository 가 CancellationException 을
    // Result.failure 로 삼키지 않게 한다 (#670).
    implementation(projects.core.common)
    implementation(projects.core.network)

    // 취소 전파 회귀 테스트 — runTest 로 Job 취소 시점을 제어한다 (#670).
    testImplementation(libs.coroutines.test)
}
