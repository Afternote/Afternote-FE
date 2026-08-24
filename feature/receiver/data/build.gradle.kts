plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.receiver.data"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.feature.receiver.domain)
    // 수신 상세가 애프터노트 도메인 모델·공유 매퍼(MappingUtils 등)·author 서비스(내려받기)를
    // 참조한다. 공유 지점의 receiver 몫 분리는 후속 판단.
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.afternote.data)
}
