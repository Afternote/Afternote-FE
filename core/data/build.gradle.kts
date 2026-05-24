plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.core.data"
}

dependencies {
    implementation(projects.core.common)
}
