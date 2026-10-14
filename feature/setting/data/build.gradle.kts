plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.setting.data"
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(projects.feature.setting.domain)
    // 탈퇴 실패 진단(ErrorReporter). core:domain·model·network 는 data 규약이 이미 붙인다.
    implementation(projects.core.common)

    testFixturesImplementation(projects.feature.setting.domain)
    testFixturesImplementation(libs.hilt.android.testing)
    add("kspTestFixtures", libs.hilt.compiler)

    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.coroutines.test)
}
