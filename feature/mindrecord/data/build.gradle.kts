plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.mindrecord.data"
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.network)
}
