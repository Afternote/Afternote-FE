plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    implementation(libs.coroutines.core)

    testFixturesImplementation(libs.coroutines.core)
}
