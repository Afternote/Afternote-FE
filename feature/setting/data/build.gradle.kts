plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.setting.data"
}

dependencies {
    implementation(projects.feature.setting.domain)
    implementation(projects.core.network)
}
