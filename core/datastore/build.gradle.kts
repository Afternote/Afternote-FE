plugins {
    id("afternote.android.datastore")
}

android {
    namespace = "com.afternote.core.datastore"
}
dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.jetbrains.kotlinx.serialization.json)
}
