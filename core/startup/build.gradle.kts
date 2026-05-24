import java.util.Properties

plugins {
    id("afternote.android.library")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.afternote.core.startup"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // 네이티브 앱 키는 이 파일에 문자열로 박지 말 것(저장소 유출 시 도용 위험).
        // 루트 local.properties(gitignore) 또는 CI 환경변수 KAKAO_NATIVE_APP_KEY만 사용.
        val kakaoKey =
            localProperties.getProperty("KAKAO_NATIVE_APP_KEY")
                ?: System.getenv("KAKAO_NATIVE_APP_KEY")
                ?: ""

        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kakao.sdk.auth)
}
