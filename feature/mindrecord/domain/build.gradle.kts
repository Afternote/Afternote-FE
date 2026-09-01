plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    implementation(libs.coroutines.core)

    testImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.coroutines.core)
}
