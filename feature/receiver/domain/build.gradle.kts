plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    implementation(projects.core.domain)
    // 수신 모델·계약이 애프터노트 도메인 타입(AfternoteType·LeaveMessageBlock)을 참조한다.
    implementation(projects.feature.afternote.domain)
    implementation(libs.coroutines.core)
    implementation(libs.androidx.paging.common)

    testFixturesImplementation(libs.coroutines.core)
    testFixturesImplementation(libs.androidx.paging.common)
}
