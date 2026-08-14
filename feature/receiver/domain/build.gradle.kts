plugins {
    id("afternote.android.domain")
}

android {
    namespace = "com.afternote.feature.receiver.domain"
}
dependencies {
    implementation(projects.core.domain)
    implementation(libs.coroutines.core)
}
