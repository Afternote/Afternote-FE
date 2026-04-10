plugins {
    id("afternote.android.library")
}

android {
    namespace = "com.afternote.core.startup"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.work.runtime.ktx)
}
