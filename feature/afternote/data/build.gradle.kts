plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.afternote.data"
}

dependencies {
    implementation(projects.feature.afternote.domain)
}
