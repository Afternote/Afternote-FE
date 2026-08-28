plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}
dependencies {
    implementation(projects.core.domain)
    implementation(libs.coroutines.core)
    implementation(libs.androidx.paging.common)

    testFixturesImplementation(libs.coroutines.core)
    testFixturesImplementation(libs.androidx.paging.common)
}
