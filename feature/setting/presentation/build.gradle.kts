plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

val googleWebClientId = socialLoginKey("GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "com.afternote.feature.setting.presentation"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions.unitTests.isIncludeAndroidResources = true

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }
}

dependencies {
    implementation(projects.feature.setting.domain)
    implementation(projects.core.datastore)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.coil.compose)
    implementation(libs.kakao.sdk.auth)
    implementation(libs.kakao.sdk.user)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(projects.core.network)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
