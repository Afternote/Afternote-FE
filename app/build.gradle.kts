import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    id("afternote.android.application")
    id("afternote.android.navigation")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.app.distribution)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.compose.screenshot)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// 카카오 OAuth redirect(`kakao{KEY}://oauth`) 핸들러 등록용.
// SDK 런타임 초기화 키는 `core:startup`의 BuildConfig.KAKAO_NATIVE_APP_KEY 사용.
val kakaoKey = socialLoginKey("KAKAO_NATIVE_APP_KEY")

android {
    namespace = "com.afternote.afternote_fe"

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.afternote.afternote_fe"
        versionCode = 1
        versionName = "1.0"

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
        // 팀 공유 debug keystore 로 서명해 카카오 키 해시를 머신 간 통일.
        //
        // opt-in 이다. AGP 는 debug keystore 를 머신마다 자동 생성하는 것이 기본이고
        // (https://developer.android.com/studio/publish/app-signing), 그 모델을 없애지 않는다 —
        // `DEBUG_*` 를 적지 않은 머신은 아래 when 의 첫 갈래로 빠져 `~/.android/debug.keystore`
        // 로 서명된다. 빌드·로그인 동작에 차이는 없고, 카카오 콘솔에 본인 해시를 등록하면 된다
        // (카카오는 원래 "모든 개발자의 디버그 키 해시" 등록을 요구:
        //  https://developers.kakao.com/docs/latest/ko/android/getting-started).
        // 공유 keystore 는 그 등록 건수를 1개로 줄이려는 선택지이지 강제가 아니다.
        // CI 도 프로퍼티가 없으므로 같은 경로로 폴백한다.
        //
        // 트레이드오프: 이 keystore 의 해시가 카카오·구글 콘솔에 등록되므로, 파일이 유출되면
        // 제3자가 같은 키 해시로 우리 앱 행세를 하며 소셜 로그인을 호출할 수 있다. 영향은
        // debug 한정이다 — release 는 별도 keystore(`RELEASE_*`) 이고, debug 인증서로 서명한
        // 앱은 스토어 배포가 불가능하다("insecure by design", 위 Android 문서). 실수 커밋은
        // 루트 `.gitignore` 의 `*.jks`·`*.keystore` 가 막지만, 전달 경로는 개인 채널로 유지한다.
        // 이 리스크를 받아들이기 싫으면 위 opt-out(프로퍼티 미기재)을 쓰면 된다.
        //
        // 네 키는 하나의 단위로 다룬다. `DEBUG_STORE_FILE` 만 보고 진입하면 storeFile 은 공유
        // keystore 로 바뀌는데 자격 값은 기본 keystore 의 `android` 가 아니라 null 로 덮여,
        // 폴백도 아니고 원인도 안 보이는 서명 실패가 난다. 부분 기재·경로 오류는 서명 단계까지
        // 가지 않고 configuration 단계에서 누락 항목을 알리며 끊는다.
        getByName("debug") {
            val debugSigningKeys =
                listOf(
                    "DEBUG_STORE_FILE",
                    "DEBUG_STORE_PASSWORD",
                    "DEBUG_KEY_ALIAS",
                    "DEBUG_KEY_PASSWORD",
                )
            val provided =
                debugSigningKeys
                    .mapNotNull { key ->
                        localProperties.getProperty(key)?.takeIf { it.isNotBlank() }?.let { key to it }
                    }.toMap()

            when (provided.size) {
                0 -> {
                    // 전부 미기재 — AGP 기본 debug keystore 로 서명(공유 keystore 미수령 머신·CI).
                }

                debugSigningKeys.size -> {
                    val sharedKeystore = file(provided.getValue("DEBUG_STORE_FILE"))
                    if (!sharedKeystore.isFile) {
                        throw GradleException(
                            """
                            |DEBUG_STORE_FILE 이 가리키는 공유 debug keystore 를 찾을 수 없습니다: $sharedKeystore
                            |파일을 해당 경로에 두거나, DEBUG_* 네 키를 모두 지워 기본 debug keystore 로 되돌리세요.
                            |설정 방법은 README '공유 debug keystore' 섹션 참고.
                            """.trimMargin(),
                        )
                    }
                    storeFile = sharedKeystore
                    storePassword = provided.getValue("DEBUG_STORE_PASSWORD")
                    keyAlias = provided.getValue("DEBUG_KEY_ALIAS")
                    keyPassword = provided.getValue("DEBUG_KEY_PASSWORD")
                }

                else -> {
                    throw GradleException(
                        """
                        |공유 debug keystore 설정이 불완전합니다.
                        |루트 local.properties 누락 항목: ${(debugSigningKeys - provided.keys).joinToString()}
                        |네 키는 하나의 단위입니다 — 모두 채우거나 모두 지우세요(모두 지우면 기본 debug keystore 로 서명).
                        |설정 방법은 README '공유 debug keystore' 섹션 참고.
                        """.trimMargin(),
                    )
                }
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
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.core.splashscreen)

    // 카카오 OAuth redirect Activity(`com.kakao.sdk.auth.AuthCodeHandlerActivity`)를
    // app 매니페스트에서 직접 참조하므로 컴파일 classpath에 노출 필요.
    implementation(libs.kakao.sdk.auth)

    // Firebase — Crashlytics 는 크래시 자동 수집이라 초기화 코드가 필요 없다.
    // 버전은 BoM 이 관리하므로 개별 좌표에는 버전을 적지 않는다.
    // Analytics 는 넣지 않는다. breadcrumb 이 그것에 의존하지만, 얻는 값보다 자동 수집되는
    // 사용자 데이터 범위가 커서 제외했다 — 실패 지점은 `<흐름>_stage` 키로 직접 남긴다.
    // 그래서 로그에 "Could not register handler for breadcrumbs events" 가 뜨는 건 정상이다.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    // Core
    implementation(projects.core.common)
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

    // Compose Preview Screenshot Testing (#330) — 1hyok 영역 (홈) 적용
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
