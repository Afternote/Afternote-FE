import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    id("afternote.android.application")
    id("afternote.android.navigation")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.app.distribution)
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

        // 카카오 OAuth redirect(`kakao{KEY}://oauth`) 핸들러 등록용.
        // SDK 런타임 초기화 키는 `core:startup`의 BuildConfig.KAKAO_NATIVE_APP_KEY 사용.
        val kakaoKey =
            localProperties.getProperty("KAKAO_NATIVE_APP_KEY")
                ?: System.getenv("KAKAO_NATIVE_APP_KEY")
                ?: ""
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoKey
    }

    signingConfigs {
        create("release") {
            val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            firebaseAppDistribution {
                groups = "afternote"
                releaseNotes = "Release build for internal distribution"
            }
        }
    }
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.core.splashscreen)

    // 카카오 OAuth redirect Activity(`com.kakao.sdk.auth.AuthCodeHandlerActivity`)를
    // app 매니페스트에서 직접 참조하므로 컴파일 classpath에 노출 필요.
    implementation(libs.kakao.sdk.auth)

    // Core
    implementation(projects.core.network)
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

    // Feature — domain (홈 요약 UseCase가 마인드레코드 도메인 Repository에, 수신자 홈이 애프터노트 도메인 Repository에 직접 의존)
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.feature.afternote.domain)
    implementation(projects.feature.receiver.domain)

    // Feature — data (Hilt @Module / 바인딩이 루트 그래프에 포함되도록 app이 classpath에 둔다)
    implementation(projects.feature.afternote.data)
    implementation(projects.feature.mindrecord.data)
    implementation(projects.feature.timeletter.data)
    implementation(projects.feature.onboarding.data)
}
