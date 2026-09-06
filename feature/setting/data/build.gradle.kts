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
    implementation(projects.core.network)
    testFixturesImplementation(projects.feature.setting.domain)
    testFixturesImplementation(libs.hilt.android.testing)
    add("kspTestFixtures", libs.hilt.compiler)
}
