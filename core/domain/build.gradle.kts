plugins {
    id("afternote.android.domain")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.domain"
}
dependencies {
    implementation(projects.core.model)

    // core 계약 정본 fake 가 공개 API 모델과 StateFlow 기반 메모리 상태를 함께 노출한다 (#1041).
    testFixturesImplementation(projects.core.model)
    testFixturesImplementation(libs.coroutines.core)
}
