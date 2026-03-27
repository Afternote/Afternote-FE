plugins {
    id("afternote.android.library")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jetbrains.kotlinx.serialization.json)
    implementation(libs.kakao.sdk.auth)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.datastore)
}
