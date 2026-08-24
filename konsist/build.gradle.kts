plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
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
