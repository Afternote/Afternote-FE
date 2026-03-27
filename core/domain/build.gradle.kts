plugins {
    id("afternote.android.domain")
}

android {
    namespace = "com.afternote.core.domain"
}
dependencies {
    implementation(projects.core.model)
}
