plugins {
    id("afternote.android.domain")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.mindrecord.domain"
}

dependencies {
    implementation(libs.coroutines.core)
}
