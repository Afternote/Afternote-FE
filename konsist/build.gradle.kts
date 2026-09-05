plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    // 아래 Java 11 고정 때문에 afternote.jvm.library(17) 를 탈 수 없어 ktlint 만 빠져 있었다 (#1419).
    id("afternote.jvm.lint")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit)

    // konsist 0.17.3 이 kotlin-compiler-embeddable 2.0.21 경유로 coroutines 1.9.0 을 끌어와, 카탈로그
    // 선언(1.11.0)과 어긋난 채 해석된다(#974). 이 모듈은 아키텍처 테스트 전용이라 산출물에 영향이 없지만
    // 감사가 보는 testRuntimeClasspath 를 카탈로그 하나의 의도로 맞춘다.
    constraints {
        testImplementation(libs.coroutines.core) {
            because("카탈로그 선언 버전으로 정렬 — konsist 전이 1.9.0 과 불일치(#974)")
        }
    }
}

tasks.withType<Test>().configureEach {
    // 「해소된 항목은 경고로 알린다」 가 찍는 경고가 CI 로그에 남아야, 목록을 언제 갱신할지 알아챈다.
    testLogging { showStandardStreams = true }

    // 이 모듈의 가드는 타 모듈 소스를 **런타임에** 읽는다(`Konsist.scopeFromProject()`).
    // 그 소스를 입력으로 선언하지 않으면 Gradle 이 볼 때 이 태스크의 입력은 konsist 자신의
    // 소스와 classpath 뿐이라, 가드를 위반한 변경에도 태스크를 안 돌리고 초록을 낸다 (#1657).
    //
    // 두 경로로 새고, 둘 다 실측했다.
    //   - `UP-TO-DATE` — 타 모듈 프로덕션 소스만 바꾸면 스킵된다. 로컬 검증이 여기서 무력해진다.
    //   - `FROM-CACHE` — 산출물을 다 지운 새 체크아웃에서도 build cache 가 결과를 되살린다.
    //     CI 는 `--build-cache` 로 돌고, 워밍(`build-cache-warm.yml`)이 이 태스크를 캐시에 넣는다.
    inputs
        .files(
            rootProject.fileTree(rootProject.projectDir) {
                // konsist 가 실제로 읽는 `/src/` 아래 `.kt`와 production visibility 분석이
                // 프로덕션 consumer 확인에 쓰는 `.gradle.kts`를 모두 입력으로 둔다. Kotlin 파일은
                // 소스셋을 main 으로 좁히지 않는다: ReceiverHomeResource 가드는 screenshotTest 의
                // FQN 참조를 잡고, 다른 가드도 패키지·어노테이션으로만 걸러 소스셋을 가리지 않는다.
                // 좁히면 그만큼 가드가 다시 침묵한다.
                include("**/src/**/*.kt", "**/*.gradle.kts")

                // 정렬 아이콘 가드(AlignIconSharedAssetKonsistTest)는 `.kt` 가 아니라 **드로어블 벡터**를
                // 읽는다. 그 가드가 막으려는 변경이 곧 「모듈에 사본 xml 을 되살리는 것」이라, 이 include
                // 가 없으면 정확히 그 변경에서만 태스크가 `UP-TO-DATE` 로 침묵한다 — 실측으로 확인했다.
                include("**/src/**/res/drawable*/**/*.xml")

                // konsist 는 빌드 산출물과 `.gradle` 을 자기 힘으로 걸러낸다. 여기서도 빼야
                // 생성 소스가 입력에 섞여 매 빌드 입력이 흔들리는 것을 막는다.
                exclude("**/build/**", "**/.gradle/**", "**/.git/**")

                // 저장소 루트 밑에 **다른 브랜치의 체크아웃**이 산다(워크트리). 남의 체크아웃은
                // 이 빌드의 소스가 아니므로 입력이 될 수 없고, 넣으면 걷기 비용이 폭발한다 —
                // 메인 체크아웃 실측으로 걸러낸 `.kt` 1,045개 대 안 걸러낸 87,688개다.
                // konsist 자신은 이 경로를 걸러내지 못하는데, 그건 별개 결함이라 #1657 에 적어 뒀다.
                exclude("**/.claude/**", "**/.codex/**")
            },
        ).withPropertyName("konsistScannedRepositoryInputs")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
