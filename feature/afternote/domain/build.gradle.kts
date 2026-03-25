plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.feature.afternote.domain"
}
dependencies {
    implementation(projects.core.domain)
}
