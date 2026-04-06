import java.util.Properties

plugins {
    id("afternote.android.application")
    id("afternote.android.navigation")
}

val localProperties =
    Properties().apply {
        load(rootProject.file("local.properties").inputStream())
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

        buildConfigField(
            "String",
            "KAKAO_NATIVE_APP_KEY",
            "\"${localProperties.getProperty("KAKAO_NATIVE_APP_KEY")}\"",
        )
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
    implementation(projects.feature.afternote.presentation)
    implementation(projects.feature.afternote.data)
    implementation(projects.feature.mindrecord.presentation)
    implementation(projects.feature.timeletter.presentation)
    implementation(projects.feature.onboarding.presentation)
}
