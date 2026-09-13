plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.setting.data"
}

dependencies {
    implementation(projects.feature.setting.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.network)
    testImplementation(libs.junit)
    testImplementation(testFixtures(projects.core.domain))
}
