plugins {
    id("afternote.jvm.domain")
    id("afternote.kover")
}

dependencies {
    implementation(projects.core.domain)
    // android.library 규약이 전이로 넘겨 주던 코루틴을 JVM 전환 후 직접 선언한다.
    implementation(libs.coroutines.core)

    testImplementation(testFixtures(projects.core.domain))
}
