plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    // 계정·알림 계약이 노출하는 도메인 모델(UserConnectedAccount·UserPushSetting·UserMarketingConsent).
    implementation(projects.core.model)
    // 수신자 저장 UseCase 가 core 계약 UserReceiverRepository 를 쓴다 (#1691).
    implementation(projects.core.domain)
    implementation(libs.coroutines.core)

    testFixturesImplementation(projects.core.model)

    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.domain))
}
