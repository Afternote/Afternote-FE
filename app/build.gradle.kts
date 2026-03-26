import java.util.Properties

plugins {
    id("afternote.android.application")
}

val localProperties =
    Properties().also { props ->
        val file = rootProject.file("local.properties")
        if (file.exists()) props.load(file.inputStream())
    }

android {
    namespace = "com.afternote.afternote_fe"

    defaultConfig {
        applicationId = "com.afternote.afternote_fe"
        versionCode = 1
        versionName = "1.0"

        val kakaoAppKey = localProperties.getProperty("KAKAO_APP_KEY", "")
        if (kakaoAppKey.isBlank()) {
            logger.warn("⚠️  KAKAO_APP_KEY is not set in local.properties. Kakao login will not work at runtime.")
        }
        buildConfigField("String", "KAKAO_APP_KEY", "\"$kakaoAppKey\"")
        manifestPlaceholders["KAKAO_APP_KEY"] = kakaoAppKey
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.datastore)
    implementation(projects.feature.onboarding.presentation)
    implementation(projects.feature.onboarding.data)
    implementation(libs.kakao.sdk.user)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
