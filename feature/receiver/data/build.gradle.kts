plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.receiver.data"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.feature.receiver.domain)
}
