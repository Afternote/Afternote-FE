plugins {
    id("afternote.android.library")
}

val kakaoKey = socialLoginKey("KAKAO_NATIVE_APP_KEY")

android {
    namespace = "com.afternote.core.startup"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kakao.sdk.auth)
}
