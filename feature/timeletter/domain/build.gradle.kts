plugins {
    id("afternote.android.domain")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.timeletter.domain"
}

dependencies {
    implementation(projects.core.domain)
    testImplementation(testFixtures(projects.core.domain))
}
