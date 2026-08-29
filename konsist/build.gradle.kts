plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    // build-logic 이 이미 클래스패스에 올려 둔 플러그인이라 버전을 다시 선언하면 충돌한다.
    id("org.jlleitschuh.gradle.ktlint")
}

// 이 모듈은 convention plugin(afternote.jvm.library) 대신 java-library 를 직접 쓰느라 ktlint 만
// 빠져 있었다. resolve-pr-impact.mjs 는 «.kt 가 바뀐 모듈» 마다 `<모듈>:ktlintCheck` 를 고르므로
// (PR #1279 이후), 태스크가 없으면 konsist 소스를 건드리는 PR 의 Ktlint job 이 태스크 선택 단계에서
// 죽는다. build-logic 과 같은 방식으로 플러그인만 직접 적용한다.
ktlint {
    version.set(libs.versions.ktlint)
    filter {
        exclude { it.file.path.contains("build/") }
    }
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
}
