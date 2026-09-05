plugins {
    // AGP는 루트의 com.android.application apply-false로 이미 classpath에 있으므로 버전 없는 ID를 쓴다.
    id("com.android.test")
    alias(libs.plugins.androidx.baselineprofile)
    // 이 모듈은 컨벤션(afternote.android.library) 대신 com.android.test 를 직접 쓰느라 ktlint 만
    // 빠져 있었다. resolve-pr-impact.mjs 가 «.kt·.kts 가 바뀐 모듈» 마다 `<모듈>:ktlintCheck` 를
    // 고르므로(#1279 이후), 태스크가 없으면 이 모듈을 건드리는 PR 의 Ktlint job 이 태스크 선택
    // 단계에서 죽는다 (#1419). 카탈로그 버전·리포터 설정을 공유하도록 컨벤션으로 붙인다.
    id("afternote.android.lint")
}

android {
    namespace = "com.afternote.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    testOptions.managedDevices.localDevices {
        create("pixel2Api34") {
            device = "Pixel 2"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    managedDevices.clear()
    managedDevices += "pixel2Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.test.runner)
}
