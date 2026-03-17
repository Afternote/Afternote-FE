plugins {
    id("afternote.android.application")
}

android {
    namespace = "com.afternote.afternote_fe"

    defaultConfig {
        applicationId = "com.afternote.afternote_fe"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(projects.feature.afternote.data) // 예시
    // implementation(projects.feature.login)
}
