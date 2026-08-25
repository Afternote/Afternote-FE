plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.core.common"
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
}
