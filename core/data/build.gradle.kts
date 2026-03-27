plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.core.data"
}

dependencies {
    implementation(libs.kakao.sdk.auth)
}
