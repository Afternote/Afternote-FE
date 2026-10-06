plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.setting.data"
}

dependencies {
    implementation(projects.feature.setting.domain)
    // 탈퇴 실패 진단(ErrorReporter). core:domain·model·network 는 data 규약이 이미 붙인다.
    implementation(projects.core.common)

    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.coroutines.test)
}
