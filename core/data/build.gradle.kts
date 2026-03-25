plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.data"
}
dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
}
