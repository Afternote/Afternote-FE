plugins {
    id("afternote.android.library")
    id("afternote.android.retrofit")
    id("afternote.android.hilt")
}

android {
    namespace = "com.afternote.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig { buildConfigField("String", "BASE_URL", "\"https://afternote.kro.kr/\"") }
}

dependencies {
    implementation(projects.core.domain)
//    순환 의존
//    implementation(projects.core.datastore)
//    implementation(projects.core.data)
}
