plugins {
    id("afternote.android.datastore")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.datastore"
}
dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
