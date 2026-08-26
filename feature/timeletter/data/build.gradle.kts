plugins {
    id("afternote.android.data")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.timeletter.data"
}

dependencies {
    implementation(projects.feature.timeletter.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)
}
