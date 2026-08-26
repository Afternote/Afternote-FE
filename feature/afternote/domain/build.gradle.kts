plugins {
    id("afternote.android.domain")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.afternote.domain"
}
dependencies {
    implementation(projects.core.domain)
    implementation(libs.coroutines.core)
    implementation(libs.androidx.paging.common)
}
