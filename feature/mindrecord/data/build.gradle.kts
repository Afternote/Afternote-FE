plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.mindrecord.data"

    // 매퍼가 해석 실패를 android.util.Log 로 남긴다 — 순수 JUnit 에서 그 호출이 죽지 않게 한다.
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.network)
}
