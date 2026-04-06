plugins {
    id("afternote.android.library")
}

android {
    namespace = "com.afternote.core.common"
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
}
