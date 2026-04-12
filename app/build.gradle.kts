plugins {
    id("afternote.android.application")
    id("afternote.android.navigation")
}

android {
    namespace = "com.afternote.afternote_fe"

    defaultConfig {
        applicationId = "com.afternote.afternote_fe"
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.androidx.hilt.navigation.compose)

    // Core
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(projects.core.startup)
    implementation(projects.core.di)
    implementation(projects.core.domain)

    // Feature — presentation
    implementation(projects.feature.afternote.presentation)
    implementation(projects.feature.mindrecord.presentation)
    implementation(projects.feature.timeletter.presentation)
    implementation(projects.feature.onboarding.presentation)
    implementation(projects.feature.setting.presentation)

    // Feature — data (Hilt @Module / 바인딩이 루트 그래프에 포함되도록 app이 classpath에 둔다)
    implementation(projects.feature.afternote.data)
    implementation(projects.feature.mindrecord.data)
    implementation(projects.feature.timeletter.data)
    implementation(projects.feature.onboarding.data)
}
