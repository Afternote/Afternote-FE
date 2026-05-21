import java.util.Properties

plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.afternote.feature.setting.presentation"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val googleWebClientId =
            localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
                ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
                ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }
}

dependencies {
    implementation(projects.feature.setting.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.kakao.sdk.auth)
    implementation(libs.kakao.sdk.user)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
