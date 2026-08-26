plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.common"
    resourcePrefix = "core_common_"
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
}
