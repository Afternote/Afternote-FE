plugins {
    id("afternote.android.library.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.afternote.core.ui"
    resourcePrefix = "core_ui_"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
