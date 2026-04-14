plugins {
    id("afternote.android.domain")
}

android {
    namespace = "com.afternote.feature.afternote.domain"
}
dependencies {
    implementation(projects.core.domain)
    implementation(libs.coroutines.core)
}
