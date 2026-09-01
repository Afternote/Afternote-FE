plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.timeletter.presentation"
    testOptions.unitTests.isIncludeAndroidResources = true
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(projects.feature.timeletter.domain)
    implementation(projects.feature.timeletter.res)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.compose.wheel.picker)
    implementation(libs.coil.compose)
    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(testFixtures(projects.feature.timeletter.domain))
    testImplementation(libs.robolectric)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
