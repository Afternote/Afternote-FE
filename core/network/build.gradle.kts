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
    defaultConfig { buildConfigField("String", "BASE_URL", "\"https://afternote.kro.kr/api/v1/\"") }
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(projects.core.domain)
    implementation(projects.core.model)
}
