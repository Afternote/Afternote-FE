plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.afternote.data"

    // 매퍼가 항목을 기각할 때 android.util.Log 로 보고한다 — 그 경로가 JVM 테스트에서도 돌아야 한다.
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.receiver.domain)

    // LeaveMessageBlockContractTest 가 수신 상세 응답 샘플(receiver:data DTO)로 계약을 검증한다.
    testImplementation(projects.feature.receiver.data)
}
