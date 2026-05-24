plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("afternote.layer.rules")
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
