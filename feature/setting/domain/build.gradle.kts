plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("afternote.kover")
    // 아래 Java 11 고정 때문에 afternote.jvm.library(17) 를 탈 수 없어 ktlint 만 빠져 있었다 (#1419).
    id("afternote.jvm.lint")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    // Java 11 고정 때문에 공통 JVM 규약을 쓰지 못하는 기존 예외다. strict 전환 전까지 동일한
    // explicit API warning inventory 를 유지한다.
    explicitApiWarning()
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
