plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.afternote.data"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.receiver.domain)
}
