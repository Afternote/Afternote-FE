plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.domain"
}
dependencies {
    implementation(projects.core.model)
}
