import java.util.Properties

plugins {
    id("afternote.android.application")
    id("afternote.android.navigation")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.afternote.afternote_fe"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.afternote.afternote_fe"
        versionCode = 1
        versionName = "1.0"

        val kakaoKey =
            localProperties.getProperty("KAKAO_NATIVE_APP_KEY")
                ?: System.getenv("KAKAO_NATIVE_APP_KEY")
                ?: ""

        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kakao.sdk.auth)

    implementation(projects.core.ui)
    implementation(projects.core.di)
    implementation(projects.core.datastore)
    implementation(projects.feature.afternote.presentation)
    implementation(projects.feature.afternote.data)
    implementation(projects.feature.mindrecord.presentation)
    implementation(projects.feature.timeletter.presentation)
    implementation(projects.feature.onboarding.presentation)
}
