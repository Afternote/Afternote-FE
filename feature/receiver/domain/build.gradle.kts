plugins {
    id("afternote.android.domain")
}

android {
    namespace = "com.afternote.feature.receiver.domain"
}
dependencies {
    implementation(projects.core.domain)
    // 수신 모델·계약이 애프터노트 도메인 타입(AfternoteType·LeaveMessageBlock)을 참조한다.
    implementation(projects.feature.afternote.domain)
    implementation(libs.coroutines.core)
    implementation(libs.androidx.paging.common)
}
