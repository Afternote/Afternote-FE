plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.data"
    // AccessTokenExpiryTracker 실물이 참조하는 SystemClock 등 android.* 정적 호출을
    // JVM 단위 테스트에서 기본값(0)으로 처리 — core:network 와 같은 설정.
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(projects.core.common)
    testImplementation(testFixtures(projects.core.domain))
    // TokenDataSource 실물에 주입할 in-memory DataStore<Preferences> 구성용 (단위 테스트 전용)
    testImplementation(libs.androidx.datastore.preferences)
}
