plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    // 계정·알림 계약이 노출하는 도메인 모델(UserConnectedAccount·UserPushSetting·UserMarketingConsent).
    implementation(projects.core.model)

    testFixturesImplementation(projects.core.model)
}
