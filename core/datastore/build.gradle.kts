plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.datastore"
}
dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(projects.core.common)
    implementation(libs.jetbrains.kotlinx.serialization.json)
}
