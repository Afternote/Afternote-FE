plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.coroutines.core)
    testFixturesImplementation(projects.core.model)
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.coroutines.test)
}
