plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.timeletter.data"

    // VoiceRecorderRepositoryImpl 은 프로덕션에서 이 모듈 안에서만 쓰여 internal 로 좁혔다.
    // app androidTest 가 실제 MediaRecorder 경계를 검증하려면 internal 을 넓히는 대신
    // 같은 모듈의 testFixtures 에서 공개 계약(VoiceRecorderRepository)만 돌려주는 팩토리를
    // 노출한다 (docs/convention/production-visibility.md, #440 리뷰).
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(projects.feature.timeletter.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)

    testFixturesImplementation(projects.feature.timeletter.domain)
    testFixturesImplementation(libs.coroutines.core)
}
