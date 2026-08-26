plugins {
    id("afternote.android.domain")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.domain"
}
dependencies {
    implementation(projects.core.model)
}
