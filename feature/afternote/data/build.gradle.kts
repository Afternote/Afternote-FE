plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.afternote.data"
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
