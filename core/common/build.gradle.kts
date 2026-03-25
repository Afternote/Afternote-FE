plugins {
    id("afternote.android.library")
}

android {
    namespace = "com.afternote.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jetbrains.kotlinx.serialization.json)
}
